/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;
import static com.blabla.documentsui.DevicePolicyResources.Strings.PERSONAL_TAB;
import static com.blabla.documentsui.DevicePolicyResources.Strings.WORK_TAB;

import android.app.admin.DevicePolicyManager;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.blabla.documentsui.R;
import com.blabla.documentsui.base.State;
import com.blabla.documentsui.base.UserId;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts user-specific lists of items into a single merged list appropriate for displaying in the
 * UI, including the relevant headers.
 */
class UserItemsCombiner {

    private UserId mCurrentUser;
    private final Resources mResources;
    private final DevicePolicyManager mDpm;
    private final State mState;
    private List<Item> mRootList;
    private List<Item> mRootListOtherUser;

    UserItemsCombiner(Resources resources, DevicePolicyManager dpm, State state) {
        mCurrentUser = UserId.CURRENT_USER;
        mResources = checkNotNull(resources);
        mDpm = dpm;
        mState = checkNotNull(state);
    }

    @VisibleForTesting
    UserItemsCombiner overrideCurrentUserForTest(UserId userId) {
        mCurrentUser = checkNotNull(userId);
        return this;
    }

    UserItemsCombiner setRootListForCurrentUser(List<Item> rootList) {
        mRootList = checkNotNull(rootList);
        return this;
    }

    UserItemsCombiner setRootListForOtherUser(List<Item> rootList) {
        mRootListOtherUser = checkNotNull(rootList);
        return this;
    }

    /**
     * Returns a combined lists from the provided lists. {@link HeaderItem}s indicating profile
     * will be added if the list of current user and the other user are not empty.
     */
    public List<Item> createPresentableList() {
        checkArgument(mRootList != null, "RootListForCurrentUser is not set");
        checkArgument(mRootListOtherUser != null, "setRootListForOtherUser is not set");

        final List<Item> result = new ArrayList<>();
        if (mState.supportsCrossProfile() && mState.canShareAcrossProfile) {
            if (!mRootList.isEmpty() && !mRootListOtherUser.isEmpty()) {
                // Identify personal and work root list.
                final List<Item> personalRootList;
                final List<Item> workRootList;
                if (mCurrentUser.isSystem()) {
                    personalRootList = mRootList;
                    workRootList = mRootListOtherUser;
                } else {
                    personalRootList = mRootListOtherUser;
                    workRootList = mRootList;
                }
                result.add(new HeaderItem(getEnterpriseString(
                        PERSONAL_TAB, R.string.personal_tab)));
                result.addAll(personalRootList);
                result.add(new HeaderItem(getEnterpriseString(WORK_TAB, R.string.work_tab)));
                result.addAll(workRootList);
            } else {
                result.addAll(mRootList);
                result.addAll(mRootListOtherUser);
            }
        } else {
            result.addAll(mRootList);
        }
        return result;
    }

    private String getEnterpriseString(String updatableStringId, int defaultStringId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        if (SdkLevel.isAtLeastT()) {
            return getUpdatableEnterpriseString(updatableStringId, defaultStringId);
        } else {
            return mResources.getString(defaultStringId);
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private String getUpdatableEnterpriseString(String updatableStringId, int defaultStringId) {
        return mDpm.getResources().getString(
                updatableStringId, () -> mResources.getString(defaultStringId));
    }
}
