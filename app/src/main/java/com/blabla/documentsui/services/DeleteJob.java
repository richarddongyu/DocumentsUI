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

import android.app.Notification;
import android.app.Notification.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.blabla.documentsui.MetricConsts;
import com.blabla.documentsui.Metrics;
import com.blabla.documentsui.R;
import com.blabla.documentsui.base.DocumentInfo;
import com.blabla.documentsui.base.DocumentStack;
import com.blabla.documentsui.base.Features;
import com.blabla.documentsui.base.UserId;
import com.blabla.documentsui.clipping.UrisSupplier;
import com.blabla.documentsui.base.SharedMinimal;

import java.io.FileNotFoundException;

final class DeleteJob extends ResolvedResourcesJob {

    private static final String TAG = "DeleteJob";

    private final Uri mParentUri;

    private volatile int mDocsProcessed = 0;

    /**
     * Moves files to a destination identified by {@code destination}.
     * Performs most work by delegating to CopyJob, then deleting
     * a file after it has been copied.
     *
     * @see @link {@link Job} constructor for most param descriptions.
     */
    DeleteJob(Context service, Listener listener, String id, DocumentStack stack,
              UrisSupplier srcs, @Nullable Uri srcParent, Features features) {
        super(service, listener, id, FileOperationService.OPERATION_DELETE, stack, srcs, features);
        mParentUri = srcParent;
    }

    @Override
    Builder createProgressBuilder() {
        return super.createProgressBuilder(
                service.getString(R.string.delete_notification_title),
                R.drawable.ic_menu_delete,
                service.getString(android.R.string.cancel),
                R.drawable.ic_cab_cancel);
    }

    @Override
    public Notification getSetupNotification() {
        return getSetupNotification(service.getString(R.string.delete_preparing));
    }

    @Override
    public Notification getProgressNotification() {
        mProgressBuilder.setProgress(mResourceUris.getItemCount(), mDocsProcessed, false);
        String format = service.getString(R.string.delete_progress);
        mProgressBuilder.setSubText(
                String.format(format, mDocsProcessed, mResourceUris.getItemCount()));

        mProgressBuilder.setContentText(null);

        return mProgressBuilder.build();
    }

    @Override
    Notification getFailureNotification() {
        return getFailureNotification(
                R.plurals.delete_error_notification_title, R.drawable.ic_menu_delete);
    }

    @Override
    Notification getWarningNotification() {
        throw new UnsupportedOperationException();
    }

    @Override
    void start() {
        ContentResolver resolver = appContext.getContentResolver();

        DocumentInfo parentDoc;
        try {
            parentDoc = mParentUri != null
                ? DocumentInfo.fromUri(resolver, mParentUri, UserId.DEFAULT_USER)
                : null;
        } catch (FileNotFoundException e) {
          Log.e(TAG, "Failed to resolve parent from Uri: " + mParentUri + ". Cannot continue.", e);
          failureCount += this.mResourceUris.getItemCount();
          return;
        }

        for (DocumentInfo doc : mResolvedDocs) {
            if (SharedMinimal.DEBUG) {
                Log.d(TAG, "Deleting document @ " + doc.derivedUri);
            }
            try {
                deleteDocument(doc, parentDoc);
            } catch (ResourceException e) {
                Metrics.logFileOperationFailure(
                        appContext, MetricConsts.SUBFILEOP_DELETE_DOCUMENT, doc.derivedUri);
                Log.e(TAG, "Failed to delete document @ " + doc.derivedUri, e);
                onFileFailed(doc);
            }

            mDocsProcessed++;
            if (isCanceled()) {
                return;
            }
        }

        Metrics.logFileOperation(operationType, mResolvedDocs, null);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("DeleteJob")
                .append("{")
                .append("id=" + id)
                .append(", uris=" + mResourceUris)
                .append(", docs=" + mResolvedDocs)
                .append(", srcParent=" + mParentUri)
                .append(", location=" + stack)
                .append("}")
                .toString();
    }
}
