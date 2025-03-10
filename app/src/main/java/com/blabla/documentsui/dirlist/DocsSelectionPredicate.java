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
package com.blabla.documentsui.dirlist;

import static androidx.core.util.Preconditions.checkArgument;

import android.database.Cursor;
import android.provider.DocumentsContract.Document;
import android.util.Log;

import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate;
import androidx.recyclerview.widget.RecyclerView;

import com.blabla.documentsui.ActivityConfig;
import com.blabla.documentsui.Model;
import com.blabla.documentsui.base.State;
import com.blabla.documentsui.base.DocumentInfo;

/**
 * Class embodying the logic as to whether an item (specified by id or position)
 * can be selected (or not).
 */
final class DocsSelectionPredicate extends SelectionPredicate<String> {

    private ActivityConfig mConfig;
    private Model mModel;
    private RecyclerView mRecView;
    private State mState;

    DocsSelectionPredicate(
            ActivityConfig config, State state, Model model, RecyclerView recView) {

        checkArgument(config != null);
        checkArgument(state != null);
        checkArgument(model != null);
        checkArgument(recView != null);

        mConfig = config;
        mState = state;
        mModel = model;
        mRecView = recView;

    }

    @Override
    public boolean canSetStateForKey(String id, boolean nextState) {
        if (nextState) {
            // Check if an item can be selected
            final Cursor cursor = mModel.getItem(id);
            if (cursor == null) {
                Log.w(DirectoryFragment.TAG, "Couldn't obtain cursor for id: " + id);
                return false;
            }

            final String docMimeType = DocumentInfo.getCursorString(cursor, Document.COLUMN_MIME_TYPE);
            final int docFlags = DocumentInfo.getCursorInt(cursor, Document.COLUMN_FLAGS);
            return mConfig.canSelectType(docMimeType, docFlags, mState);
        }

        // Right now all selected items can be deselected.
        return true;
    }

    @Override
    public boolean canSetStateAtPosition(int position, boolean nextState) {
        // This method features a nextState arg for symmetry.
        // But, there are no current uses for checking un-selecting state by position.
        // So rather than have some unsuspecting client think canSetState(int, false)
        // will ever do anything. Let's just be grumpy about it.
        assert nextState == true;

        // NOTE: Given that we have logic in some places disallowing selection,
        // it may be a bug that Band and Gesture based selections don't
        // also verify something can be unselected.

        // The band selection model only operates on documents and directories.
        // Exclude other types of adapter items like whitespace and dividers.
        final RecyclerView.ViewHolder vh = mRecView.findViewHolderForAdapterPosition(position);
        if (vh == null) {
            return false;
        } else {
            return ModelBackedDocumentsAdapter.isContentType(vh.getItemViewType());
        }
    }

    @Override
    public boolean canSelectMultiple() {
        return mState.allowMultiple;
    }
}
