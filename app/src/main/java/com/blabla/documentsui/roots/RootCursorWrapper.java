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

package com.blabla.documentsui.roots;

import static com.blabla.documentsui.base.SharedMinimal.VERBOSE;

import android.database.AbstractCursor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.blabla.documentsui.base.UserId;

/**
 * Cursor wrapper that adds columns to identify which root a document came from.
 */
public class RootCursorWrapper extends AbstractCursor {
    private final UserId mUserId;
    private final String mAuthority;
    private final String mRootId;

    private final Cursor mCursor;
    private final int mCount;

    private final String[] mColumnNames;

    private final int mAuthorityIndex;
    private final int mRootIdIndex;
    private final int mUserIdIndex;

    public static final String COLUMN_USER_ID = "android:userId";
    public static final String COLUMN_AUTHORITY = "android:authority";
    public static final String COLUMN_ROOT_ID = "android:rootId";
    private static final String TAG = "RootCursorWrapper";

    public RootCursorWrapper(UserId userId, String authority, String rootId, Cursor cursor,
            int maxCount) {
        mUserId = userId;
        mAuthority = authority;
        mRootId = rootId;
        mCursor = cursor;

        final int count = cursor.getCount();
        if (maxCount > 0 && count > maxCount) {
            mCount = maxCount;
        } else {
            mCount = count;
        }

        if (cursor.getColumnIndex(COLUMN_USER_ID) != -1
                || cursor.getColumnIndex(COLUMN_AUTHORITY) != -1
                || cursor.getColumnIndex(COLUMN_ROOT_ID) != -1) {
            throw new IllegalArgumentException("Cursor contains internal columns!");
        }
        final String[] before = cursor.getColumnNames();
        // Create a new columnNames and copy the existing one to it.
        // Add the internal column names to the end of the array.
        mColumnNames = new String[before.length + 3];
        System.arraycopy(before, 0, mColumnNames, 0, before.length);
        mAuthorityIndex = before.length;
        mRootIdIndex = before.length + 1;
        mUserIdIndex = before.length + 2;
        mColumnNames[mAuthorityIndex] = COLUMN_AUTHORITY;
        mColumnNames[mRootIdIndex] = COLUMN_ROOT_ID;
        mColumnNames[mUserIdIndex] = COLUMN_USER_ID;
    }

    @Override
    public Bundle getExtras() {
        Bundle extras = mCursor.getExtras();

        if (extras == null) {
            if (VERBOSE) Log.v(TAG, "Cursor for root " + mRootId + " does not have any extras.");
            return Bundle.EMPTY;
        }

        return extras;
    }

    @Override
    public void close() {
        super.close();
        mCursor.close();
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        return mCursor.moveToPosition(newPosition);
    }

    @Override
    public String[] getColumnNames() {
        return mColumnNames;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public double getDouble(int column) {
        return mCursor.getDouble(column);
    }

    @Override
    public float getFloat(int column) {
        return mCursor.getFloat(column);
    }

    @Override
    public int getInt(int column) {
        if (column == mUserIdIndex) {
            return mUserId.getIdentifier();
        } else {
            return mCursor.getInt(column);
        }
    }

    @Override
    public long getLong(int column) {
        return mCursor.getLong(column);
    }

    @Override
    public short getShort(int column) {
        return mCursor.getShort(column);
    }

    @Override
    public String getString(int column) {
        if (column == mAuthorityIndex) {
            return mAuthority;
        } else if (column == mRootIdIndex) {
            return mRootId;
        } else {
            return mCursor.getString(column);
        }
    }

    @Override
    public int getType(int column) {
        return mCursor.getType(column);
    }

    @Override
    public boolean isNull(int column) {
        return mCursor.isNull(column);
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }
}
