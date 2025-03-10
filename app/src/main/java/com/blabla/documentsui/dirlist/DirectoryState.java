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
package com.blabla.documentsui.dirlist;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.blabla.documentsui.base.DocumentInfo;
import com.blabla.documentsui.base.RootInfo;
import com.blabla.documentsui.base.Shared;
import com.blabla.documentsui.services.FileOperation;
import com.blabla.documentsui.services.FileOperationService;
import com.blabla.documentsui.sorting.SortDimension.SortDirection;
import com.blabla.documentsui.sorting.SortModel;

final class DirectoryState {

    private static final String EXTRA_SORT_DIMENSION_ID = "sortDimensionId";
    private static final String EXTRA_SORT_DIRECTION = "sortDirection";
    private static final String EXTRA_SELECTION_ID = "selectionId";

    // Null when viewing Recents directory.
    @Nullable
    DocumentInfo mDocument;
    // Here we save the clip details of moveTo/copyTo actions when picker shows up.
    // This will be written to saved instance.
    @Nullable
    FileOperation mPendingOperation;
    int mLastSortDimensionId = SortModel.SORT_DIMENSION_ID_UNKNOWN;
    @SortDirection int mLastSortDirection;

    // The unique id to identify the selection. It is null when the corresponding
    // container (fragment/activity) is the first launch.
    @Nullable String mSelectionId;

    private RootInfo mRoot;
    private String mConfigKey;

    public void restore(Bundle bundle) {
        mRoot = bundle.getParcelable(Shared.EXTRA_ROOT);
        mDocument = bundle.getParcelable(Shared.EXTRA_DOC);
        mPendingOperation = bundle.getParcelable(FileOperationService.EXTRA_OPERATION);
        mLastSortDimensionId = bundle.getInt(EXTRA_SORT_DIMENSION_ID);
        mLastSortDirection = bundle.getInt(EXTRA_SORT_DIRECTION);
        mSelectionId = bundle.getString(EXTRA_SELECTION_ID);
    }

    public void save(Bundle bundle) {
        bundle.putParcelable(Shared.EXTRA_ROOT, mRoot);
        bundle.putParcelable(Shared.EXTRA_DOC, mDocument);
        bundle.putParcelable(FileOperationService.EXTRA_OPERATION, mPendingOperation);
        bundle.putInt(EXTRA_SORT_DIMENSION_ID, mLastSortDimensionId);
        bundle.putInt(EXTRA_SORT_DIRECTION, mLastSortDirection);
        bundle.putString(EXTRA_SELECTION_ID, mSelectionId);
    }

    public FileOperation claimPendingOperation() {
        FileOperation op = mPendingOperation;
        mPendingOperation = null;
        return op;
    }

    String getConfigKey() {
        if (mConfigKey == null) {
            final StringBuilder builder = new StringBuilder();
            builder.append(mRoot != null ? mRoot.authority : "null").append(';');
            builder.append(mRoot != null ? mRoot.rootId : "null").append(';');
            builder.append(mDocument != null ? mDocument.documentId : "null");
            mConfigKey = builder.toString();
        }
        return mConfigKey;
    }
}