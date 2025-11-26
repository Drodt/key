/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Map;
import java.util.WeakHashMap;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.rusty.logic.sort.TermArg;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ArrayType implements Type {
    private static final WeakHashMap<TypeAndLen, ArrayType> INSTANCES = new WeakHashMap<>();

    private final Type elementType;
    private final ArrayLen length;
    private final Sort sort;
    private final Name name;

    private ArrayType(Type elementType, ArrayLen length, Services services) {
        this.elementType = elementType;
        this.length = length;
        var psd = services.getNamespaces().parametricSorts().lookup("Array");
        sort = ParametricSortInstance.get(psd,
            ImmutableList.of(new SortArg(elementType.getSort(services)),
                new TermArg(length.toTerm(services))));
        name = new Name("[" + elementType + "; " + length + "]");
    }

    public static ArrayType getInstance(Type elementType, ArrayLen length, Services services) {
        return INSTANCES.computeIfAbsent(new TypeAndLen(elementType, length),
            k -> new ArrayType(elementType, length, services));
    }

    public ArrayLen getLength() {
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
    public @NonNull Name name() {
        return name;
    }

    @Override
    public Type instantiate(Map<GenericParam, GenericTyArg> instMap, Services services) {
        var it = elementType.instantiate(instMap, services);
        if (it == elementType)
            return this;
        return getInstance(it, length, services);
    }

    record TypeAndLen(Type ty, ArrayLen len) {
    }
}
