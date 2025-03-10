/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.blabla.documentsui.prefs;

import static com.blabla.documentsui.base.State.MODE_UNKNOWN;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;

import com.blabla.documentsui.base.RootInfo;
import com.blabla.documentsui.base.State.ViewMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Methods for accessing the local preferences.
 */
public class LocalPreferences {
    private static final String ROOT_VIEW_MODE_PREFIX = "rootViewMode-";
    private static final String SHOW_HIDDEN_FILES = "showHiddenFiles";

    public static @ViewMode int getViewMode(Context context, RootInfo root,
            @ViewMode int fallback) {
        return getPrefs(context).getInt(createKey(ROOT_VIEW_MODE_PREFIX, root), fallback);
    }

    /** Returns if hidden files should be shown. */
    public static boolean getShowHiddenFiles(Context context, boolean fallback) {
        return getPrefs(context).getBoolean(SHOW_HIDDEN_FILES, fallback);
    }

    public static void setViewMode(Context context, RootInfo root, @ViewMode int viewMode) {
        assert(viewMode != MODE_UNKNOWN);
        getPrefs(context).edit().putInt(createKey(ROOT_VIEW_MODE_PREFIX, root), viewMode).apply();
    }

    /** Sets if hidden files should be shown. */
    @VisibleForTesting
    public static void setShowHiddenFiles(Context context, boolean showHiddenFiles) {
        getPrefs(context).edit()
                .putBoolean(SHOW_HIDDEN_FILES, showHiddenFiles)
                .apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String createKey(String prefix, RootInfo root) {
        return prefix + root.authority + root.rootId;
    }

    public static final int PERMISSION_ASK = 0;
    public static final int PERMISSION_ASK_AGAIN = 1;
    public static final int PERMISSION_NEVER_ASK = -1;

    @IntDef(flag = true, value = {
            PERMISSION_ASK,
            PERMISSION_ASK_AGAIN,
            PERMISSION_NEVER_ASK,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PermissionStatus {}

    public static boolean shouldBackup(String s) {
        return (s != null) ? s.startsWith(ROOT_VIEW_MODE_PREFIX) : false;
    }
}
