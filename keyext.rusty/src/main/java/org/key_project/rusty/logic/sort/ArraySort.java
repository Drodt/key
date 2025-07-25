/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import java.util.WeakHashMap;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;

import org.jspecify.annotations.Nullable;

public final class ArraySort extends SortImpl {
    private static final WeakHashMap<ElemSortAndLen, ArraySort> instances = new WeakHashMap<>();

    private final Sort elementSort;
    private final int length;

    private ArraySort(Sort elemSort, int length) {
        super(new Name("[" + elemSort + "; " + length + "]"));
        this.elementSort = elemSort;
        this.length = length;
    }

    public static ArraySort get(Sort elementSort, int length) {
        return instances.computeIfAbsent(new ElemSortAndLen(elementSort, length),
            el -> new ArraySort(elementSort, length));
    }

    public Sort getElementSort() {
        return elementSort;
    }

    public int getLength() {
        return length;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (!(o instanceof ArraySort as))
            return false;
        return as.elementSort.equals(elementSort) && as.length == length;
    }

    record ElemSortAndLen(Sort elemSort, int length) {
    }
}
