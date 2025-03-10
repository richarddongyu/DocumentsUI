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
import android.text.format.DateFormat;

import com.blabla.documentsui.R;

import java.util.Locale;

/**
 * Helper methods for dealing with dates.
 */
final class DateUtils {
    /**
     * This small helper method combines two different DateFormat subclasses in order to format
     * both the date and the time based on user locale.
     * @param date Unix timestamp
     * @return formatted String of date
     */
    static String formatDate(Context context, long date) {
        Resources res = context.getResources();
        int formatRes = DateFormat.is24HourFormat(context)
                ? R.string.datetime_format_24
                : R.string.datetime_format_12;
        String format = DateFormat.getBestDateTimePattern(
                Locale.getDefault(),
                res.getString(formatRes));
        return DateFormat.format(format, date).toString();
    }
}
