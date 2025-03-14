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
package com.blabla.documentsui.inspector;

import android.content.Context;
import android.content.res.Resources;
import android.text.Selection;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.textclassifier.TextClassifier;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.blabla.documentsui.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Organizes and Displays the basic details about a file
 */
public class TableView extends LinearLayout implements InspectorController.TableDisplay {

    private final LayoutInflater mInflater;

    private final Map<CharSequence, KeyValueRow> mRows = new HashMap<>();
    private final Resources mRes;
    private final Map<CharSequence, TextView> mTitles = new HashMap<>();
    private final TextClassifier mClassifier;

    public TableView(Context context) {
        this(context, null);
    }

    public TableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRes = context.getResources();
        mClassifier = GpsCoordinatesTextClassifier.create(context);
    }

    void setTitle(@StringRes int title, boolean showDivider) {
        putTitle(mRes.getString(title), showDivider);
    }

    // A naughty title method (that takes strings, not message ids), mostly for DebugView.
    protected void putTitle(CharSequence title, boolean showDivider) {
        TextView view = mTitles.get(title);
        if (view == null) {
            LinearLayout layout =
                (LinearLayout) mInflater.inflate(R.layout.inspector_section_title, null);
            if (!showDivider) {
                layout.setDividerDrawable(null);
            }
            view = (TextView) layout.findViewById(R.id.inspector_header_title);
            addView(layout);
            mTitles.put(title, view);
        }
        view.setText(title);
        view.setCustomSelectionActionModeCallback(
                new HeaderTextSelector(view, this::selectText));
        view.setVisibility(title.toString().isEmpty() ? GONE : VISIBLE);
    }

    private void selectText(Spannable text, int start, int stop) {
        Selection.setSelection(text, start, stop);
    }

    protected KeyValueRow createKeyValueRow(ViewGroup parent) {
        KeyValueRow row = (KeyValueRow) mInflater.inflate(R.layout.table_key_value_row, null);
        parent.addView(row);
        row.setTextClassifier(mClassifier);
        return row;
    }

    /**
     * Puts or updates a value in the table view.
     */
    @Override
    public void put(@StringRes int keyId, CharSequence value) {
        put(mRes.getString(keyId), value);
    }

    /**
     * Puts or updates a value in the table view.
     */
    protected KeyValueRow put(CharSequence key, CharSequence value) {
        KeyValueRow row = mRows.get(key);

        if (row == null) {
            row = createKeyValueRow(this);
            row.setKey(key);
            mRows.put(key, row);
        } else if (row.hasOnClickListeners()) {
            row.removeOnClickListener();
        }

        row.setValue(value);
        row.setTextClassifier(mClassifier);
        return row;
    }

    @Override
    public void put(@StringRes int keyId, CharSequence value, OnClickListener callback) {
        put(keyId, value);
        mRows.get(mRes.getString(keyId)).setOnClickListener(callback);
    }

    @Override
    public boolean isEmpty() {
        return mRows.isEmpty();
    }

    @Override
    public void setVisible(boolean visible) {
        setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
