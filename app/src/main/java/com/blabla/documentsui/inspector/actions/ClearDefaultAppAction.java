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
package com.blabla.documentsui.inspector.actions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.StringRes;

import com.blabla.documentsui.R;
import com.blabla.documentsui.base.DocumentInfo;

/**
 * Model for clearing the default app that opens a file.
 */
public final class ClearDefaultAppAction extends Action {

    public ClearDefaultAppAction(Context context, PackageManager pm, DocumentInfo doc) {
        super(context, pm, doc);
    }

    /**
     * @return the header for this action. In English it would be "This file opens with"
     */
    @Override
    public String getHeader() {
        return mContext.getString(R.string.handler_app_file_opens_with);
    }

    @Override
    public int getButtonIcon() {
        return R.drawable.ic_action_clear;
    }

    /**
     * Checks if we can clear the default app to open a file.
     *
     * @return true if we can clear, false if we can't clear.
     */
    @Override
    public boolean canPerformAction() {
        return false;
    }

    @Override
    public String getPackageName() {

        Intent intent = new Intent(Intent.ACTION_VIEW, mDoc.derivedUri);
        ResolveInfo resolveInfo = mPm.resolveActivity(intent, 0);

        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        } else {
            return null;
        }
    }

    public @StringRes int getButtonLabel() {
        return R.string.button_clear;
    }
}