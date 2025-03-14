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

package com.blabla.documentsui;

import android.graphics.Point;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.AbsListView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * This class acts as a middle-man handler for potential auto-scrolling before passing the dragEvent
 * onto {@link ItemDragListener}.
 */
public class DragHoverListener implements OnDragListener {

    private final ItemDragListener<? extends ItemDragListener.DragHost> mDragHandler;
    private final IntSupplier mHeight;
    private final BooleanSupplier mCanScrollUp;
    private final BooleanSupplier mCanScrollDown;
    private final Runnable mDragScroller;

    /**
     * Predicate to tests whether it's the scroll view itself.
     *
     * {@link DragHoverListener} is used for both the scroll view and its children.
     * When we decide whether it's in the scroll zone we need to obtain the coordinate
     * relative to container view so we need to transform the coordinate if the view
     * that gets drag and drop events is a child of scroll view.
     */
    private final Predicate<View> mIsScrollView;

    private boolean mDragHappening;
    private @Nullable Point mCurrentPosition;

    @VisibleForTesting
    DragHoverListener(
            ItemDragListener<? extends ItemDragListener.DragHost> dragHandler,
            IntSupplier heightSupplier,
            Predicate<View> isScrollView,
            BooleanSupplier scrollUpSupplier,
            BooleanSupplier scrollDownSupplier,
            ViewAutoScroller.ScrollerCallbacks scrollCallbacks) {

        mDragHandler = dragHandler;
        mHeight = heightSupplier;
        mIsScrollView = isScrollView;
        mCanScrollUp = scrollUpSupplier;
        mCanScrollDown = scrollDownSupplier;

        ViewAutoScroller.ScrollHost scrollHost = new ViewAutoScroller.ScrollHost() {
            @Override
            public Point getCurrentPosition() {
                return mCurrentPosition;
            }

            @Override
            public int getViewHeight() {
                return mHeight.getAsInt();
            }

            @Override
            public boolean isActive() {
                return mDragHappening;
            }
        };

        mDragScroller = new ViewAutoScroller(scrollHost, scrollCallbacks);
    }

    public static DragHoverListener create(
            ItemDragListener<? extends ItemDragListener.DragHost> dragHandler,
            AbsListView scrollView) {
        return create(dragHandler, scrollView, scrollView::scrollListBy);
    }

    public static DragHoverListener create(
            ItemDragListener<? extends ItemDragListener.DragHost> dragHandler,
            View scrollView) {
        return create(
                dragHandler,
                scrollView,
                (int dy) -> {
                    scrollView.scrollBy(0, dy);
                });
    }

    static DragHoverListener create(
            ItemDragListener<? extends ItemDragListener.DragHost> dragHandler,
            View scrollView,
            IntConsumer scroller) {

        ViewAutoScroller.ScrollerCallbacks scrollCallbacks = new ViewAutoScroller.ScrollerCallbacks() {
            @Override
            public void scrollBy(int dy) {
                scroller.accept(dy);
            }

            @Override
            public void runAtNextFrame(Runnable r) {
                scrollView.postOnAnimation(r);
            }

            @Override
            public void removeCallback(Runnable r) {
                scrollView.removeCallbacks(r);
            }
        };

        return new DragHoverListener(
                dragHandler,
                scrollView::getHeight,
                (view) -> (scrollView == view),
                () -> scrollView.canScrollVertically(-1),
                () -> scrollView.canScrollVertically(1),
                scrollCallbacks);
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                mDragHappening = true;
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                mDragHappening = false;
                break;
            case DragEvent.ACTION_DRAG_LOCATION:
                handleLocationEvent(v, event.getX(), event.getY());
                break;
            default:
                break;
        }

        // Always forward events to the drag handler for item highlight, spring load, etc.
        return mDragHandler.onDrag(v, event);
    }

    private boolean handleLocationEvent(View v, float x, float y) {
        mCurrentPosition = transformToScrollViewCoordinate(v, x, y);
        if (insideDragZone()) {
            mDragScroller.run();
            return true;
        }
        return false;
    }

    private Point transformToScrollViewCoordinate(View v, float x, float y) {
        // Check if v is the scroll view itself. If not we need to transform the coordinate to
        // relative to the scroll view because we need to test the scroll zone in the coordinate
        // relative to the scroll view; if yes we don't need to transform coordinates.
        final boolean isScrollView = mIsScrollView.test(v);
        final float offsetX = isScrollView ? 0 : v.getX();
        final float offsetY = isScrollView ? 0 : v.getY();
        return new Point(Math.round(offsetX + x), Math.round(offsetY + y));
    }

    private boolean insideDragZone() {
        if (mCurrentPosition == null) {
            return false;
        }

        float topBottomRegionHeight = mHeight.getAsInt()
                * ViewAutoScroller.TOP_BOTTOM_THRESHOLD_RATIO;
        boolean shouldScrollUp = mCurrentPosition.y < topBottomRegionHeight
                && mCanScrollUp.getAsBoolean();
        boolean shouldScrollDown = mCurrentPosition.y > mHeight.getAsInt() - topBottomRegionHeight
                && mCanScrollDown.getAsBoolean();
        return shouldScrollUp || shouldScrollDown;
    }
}
