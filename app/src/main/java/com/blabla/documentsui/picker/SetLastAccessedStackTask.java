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

package com.blabla.documentsui.picker;

import android.app.Activity;

import com.blabla.documentsui.base.DocumentStack;
import com.blabla.documentsui.base.PairedTask;

class SetLastAccessedStackTask extends PairedTask<Activity, Void, Void> {

    private final LastAccessedStorage mLastAccessed;
    private final DocumentStack mStack;
    private final Runnable mCallback;

    SetLastAccessedStackTask(
            Activity activity,
            LastAccessedStorage lastAccessed,
            DocumentStack stack,
            Runnable callback) {

        super(activity);
        mLastAccessed = lastAccessed;
        mStack = stack;
        mCallback = callback;
    }

    @Override
    protected Void run(Void... params) {
        mLastAccessed.setLastAccessed(mOwner, mStack);
        return null;
    }

    @Override
    protected void finish(Void result) {
        mCallback.run();
    }
}
