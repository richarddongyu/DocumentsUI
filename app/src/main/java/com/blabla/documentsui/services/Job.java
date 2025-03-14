/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blabla.documentsui.services;

import static android.content.ContentResolver.wrap;
import static com.blabla.documentsui.DocumentsApplication.acquireUnstableProviderOrThrow;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.FileUtils;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;

import com.blabla.documentsui.Metrics;
import com.blabla.documentsui.OperationDialogFragment;
import com.blabla.documentsui.R;
import com.blabla.documentsui.base.DocumentInfo;
import com.blabla.documentsui.base.DocumentStack;
import com.blabla.documentsui.base.Features;
import com.blabla.documentsui.base.Shared;
import com.blabla.documentsui.clipping.UrisSupplier;
import com.blabla.documentsui.files.FilesActivity;

import java.io.FileNotFoundException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A mashup of work item and ui progress update factory. Used by {@link FileOperationService}
 * to do work and show progress relating to this work.
 */
abstract public class Job implements Runnable {
    private static final String TAG = "Job";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_CREATED, STATE_STARTED, STATE_SET_UP, STATE_COMPLETED, STATE_CANCELED})
    @interface State {}
    static final int STATE_CREATED = 0;
    static final int STATE_STARTED = 1;
    static final int STATE_SET_UP = 2;
    static final int STATE_COMPLETED = 3;
    /**
     * A job is in canceled state as long as {@link #cancel()} is called on it, even after it is
     * completed.
     */
    static final int STATE_CANCELED = 4;

    static final String INTENT_TAG_WARNING = "warning";
    static final String INTENT_TAG_FAILURE = "failure";
    static final String INTENT_TAG_PROGRESS = "progress";
    static final String INTENT_TAG_CANCEL = "cancel";

    final Context service;
    final Context appContext;
    final Listener listener;

    final @FileOperationService.OpType int operationType;
    final String id;
    final DocumentStack stack;

    final UrisSupplier mResourceUris;

    int failureCount = 0;
    final ArrayList<DocumentInfo> failedDocs = new ArrayList<>();
    final ArrayList<Uri> failedUris = new ArrayList<>();

    final Builder mProgressBuilder;

    final CancellationSignal mSignal = new CancellationSignal();

    private final Map<String, ContentProviderClient> mClients = new HashMap<>();
    private final Features mFeatures;

    private volatile @State int mState = STATE_CREATED;

    /**
     * A simple progressable job, much like an AsyncTask, but with support
     * for providing various related notification, progress and navigation information.
     * @param service The service context in which this job is running.
     * @param listener
     * @param id Arbitrary string ID
     * @param stack The documents stack context relating to this request. This is the
     *     destination in the Files app where the user will be take when the
     *     navigation intent is invoked (presumably from notification).
     * @param srcs the list of docs to operate on
     */
    Job(Context service, Listener listener, String id,
        @FileOperationService.OpType int opType, DocumentStack stack, UrisSupplier srcs, Features features) {

        assert(opType != FileOperationService.OPERATION_UNKNOWN);

        this.service = service;
        this.appContext = service.getApplicationContext();
        this.listener = listener;
        this.operationType = opType;

        this.id = id;
        this.stack = stack;
        this.mResourceUris = srcs;

        mFeatures = features;

        mProgressBuilder = createProgressBuilder();
    }

    @Override
    public final void run() {
        if (isCanceled()) {
            // Canceled before running
            return;
        }

        mState = STATE_STARTED;
        listener.onStart(this);

        try {
            boolean result = setUp();
            if (result && !isCanceled()) {
                mState = STATE_SET_UP;
                start();
            }
        } catch (RuntimeException e) {
            // No exceptions should be thrown here, as all calls to the provider must be
            // handled within Job implementations. However, just in case catch them here.
            Log.e(TAG, "Operation failed due to an unhandled runtime exception.", e);
            Metrics.logFileOperationErrors(operationType, failedDocs, failedUris);
        } finally {
            mState = (mState == STATE_STARTED || mState == STATE_SET_UP) ? STATE_COMPLETED : mState;
            finish();
            listener.onFinished(this);

            // NOTE: If this details is a JumboClipDetails, and it's still referred in primary clip
            // at this point, user won't be able to paste it to anywhere else because the underlying
            mResourceUris.dispose();
        }
    }

    boolean setUp() {
        return true;
    }

    abstract void finish();

    abstract void start();
    abstract Notification getSetupNotification();
    abstract Notification getProgressNotification();
    abstract Notification getFailureNotification();

    abstract Notification getWarningNotification();

    Uri getDataUriForIntent(String tag) {
        return Uri.parse(String.format("data,%s-%s", tag, id));
    }

    ContentProviderClient getClient(Uri uri) throws RemoteException {
        ContentProviderClient client = mClients.get(uri.getAuthority());
        if (client == null) {
            // Acquire content providers.
            client = acquireUnstableProviderOrThrow(
                    getContentResolver(),
                    uri.getAuthority());

            mClients.put(uri.getAuthority(), client);
        }

        assert(client != null);
        return client;
    }

    ContentProviderClient getClient(DocumentInfo doc) throws RemoteException {
        return getClient(doc.derivedUri);
    }

    void releaseClient(Uri uri) {
        ContentProviderClient client = mClients.get(uri.getAuthority());
        if (client != null) {
            client.close();
            mClients.remove(uri.getAuthority());
        }
    }

    void releaseClient(DocumentInfo doc) {
        releaseClient(doc.derivedUri);
    }

    final void cleanup() {
        for (ContentProviderClient client : mClients.values()) {
            FileUtils.closeQuietly(client);
        }
    }

    final @State int getState() {
        return mState;
    }

    final void cancel() {
        mState = STATE_CANCELED;
        mSignal.cancel();
        Metrics.logFileOperationCancelled(operationType);
    }

    final boolean isCanceled() {
        return mState == STATE_CANCELED;
    }

    final boolean isFinished() {
        return mState == STATE_CANCELED || mState == STATE_COMPLETED;
    }

    final ContentResolver getContentResolver() {
        return service.getContentResolver();
    }

    void onFileFailed(DocumentInfo file) {
        failureCount++;
        failedDocs.add(file);
    }

    void onResolveFailed(Uri uri) {
        failureCount++;
        failedUris.add(uri);
    }

    final boolean hasFailures() {
        return failureCount > 0;
    }

    boolean hasWarnings() {
        return false;
    }

    final void deleteDocument(DocumentInfo doc, @Nullable DocumentInfo parent)
            throws ResourceException {
        try {
            if (parent != null && doc.isRemoveSupported()) {
                DocumentsContract.removeDocument(wrap(getClient(doc)), doc.derivedUri,
                        parent.derivedUri);
            } else if (doc.isDeleteSupported()) {
                DocumentsContract.deleteDocument(wrap(getClient(doc)), doc.derivedUri);
            } else {
                throw new ResourceException("Unable to delete source document. "
                        + "File is not deletable or removable: %s.", doc.derivedUri);
            }
        } catch (FileNotFoundException | RemoteException | RuntimeException e) {
            if (e instanceof DeadObjectException) {
                releaseClient(doc);
            }
            throw new ResourceException("Failed to delete file %s due to an exception.",
                    doc.derivedUri, e);
        }
    }

    Notification getSetupNotification(String content) {
        mProgressBuilder.setProgress(0, 0, true)
                .setContentText(content);
        return mProgressBuilder.build();
    }

    Notification getFailureNotification(@PluralsRes int titleId, @DrawableRes int icon) {
        final Intent navigateIntent = buildNavigateIntent(INTENT_TAG_FAILURE);
        navigateIntent.putExtra(FileOperationService.EXTRA_DIALOG_TYPE, OperationDialogFragment.DIALOG_TYPE_FAILURE);
        navigateIntent.putExtra(FileOperationService.EXTRA_OPERATION_TYPE, operationType);
        navigateIntent.putParcelableArrayListExtra(FileOperationService.EXTRA_FAILED_DOCS, failedDocs);
        navigateIntent.putParcelableArrayListExtra(FileOperationService.EXTRA_FAILED_URIS, failedUris);

        final Builder errorBuilder = createNotificationBuilder()
                .setContentTitle(service.getResources().getQuantityString(titleId,
                        failureCount, failureCount))
                .setContentText(service.getString(R.string.notification_touch_for_details))
                .setContentIntent(PendingIntent.getActivity(appContext, 0, navigateIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT
                        | PendingIntent.FLAG_MUTABLE))
                .setCategory(Notification.CATEGORY_ERROR)
                .setSmallIcon(icon)
                .setAutoCancel(true);

        return errorBuilder.build();
    }

    abstract Builder createProgressBuilder();

    final Builder createProgressBuilder(
            String title, @DrawableRes int icon,
            String actionTitle, @DrawableRes int actionIcon) {
        Builder progressBuilder = createNotificationBuilder()
                .setContentTitle(title)
                .setContentIntent(
                        PendingIntent.getActivity(appContext, 0,
                                buildNavigateIntent(INTENT_TAG_PROGRESS),
                                PendingIntent.FLAG_IMMUTABLE))
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setSmallIcon(icon)
                .setOngoing(true);

        final Intent cancelIntent = createCancelIntent();

        progressBuilder.addAction(
                actionIcon,
                actionTitle,
                PendingIntent.getService(
                        service,
                        0,
                        cancelIntent,
                        PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT
                        | PendingIntent.FLAG_MUTABLE));

        return progressBuilder;
    }

    Builder createNotificationBuilder() {
        return mFeatures.isNotificationChannelEnabled()
                ? new Builder(service, FileOperationService.NOTIFICATION_CHANNEL_ID)
                : new Builder(service);
    }

    /**
     * Creates an intent for navigating back to the destination directory.
     */
    Intent buildNavigateIntent(String tag) {
        // TODO (b/35721285): Reuse an existing task rather than creating a new one every time.
        Intent intent = new Intent(service, FilesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(getDataUriForIntent(tag));
        intent.putExtra(Shared.EXTRA_STACK, (Parcelable) stack);
        return intent;
    }

    Intent createCancelIntent() {
        final Intent cancelIntent = new Intent(service, FileOperationService.class);
        cancelIntent.setData(getDataUriForIntent(INTENT_TAG_CANCEL));
        cancelIntent.putExtra(FileOperationService.EXTRA_CANCEL, true);
        cancelIntent.putExtra(FileOperationService.EXTRA_JOB_ID, id);
        return cancelIntent;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Job")
                .append("{")
                .append("id=" + id)
                .append("}")
                .toString();
    }

    /**
     * Listener interface employed by the service that owns us as well as tests.
     */
    interface Listener {
        void onStart(Job job);
        void onFinished(Job job);
    }

    /**
     * Interface for tracking job progress.
     */
    interface ProgressTracker {
        default double getProgress() {  return -1; }
        default long getRemainingTimeEstimate() {
            return -1;
        }
    }
}
