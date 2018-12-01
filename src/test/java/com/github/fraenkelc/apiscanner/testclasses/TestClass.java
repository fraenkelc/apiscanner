/*
 * Copyright 2018 Christian Fraenkel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.fraenkelc.apiscanner.testclasses;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.tree.TreeModel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@TestClassRetentionAnnotation(enumValue = TestEnum.ONE)
public class TestClass {
    private ConcurrentHashMap privateHashMap = null;
    protected SortedSet<Exception> protectedSortedSet = null;
    public SortedMap<Runnable, TreeModel> publicSortedMap = null;

    private static ConcurrentHashMap privateStaticHashMap = null;
    protected static SortedSet<Exception> protectedStaticSortedSet = null;
    public static SortedMap<Runnable, TreeModel> publicStaticSortedMap = null;

    private static final ConcurrentHashMap privateStaticFinalHashMap = null;
    protected static final SortedSet<Exception> protectedStaticFinalSortedSet = null;
    public static final SortedMap<Runnable, TreeModel> publicStaticFinalSortedMap = null;

    public static StringBuffer returnsStringBuffer() {
        return null;
    }

    public StringBuilder returnsStringBuilder() {
        return null;
    }

    private String privateMethod() {
        return null;
    }

    public void returnsVoid() {

    }

    @Nullable
    protected HashMap<String, Map<Set<System>, Override>> protectedMethodWithGenerics() {
        return null;
    }

    public @Nullable
    Map<String, String> publicMapWithArguments(@Nonnull String test) {
        return null;
    }
}
