/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.blabla.documentsui.dirlist;

import static com.blabla.documentsui.DevicePolicyResources.Drawables.Style.SOLID_COLORED;
import static com.blabla.documentsui.DevicePolicyResources.Drawables.WORK_PROFILE_ICON;
import static com.blabla.documentsui.base.DocumentInfo.getCursorInt;
import static com.blabla.documentsui.base.DocumentInfo.getCursorLong;
import static com.blabla.documentsui.base.DocumentInfo.getCursorString;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.DocumentsContract.Document;
import android.text.format.Formatter;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.blabla.documentsui.R;
import com.blabla.documentsui.base.DocumentInfo;
import com.blabla.documentsui.base.Shared;
import com.blabla.documentsui.base.UserId;
import com.blabla.documentsui.roots.RootCursorWrapper;
import com.blabla.documentsui.ui.Views;
//import com.android.modules.utils.build.SdkLevel;

import java.util.function.Function;

final class GridDocumentHolder extends DocumentHolder {

    final TextView mTitle;
    final TextView mDate;
    final TextView mDetails;
    final ImageView mIconMimeLg;
    final ImageView mIconMimeSm;
    final ImageView mIconThumb;
    final ImageView mIconCheck;
    final ImageView mIconBriefcase;
    final IconHelper mIconHelper;
    final View mIconLayout;
    final View mPreviewIcon;

    // This is used in as a convenience in our bind method.
    private final DocumentInfo mDoc = new DocumentInfo();

    public GridDocumentHolder(Context context, ViewGroup parent, IconHelper iconHelper) {
        super(context, parent, R.layout.item_doc_grid);

        mIconLayout = itemView.findViewById(R.id.icon);
        mTitle = (TextView) itemView.findViewById(android.R.id.title);
        mDate = (TextView) itemView.findViewById(R.id.date);
        mDetails = (TextView) itemView.findViewById(R.id.details);
        mIconMimeLg = (ImageView) itemView.findViewById(R.id.icon_mime_lg);
        mIconMimeSm = (ImageView) itemView.findViewById(R.id.icon_mime_sm);
        mIconThumb = (ImageView) itemView.findViewById(R.id.icon_thumb);
        mIconCheck = (ImageView) itemView.findViewById(R.id.icon_check);
        mIconBriefcase = (ImageView) itemView.findViewById(R.id.icon_briefcase);
        mPreviewIcon = itemView.findViewById(R.id.preview_icon);

        mIconHelper = iconHelper;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        if (SdkLevel.isAtLeastT()) {
            setUpdatableWorkProfileIcon(context);
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private void setUpdatableWorkProfileIcon(Context context) {
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        Drawable drawable = dpm.getResources().getDrawable(WORK_PROFILE_ICON, SOLID_COLORED, () ->
                context.getDrawable(R.drawable.ic_briefcase));
        mIconBriefcase.setImageDrawable(drawable);
    }

    @Override
    public void setSelected(boolean selected, boolean animate) {
        // We always want to make sure our check box disappears if we're not selected,
        // even if the item is disabled. This is because this object can be reused
        // and this method will be called to setup initial state.
        float checkAlpha = selected ? 1f : 0f;
        if (animate) {
            fade(mIconMimeSm, checkAlpha).start();
            fade(mIconCheck, checkAlpha).start();
        } else {
            mIconCheck.setAlpha(checkAlpha);
        }

        // But it should be an error to be set to selected && be disabled.
        if (!itemView.isEnabled()) {
            assert(!selected);
        }

        super.setSelected(selected, animate);

        if (animate) {
            fade(mIconMimeSm, 1f - checkAlpha).start();
        } else {
            mIconMimeSm.setAlpha(1f - checkAlpha);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        float imgAlpha = enabled ? 1f : DISABLED_ALPHA;

        mIconMimeLg.setAlpha(imgAlpha);
        mIconMimeSm.setAlpha(imgAlpha);
        mIconThumb.setAlpha(imgAlpha);
    }

    @Override
    public void bindPreviewIcon(boolean show, Function<View, Boolean> clickCallback) {
        mPreviewIcon.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            mPreviewIcon.setContentDescription(
                    getPreviewIconContentDescription(
                            mIconHelper.shouldShowBadge(mDoc.userId.getIdentifier()),
                            mDoc.displayName));
            mPreviewIcon.setAccessibilityDelegate(new PreviewAccessibilityDelegate(clickCallback));
        }
    }

    @Override
    public void bindBriefcaseIcon(boolean show) {
        mIconBriefcase.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean inDragRegion(MotionEvent event) {
     // Entire grid box should be draggable
        return true;
    }

    @Override
    public boolean inSelectRegion(MotionEvent event) {
        return Views.isEventOver(event, itemView.getParent(), mIconLayout);
    }

    @Override
    public boolean inPreviewIconRegion(MotionEvent event) {
        return Views.isEventOver(event, itemView.getParent(), mPreviewIcon);
    }

    /**
     * Bind this view to the given document for display.
     * @param cursor Pointing to the item to be bound.
     * @param modelId The model ID of the item.
     */
    @Override
    public void bind(Cursor cursor, String modelId) {
        assert(cursor != null);

        mModelId = modelId;

        mDoc.updateFromCursor(cursor,
                UserId.of(getCursorInt(cursor, RootCursorWrapper.COLUMN_USER_ID)),
                getCursorString(cursor, RootCursorWrapper.COLUMN_AUTHORITY));

        mIconHelper.stopLoading(mIconThumb);

        mIconMimeLg.animate().cancel();
        mIconMimeLg.setAlpha(1f);
        mIconThumb.animate().cancel();
        mIconThumb.setAlpha(0f);

        mIconHelper.load(mDoc, mIconThumb, mIconMimeLg, mIconMimeSm);

        mTitle.setText(mDoc.displayName, TextView.BufferType.SPANNABLE);
        mTitle.setVisibility(View.VISIBLE);

        // If file is partial, we want to show summary field as that's more relevant than fileSize
        // and date
        if (mDoc.isPartial()) {
            final String docSummary = getCursorString(cursor, Document.COLUMN_SUMMARY);
            mDetails.setVisibility(View.VISIBLE);
            mDate.setText(null);
            mDetails.setText(docSummary);
        } else {
            if (mDoc.lastModified == -1) {
                mDate.setText(null);
            } else {
                mDate.setText(Shared.formatTime(mContext, mDoc.lastModified));
            }

            final long docSize = getCursorLong(cursor, Document.COLUMN_SIZE);
            if (mDoc.isDirectory() || docSize == -1) {
                mDetails.setVisibility(View.GONE);
            } else {
                mDetails.setVisibility(View.VISIBLE);
                mDetails.setText(Formatter.formatFileSize(mContext, docSize));
            }
        }
    }
}
