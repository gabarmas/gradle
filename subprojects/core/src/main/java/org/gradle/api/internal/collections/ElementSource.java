/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.api.internal.collections;

import org.gradle.api.internal.WithEstimatedSize;

import java.util.Collection;
import java.util.Iterator;

public interface ElementSource<T> extends Iterable<T>, WithEstimatedSize, PendingSource<T> {
    /**
     * Iterates over and realizes each of the elements of this source.
     */
    @Override
    Iterator<T> iterator();

    /**
     * Iterates over only the realized elements (without flushing any pending elements)
     */
    Iterator<T> iteratorNoFlush();

    /**
     * Returns false if this source is not empty or it is not fast to determine this.
     */
    boolean constantTimeIsEmpty();

    int estimatedSize();

    boolean contains(Object element);

    boolean containsAll(Collection<?> elements);

    boolean isEmpty();

    boolean add(T element);

    boolean addRealized(T element);

    void clear();

    boolean remove(Object o);

    int size();
}
