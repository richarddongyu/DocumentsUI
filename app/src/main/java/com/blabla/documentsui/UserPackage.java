/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static androidx.core.util.Preconditions.checkNotNull;

import com.blabla.documentsui.base.UserId;

import java.util.Objects;

/**
 * Data class storing a user id and a package name.
 */
public class UserPackage {
    final UserId userId;
    final String packageName;

    public UserPackage(UserId userId, String packageName) {
        this.userId = checkNotNull(userId);
        this.packageName = checkNotNull(packageName);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (o instanceof UserPackage) {
            UserPackage other = (UserPackage) o;
            return Objects.equals(userId, other.userId)
                    && Objects.equals(packageName, other.packageName);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, packageName);
    }
}
