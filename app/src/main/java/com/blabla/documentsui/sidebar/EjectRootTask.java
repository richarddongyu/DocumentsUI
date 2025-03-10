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

package com.blabla.documentsui.sidebar;

import static android.content.ContentResolver.wrap;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.util.Log;

import com.blabla.documentsui.DocumentsApplication;
import com.blabla.documentsui.base.BooleanConsumer;

public final class EjectRootTask extends AsyncTask<Void, Void, Boolean> {

    private final String TAG = "EjectRootTask";

    private final ContentResolver mResolver;
    private final String mAuthority;
    private final String mRootId;
    private final BooleanConsumer mCallback;

    /**
     * @param finishCallback The end callback necessary when the eject task finishes
     */
    public EjectRootTask(
            ContentResolver resolver,
            String authority,
            String rootId,
            BooleanConsumer finishCallback) {
        mResolver = resolver;
        mAuthority = authority;
        mRootId = rootId;
        mCallback = finishCallback;
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        Uri rootUri = DocumentsContract.buildRootUri(mAuthority, mRootId);
        ContentProviderClient client = null;
        try {
            client = DocumentsApplication.acquireUnstableProviderOrThrow(
                    mResolver, mAuthority);
            DocumentsContract.ejectRoot(wrap(client), rootUri);
            return true;
        } catch (IllegalStateException e) {
            Log.w(TAG, "Failed to eject root.", e);
        } catch (Exception e) {
            Log.w(TAG, "Binder call failed.", e);
        } finally {
            FileUtils.closeQuietly(client);
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean ejected) {
        mCallback.accept(ejected);
    }
}