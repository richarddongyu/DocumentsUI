package com.blabla.documentsui;

import android.database.Cursor;
import android.provider.DocumentsContract;

import com.blabla.documentsui.base.UserId;
import com.blabla.documentsui.roots.RootCursorWrapper;
import com.blabla.documentsui.base.DocumentInfo;

public class ModelId {

    public static final String build(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        return ModelId.build(UserId.of(DocumentInfo.getCursorInt(cursor, RootCursorWrapper.COLUMN_USER_ID)),
                DocumentInfo.getCursorString(cursor, RootCursorWrapper.COLUMN_AUTHORITY),
                DocumentInfo.getCursorString(cursor, DocumentsContract.Document.COLUMN_DOCUMENT_ID));
    }

    public static final String build(UserId userId, String authority, String docId) {
        if (userId == null || authority == null || authority.isEmpty() || docId == null
                || docId.isEmpty()) {
            return null;
        }
        return userId + "|" + authority + "|" + docId;
    }
}
