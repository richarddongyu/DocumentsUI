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

package com.blabla.documentsui.roots;

import static com.blabla.documentsui.base.SharedMinimal.DEBUG;

import android.app.Activity;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.Nullable;

import com.blabla.documentsui.AbstractActionHandler.CommonAddons;
import com.blabla.documentsui.base.PairedTask;
import com.blabla.documentsui.base.RootInfo;
import com.blabla.documentsui.base.UserId;

public class LoadRootTask<T extends Activity & CommonAddons>
        extends PairedTask<T, Void, RootInfo> {
    private static final String TAG = "LoadRootTask";

    protected final ProvidersAccess mProviders;
    private final Uri mRootUri;
    private final UserId mUserId;
    private final LoadRootCallback mCallback;

    public LoadRootTask(
            T activity,
            ProvidersAccess providers,
            Uri rootUri,
            UserId userId,
            LoadRootCallback callback) {
        super(activity);
        mProviders = providers;
        mRootUri = rootUri;
        mUserId = userId;
        mCallback = callback;
    }

    @Override
    protected RootInfo run(Void... params) {
        if (DEBUG) {
            Log.d(TAG, "Loading root: " + mRootUri + " on user " + mUserId);
        }

        return mProviders.getRootOneshot(mUserId, mRootUri.getAuthority(), getRootId(mRootUri));
    }

    @Override
    protected void finish(RootInfo root) {
        if (root != null) {
            if (DEBUG) {
                Log.d(TAG, "Loaded root: " + root);
            }
        } else {
            Log.w(TAG, "Failed to find root: " + mRootUri + " on user " + mUserId);
        }

        mCallback.onRootLoaded(root);
    }

    protected String getRootId(Uri rootUri) {
        return DocumentsContract.getRootId(rootUri);
    }

    /**
     * Callback for task finished.
     */
    @FunctionalInterface
    public interface LoadRootCallback {
        /**
         * Return the RootInfo of input uri, null if the uri is invalid.
         */
        void onRootLoaded(@Nullable RootInfo root);
    }
}
