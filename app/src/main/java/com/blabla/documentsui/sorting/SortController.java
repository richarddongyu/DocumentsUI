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

package com.blabla.documentsui.sorting;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.blabla.documentsui.BaseActivity;
import com.blabla.documentsui.Injector;
import com.blabla.documentsui.MetricConsts;
import com.blabla.documentsui.Metrics;
import com.blabla.documentsui.R;
import com.blabla.documentsui.base.State;
import com.blabla.documentsui.base.State.ViewMode;

/**
 * A high level controller that manages sort widgets. This is useful when sort widgets can and will
 * appear in different locations in the UI, like the menu, above the file list (pinned) and embedded
 * at the top of file list... and maybe other places too.
 */
public final class SortController {

    private final @Nullable WidgetController mTableHeaderController;

    public SortController(@Nullable WidgetController tableHeaderController) {

        mTableHeaderController = tableHeaderController;
    }

    public void onViewModeChanged(@ViewMode int mode) {
        // in phone layouts we only ever have the dropdown sort controller.
        if (mTableHeaderController == null) {
            return;
        }

        // in tablet mode, we have fancy pants tabular header.
        mTableHeaderController.setVisibility(mode == State.MODE_LIST ? View.VISIBLE : View.GONE);
    }

    public void destroy() {
        if (mTableHeaderController != null) {
            mTableHeaderController.destroy();
        }
    }

    public static SortController create(
            FragmentActivity activity,
            @ViewMode int initialMode,
            SortModel sortModel) {

        final Injector<?> injector = ((BaseActivity)activity).getInjector();
        sortModel.setMetricRecorder((SortDimension dimension) -> {
            int sortType = MetricConsts.USER_ACTION_UNKNOWN;
            int demensionId = dimension.getId();
            if (demensionId == SortModel.SORT_DIMENSION_ID_TITLE) {
                sortType = MetricConsts.USER_ACTION_SORT_NAME;
            } else if (demensionId == SortModel.SORT_DIMENSION_ID_SIZE) {
                sortType = MetricConsts.USER_ACTION_SORT_SIZE;
            } else if (demensionId == SortModel.SORT_DIMENSION_ID_DATE) {
                sortType = MetricConsts.USER_ACTION_SORT_DATE;
            } else if (demensionId == SortModel.SORT_DIMENSION_ID_FILE_TYPE) {
                sortType = MetricConsts.USER_ACTION_SORT_TYPE;
            }
//            switch (dimension.getId()) {
//                case SortModel.SORT_DIMENSION_ID_TITLE:
//                    sortType = MetricConsts.USER_ACTION_SORT_NAME;
//                    break;
//                case SortModel.SORT_DIMENSION_ID_SIZE:
//                    sortType = MetricConsts.USER_ACTION_SORT_SIZE;
//                    break;
//                case SortModel.SORT_DIMENSION_ID_DATE:
//                    sortType = MetricConsts.USER_ACTION_SORT_DATE;
//                    break;
//                case SortModel.SORT_DIMENSION_ID_FILE_TYPE:
//                    sortType = MetricConsts.USER_ACTION_SORT_TYPE;
//                    break;
//            }

            Metrics.logUserAction(sortType);
            if (injector.pickResult != null) {
                injector.pickResult.increaseActionCount();
            }
        });

        SortController controller = new SortController(
                TableHeaderController.create(sortModel, activity.findViewById(R.id.table_header)));

        controller.onViewModeChanged(initialMode);
        return controller;
    }

    public interface WidgetController {
        void setVisibility(int visibility);
        void destroy();
    }
}
