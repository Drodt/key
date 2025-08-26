/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// An enum with no generic parameters or already instantiated parameters.
public record Enum(Name name, ImmutableArray<Variant> variants,
        Sort sort) implements Type, Adt {
    @Override
    public @Nullable Sort getSort(Services services) {
        return sort;
    }

    @Override
    public RustType toRustType(Services services) {
        throw new UnsupportedOperationException("Not supported yet: " + getClass().getSimpleName());
    }

    @Override
    public @NonNull String toString() {
        var sb = new StringBuilder();
        sb.append(name);
        if (sort instanceof ParametricSortInstance psi) {
            sb.append("<");
            sb.append(psi.getArgs().get(0));
            for (int i = 1; i < psi.getArgs().size(); i++) {
                sb.append(", ");
                sb.append(psi.getArgs().get(i));
            }
            sb.append(">");
        }
        return sb.toString();
    }

    @Override
    public Type instantiate(Map<GenericTyParam, GenericTyArg> instMap, Services services) {
        // We are fully instantiated
        return this;
    }
}
