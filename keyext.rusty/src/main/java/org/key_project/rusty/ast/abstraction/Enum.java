/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// AN enum with no generic parameters or already instantiated parameters.
public record Enum(Name name, ImmutableArray<Variant> variants,
        @Nullable ParametricSortDecl parametricSortDecl,
        @Nullable ImmutableArray<GenericTyArg> args) implements Type, Adt {
    @Override
    public @Nullable Sort getSort(Services services) {
        if (parametricSortDecl == null)
            return services.getNamespaces().sorts().lookup(name);
        ImmutableList<GenericArgument> args = ImmutableSLList.nil();
        assert this.args != null;
        for (int i = this.args.size() - 1; i >= 0; i--) {
            args = args.prepend(this.args.get(i).sortArg(services));
        }
        return ParametricSortInstance.get(parametricSortDecl, args);
    }

    @Override
    public RustType toRustType(Services services) {
        throw new UnsupportedOperationException("Not supported yet: " + getClass().getSimpleName());
    }

    @Override
    public @NonNull String toString() {
        var sb = new StringBuilder();
        sb.append(name);
        if (args != null) {
            sb.append("<");
            sb.append(args.get(0));
            for (int i = 1; i < args.size(); i++) {
                sb.append(", ");
                sb.append(args.get(i));
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
