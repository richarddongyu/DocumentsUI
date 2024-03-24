/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.ImageView;

import com.blabla.documentsui.ActionHandler;
import com.blabla.documentsui.R;
import com.blabla.documentsui.base.UserId;

/**
 * An {@link Item} for switch profile. This is only used in pickers.
 */
class ProfileItem extends AppItem {

    public ProfileItem(ResolveInfo info, String title, ActionHandler actionHandler) {
        super(info, title, UserId.CURRENT_USER, actionHandler);
    }

    @Override
    protected void bindIcon(ImageView icon) {
        icon.setImageResource(R.drawable.ic_user_profile);
    }

    @Override
    protected void bindActionIcon(View actionIconArea, ImageView actionIcon) {
        actionIconArea.setVisibility(View.GONE);
    }

    @Override
    public String toString() {
        return "ProfileItem{"
                + "id=" + stringId
                + ", userId=" + userId
                + ", resolveInfo=" + info
                + "}";
    }
}
