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
package com.blabla.documentsui.inspector;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.io.FileNotFoundException;

/**
 * Loads metadata from
 * {@link DocumentsContract#getDocumentMetadata(android.content.ContentProviderClient,
 * Uri)}
 */
final class MetadataLoader extends AsyncTaskLoader<Bundle> {

    private static final String TAG = "MetadataLoader";

    private final Uri mUri;
    private final ContentResolver mContentResolver;

    private @Nullable Bundle mMetadata;

    MetadataLoader(Context context, Uri uri, ContentResolver contentResolver) {
        super(context);
        mUri = uri;
        mContentResolver = contentResolver;
    }

    @Override
    public Bundle loadInBackground() {
        try {
            return DocumentsContract.getDocumentMetadata(mContentResolver, mUri);
        } catch (FileNotFoundException | RuntimeException e) {
            Log.e(TAG, "Failed to load metadata for doc: " + mUri, e);
        }

        return null;
    }

    @Override
    protected void onStartLoading() {
        if (mMetadata != null) {
            deliverResult(mMetadata);
        }
        if (takeContentChanged() || mMetadata == null) {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(Bundle metadata) {
        if (isReset()) {
            return;
        }
        mMetadata = metadata;
        if (isStarted()) {
            super.deliverResult(metadata);
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        mMetadata = null;
    }
}
