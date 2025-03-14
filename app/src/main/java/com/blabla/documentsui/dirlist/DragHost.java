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

import static com.blabla.documentsui.base.SharedMinimal.DEBUG;

import android.app.Activity;
import android.content.ClipData;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import androidx.recyclerview.selection.SelectionTracker;

import com.blabla.documentsui.AbstractActionHandler;
import com.blabla.documentsui.AbstractDragHost;
import com.blabla.documentsui.ActionHandler;
import com.blabla.documentsui.DragAndDropManager;
import com.blabla.documentsui.Metrics;
import com.blabla.documentsui.R;
import com.blabla.documentsui.base.DocumentInfo;
import com.blabla.documentsui.base.DocumentStack;
import com.blabla.documentsui.base.Lookup;
import com.blabla.documentsui.base.State;
import com.blabla.documentsui.ui.DialogController;
import com.google.android.material.snackbar.Snackbar;

import java.util.function.Predicate;

/**
 * Drag host for items in {@link DirectoryFragment}.
 */
class DragHost<T extends Activity & AbstractActionHandler.CommonAddons> extends AbstractDragHost {

    private static final String TAG = "dirlist.DragHost";

    private final T mActivity;
    private final SelectionTracker<String> mSelectionMgr;
    private final ActionHandler mActions;
    private final State mState;
    private final DialogController mDialogs;
    private final Predicate<View> mIsDocumentView;
    private final Lookup<View, DocumentHolder> mHolderLookup;
    private final Lookup<View, DocumentInfo> mDestinationLookup;

    DragHost(
            T activity,
            DragAndDropManager dragAndDropManager,
            SelectionTracker<String> selectionMgr,
            ActionHandler actions,
            State state,
            DialogController dialogs,
            Predicate<View> isDocumentView,
            Lookup<View, DocumentHolder> holderLookup,
            Lookup<View, DocumentInfo> destinationLookup) {
        super(dragAndDropManager);

        mActivity = activity;
        mSelectionMgr = selectionMgr;
        mActions = actions;
        mState = state;
        mDialogs = dialogs;
        mIsDocumentView = isDocumentView;
        mHolderLookup = holderLookup;
        mDestinationLookup = destinationLookup;
    }

    void dragStopped(boolean result) {
        if (result) {
            mSelectionMgr.clearSelection();
        }
    }

    @Override
    public void runOnUiThread(Runnable runnable) {
        mActivity.runOnUiThread(runnable);
    }

    @Override
    public void setDropTargetHighlight(View v, boolean highlight) {
    }

    @Override
    public void onViewHovered(View v) {
        if (mIsDocumentView.test(v)) {
            mActions.springOpenDirectory(mDestinationLookup.lookup(v));
        }
        mActivity.setRootsDrawerOpen(false);
    }

    @Override
    public void onDragEntered(View v) {
        mActivity.setRootsDrawerOpen(false);
        mDragAndDropManager.updateState(v, mState.stack.getRoot(), mDestinationLookup.lookup(v));
    }

    @Override
    public boolean canHandleDragEvent(View v) {
        boolean dragInitiatedFromDocsUI = mDragAndDropManager.isDragFromSameApp();
        Metrics.logDragInitiated(dragInitiatedFromDocsUI);
        if (!dragInitiatedFromDocsUI) {
            Snackbar.make(
                    v, R.string.drag_from_another_app, Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    boolean canSpringOpen(View v) {
        DocumentInfo doc = mDestinationLookup.lookup(v);
        return (doc != null) && mDragAndDropManager.canSpringOpen(mState.stack.getRoot(), doc);
    }

    boolean handleDropEvent(View v, DragEvent event) {
        mActivity.setRootsDrawerOpen(false);

        ClipData clipData = event.getClipData();
        assert (clipData != null);

        DocumentInfo dst = mDestinationLookup.lookup(v);
        if (dst == null) {
            if (DEBUG) {
                Log.d(TAG, "Invalid destination. Ignoring.");
            }
            return false;
        }

        // If destination is already at top of stack, no need to pass it in
        DocumentStack dstStack = dst.equals(mState.stack.peek())
                ? mState.stack
                : new DocumentStack(mState.stack, dst);
        return mDragAndDropManager.drop(event.getClipData(), event.getLocalState(), dstStack,
                mDialogs::showFileOperationStatus);
    }
}
