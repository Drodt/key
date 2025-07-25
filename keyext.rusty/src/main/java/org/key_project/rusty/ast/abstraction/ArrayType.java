/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.WeakHashMap;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.ArraySort;

import org.jspecify.annotations.Nullable;

public class ArrayType implements Type {
    private static final WeakHashMap<TypeAndLen, ArrayType> INSTANCES = new WeakHashMap<>();

    private final Type elementType;
    private final int length;
    private final Sort sort;
    private final Name name;

    private ArrayType(Type elementType, int length, Services services) {
        this.elementType = elementType;
        this.length = length;
        sort = ArraySort.get(elementType.getSort(services), length);
        name = new Name("[" + elementType + "; " + length + "]");
    }

    public static ArrayType getInstance(Type elementType, int length, Services services) {
        return INSTANCES.computeIfAbsent(new TypeAndLen(elementType, length),
            k -> new ArrayType(elementType, length, services));
    }

    public int getLength() {
        return length;
    }

    public Type getElementType() {
        return elementType;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return sort;
    }

    @Override
    public RustType toRustType(Services services) {
        return null;
    }

    @Override
    public Name name() {
        return name;
    }

    record TypeAndLen(Type ty, int len) {
    }
}
