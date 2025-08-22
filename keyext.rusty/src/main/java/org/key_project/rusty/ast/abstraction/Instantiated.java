/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;

public record Instantiated(HasGenerics ty, ImmutableArray<GenericTyArg> args) implements Type {
    @Override
    public @NonNull Sort getSort(Services services) {
        var psd = ty.sortDecl(services);
        if (psd == null) {
            return Objects.requireNonNull(services.getNamespaces().sorts().lookup(ty.name()));
        } else {
            ImmutableList<GenericArgument> sortArgs = ImmutableList.of();
            for (int i = args.size() - 1; i >= 0; i--) {
                sortArgs = sortArgs.prepend(args.get(i).sortArg(services));
            }
            return ParametricSortInstance.get(psd, sortArgs);
        }
    }

    @Override
    public RustType toRustType(Services services) {
        return null;
    }

    @Override
    public @NonNull Name name() {
        var sb = new StringBuilder();
        sb.append(ty.name());
        if (!args.isEmpty()) {
            sb.append("<");
            sb.append(args.get(0));
            for (int i = 1; i < args.size(); i++) {
                sb.append(", ");
                sb.append(args.get(i));
            }
            sb.append(">");
        }
        return new Name(sb.toString());
    }
}
