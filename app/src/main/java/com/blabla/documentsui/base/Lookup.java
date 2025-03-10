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
package com.blabla.documentsui.base;

import java.util.function.Function;
import androidx.annotation.Nullable;

/**
 * A {@link Function}-like interface for looking up information.
 *
 * @param K input type (the "key").
 * @param V output type (the "value").
 */
@FunctionalInterface
public interface Lookup<K, V> {
    @Nullable V lookup(K key);
}
