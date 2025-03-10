/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.blabla.documentsui;

import static com.blabla.documentsui.base.SharedMinimal.DEBUG;
import static com.blabla.documentsui.base.SharedMinimal.VERBOSE;

import android.app.AuthenticationRequiredException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.selection.Selection;

import com.blabla.documentsui.base.DocumentFilters;
import com.blabla.documentsui.base.DocumentInfo;
import com.blabla.documentsui.base.EventListener;
import com.blabla.documentsui.base.Features;
import com.blabla.documentsui.base.UserId;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The data model for the current loaded directory.
 */
@VisibleForTesting
public class Model {

    private static final String TAG = "Model";

    public @Nullable String info;
    public @Nullable String error;
    public @Nullable DocumentInfo doc;

    private final Features mFeatures;

    /** Maps Model ID to cursor positions, for looking up items by Model ID. */
    private final Map<String, Integer> mPositions = new HashMap<>();
    private final Set<String> mFileNames = new HashSet<>();

    private boolean mIsLoading;
    private List<EventListener<Update>> mUpdateListeners = new ArrayList<>();
    private @Nullable Cursor mCursor;
    private int mCursorCount;
    private String mIds[] = new String[0];

    public Model(Features features) {
        mFeatures = features;
    }

    public void addUpdateListener(EventListener<Update> listener) {
        mUpdateListeners.add(listener);
    }

    public void removeUpdateListener(EventListener<Update> listener) {
        mUpdateListeners.remove(listener);
    }

    private void notifyUpdateListeners() {
        for (EventListener<Update> handler: mUpdateListeners) {
            handler.accept(Update.UPDATE);
        }
    }

    private void notifyUpdateListeners(Exception e) {
        Update error = new Update(e, mFeatures.isRemoteActionsEnabled());
        for (EventListener<Update> handler: mUpdateListeners) {
            handler.accept(error);
        }
    }

    public void reset() {
        mCursor = null;
        mCursorCount = 0;
        mIds = new String[0];
        mPositions.clear();
        info = null;
        error = null;
        doc = null;
        mIsLoading = false;
        mFileNames.clear();
        notifyUpdateListeners();
    }

    @VisibleForTesting
    public void update(DirectoryResult result) {
        assert(result != null);
        if (DEBUG) {
            Log.i(TAG, "Updating model with new result set.");
        }

        if (result.exception != null) {
            Log.e(TAG, "Error while loading directory contents", result.exception);
            reset(); // Resets this model to avoid access to old cursors.
            notifyUpdateListeners(result.exception);
            return;
        }

        mCursor = result.getCursor();
        mCursorCount = mCursor.getCount();
        doc = result.doc;

        if (result.getModelIds() != null && result.getFileNames() != null) {
            mIds = result.getModelIds();
            mFileNames.clear();
            mFileNames.addAll(result.getFileNames());

            // Populate the positions.
            mPositions.clear();
            for (int i = 0; i < mCursorCount; ++i) {
                mPositions.put(mIds[i], i);
            }
        }

        final Bundle extras = mCursor.getExtras();
        if (extras != null) {
            info = extras.getString(DocumentsContract.EXTRA_INFO);
            error = extras.getString(DocumentsContract.EXTRA_ERROR);
            mIsLoading = extras.getBoolean(DocumentsContract.EXTRA_LOADING, false);
        }

        notifyUpdateListeners();
    }

    @VisibleForTesting
    public int getItemCount() {
        return mCursorCount;
    }

    public boolean hasFileWithName(String name) {
        return mFileNames.contains(name);
    }

    public @Nullable Cursor getItem(String modelId) {
        Integer pos = mPositions.get(modelId);
        if (pos == null) {
            if (DEBUG) {
                Log.d(TAG, "Unabled to find cursor position for modelId: " + modelId);
            }
            return null;
        }

        if (!mCursor.moveToPosition(pos)) {
            if (DEBUG) {
                Log.d(TAG,
                    "Unabled to move cursor to position " + pos + " for modelId: " + modelId);
            }
            return null;
        }

        return mCursor;
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public List<DocumentInfo> getDocuments(Selection<String> selection) {
        return loadDocuments(selection, DocumentFilters.ANY);
    }

    public @Nullable DocumentInfo getDocument(String modelId) {
        final Cursor cursor = getItem(modelId);
        return (cursor == null)
                ? null
                : DocumentInfo.fromDirectoryCursor(cursor);
    }

    public List<DocumentInfo> loadDocuments(Selection<String> selection, Predicate<Cursor> filter) {
        final int size = (selection != null) ? selection.size() : 0;

        final List<DocumentInfo> docs =  new ArrayList<>(size);
        DocumentInfo doc;
        for (String modelId: selection) {
            doc = loadDocument(modelId, filter);
            if (doc != null) {
                docs.add(doc);
            }
        }
        return docs;
    }

    public boolean hasDocuments(Selection<String> selection, Predicate<Cursor> filter) {
        for (String modelId: selection) {
            if (loadDocument(modelId, filter) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return DocumentInfo, or null. If filter returns false, null will be returned.
     */
    private @Nullable DocumentInfo loadDocument(String modelId, Predicate<Cursor> filter) {
        final Cursor cursor = getItem(modelId);

        if (cursor == null) {
            Log.w(TAG, "Unable to obtain document for modelId: " + modelId);
            return null;
        }

        if (filter.test(cursor)) {
            return DocumentInfo.fromDirectoryCursor(cursor);
        }

        if (VERBOSE) Log.v(TAG, "Filtered out document from results: " + modelId);
        return null;
    }

    public Uri getItemUri(String modelId) {
        final Cursor cursor = getItem(modelId);
        return DocumentInfo.getUri(cursor);
    }

    public UserId getItemUserId(String modelId) {
        final Cursor cursor = getItem(modelId);
        return DocumentInfo.getUserId(cursor);
    }

    /**
     * @return An ordered array of model IDs representing the documents in the model. It is sorted
     *         according to the current sort order, which was set by the last model update.
     */
    public String[] getModelIds() {
        return mIds;
    }

    public static class Update {

        public static final Update UPDATE = new Update();

        @IntDef(value = {
                TYPE_UPDATE,
                TYPE_UPDATE_EXCEPTION
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface UpdateType {}
        public static final int TYPE_UPDATE = 0;
        public static final int TYPE_UPDATE_EXCEPTION = 1;

        private final @UpdateType int mUpdateType;
        private final @Nullable Exception mException;
        private final boolean mRemoteActionEnabled;

        private Update() {
            mUpdateType = TYPE_UPDATE;
            mException = null;
            mRemoteActionEnabled = false;
        }

        public Update(Exception exception, boolean remoteActionsEnabled) {
            assert(exception != null);
            mUpdateType = TYPE_UPDATE_EXCEPTION;
            mException = exception;
            mRemoteActionEnabled = remoteActionsEnabled;
        }

        public boolean isUpdate() {
            return mUpdateType == TYPE_UPDATE;
        }

        public boolean hasException() {
            return mUpdateType == TYPE_UPDATE_EXCEPTION;
        }

        public boolean hasAuthenticationException() {
            return mRemoteActionEnabled
                    && hasException()
                    && mException instanceof AuthenticationRequiredException;
        }

        public boolean hasCrossProfileException() {
            return mRemoteActionEnabled
                    && hasException()
                    && mException instanceof CrossProfileException;
        }

        public @Nullable Exception getException() {
            return mException;
        }
    }
}
