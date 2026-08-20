/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// A struct with no generic parameters or already instantiated parameters.
public record Struct(Name name, ImmutableList<Field> fields,
        @Nullable ParametricSortDecl parametricSortDecl,
        @Nullable ImmutableList<GenericTyArg> args) implements Type, Adt {
    @Override
    public @Nullable Sort getSort(Services services) {
        if (parametricSortDecl == null)
            return services.getNamespaces().sorts().lookup(name);
        List<GenericArgument> args = new LinkedList<>();
        assert this.args != null;
        for (int i = 0; i < this.args.size(); i++) {
            args.add(this.args.get(i).sortArg(services));
        }
        return ParametricSortInstance.get(parametricSortDecl, ImmutableList.fromList(args));
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
    public Type instantiate(Map<GenericParam, GenericTyArg> instMap, Services services) {
        // We are fully instantiated
        return this;
    }
}
