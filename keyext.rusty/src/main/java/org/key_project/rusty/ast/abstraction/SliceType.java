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
import org.key_project.rusty.ast.ty.SliceRustType;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SliceType implements Type {
    private static final WeakHashMap<Type, SliceType> INSTANCES = new WeakHashMap<>();
    private final Type inner;
    private final Name name;

    private SliceType(Type inner) {
        this.inner = inner;
        name = new Name("[" + inner.toString() + "]");
    }

    public static SliceType get(Type inner) {
        return INSTANCES.computeIfAbsent(inner, k -> new SliceType(inner));
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RustType toRustType(Services services) {
        return new SliceRustType(inner.toRustType(services));
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    @Override
    public Type instantiate(Map<GenericParam, GenericTyArg> instMap, Services services) {
        var it = inner.instantiate(instMap, services);
        if (it == inner)
            return this;
        return get(it);
    }
}
