/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record Instantiated(HasGenerics ty, ImmutableArray<GenericTyArg> args) implements Type {
    @Override
    public @Nullable Sort getSort(Services services) {
        return null;
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
