/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.HashMap;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;

public record GenericEnum(Name name, ImmutableArray<GenericVariant> variants,
        ImmutableArray<GenericParam> params, ParametricSortDecl sortDecl) implements GenericAdt {
    @Override
    public Type instantiate(ImmutableArray<GenericTyArg> args, Services services) {
        assert args.size() == params().size();
        var instMap = new HashMap<GenericParam, GenericTyArg>();
        ImmutableList<GenericArgument> sortArgs = ImmutableSLList.nil();
        for (int i = params().size() - 1; i >= 0; i--) {
            instMap.put(params().get(i), args.get(i));
            sortArgs = sortArgs.prepend(args.get(i).sortArg(services));
        }
        var vars = new Variant[variants.size()];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = variants.get(i).instantiate(instMap, services);
        }
        return new Enum(name, new ImmutableArray<>(vars),
            ParametricSortInstance.get(sortDecl, sortArgs));
    }

    @Override
    public @NonNull String toString() {
        var sb = new StringBuilder();
        sb.append(name);
        if (!params.isEmpty()) {
            sb.append("<");
            sb.append(params.get(0));
            for (int i = 1; i < params.size(); i++) {
                sb.append(", ");
                sb.append(params.get(i));
            }
            sb.append(">");
        }
        return sb.toString();
    }
}
