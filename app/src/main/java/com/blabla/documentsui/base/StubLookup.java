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
package com.blabla.documentsui.base;

/**
 * Lookup that always returns null.
 * @param <K> input type (the "key") which implements {@link Lookup}.
 * @param <V> output type (the "value") which implements {@link Lookup}.
 */
public final class StubLookup<K, V> implements Lookup<K, V> {
    @Override
    public V lookup(K key) {
        return null;
    }
}
