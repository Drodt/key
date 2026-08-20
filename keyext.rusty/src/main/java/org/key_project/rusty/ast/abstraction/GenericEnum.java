/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;

public record GenericEnum(Name name, ImmutableList<GenericVariant> variants,
        ImmutableList<GenericParam> params, ParametricSortDecl sortDecl) implements GenericAdt {
    @Override
    public Type instantiate(ImmutableList<GenericTyArg> args, Services services) {
        assert args.size() == params().size();
        var instMap = new HashMap<GenericParam, GenericTyArg>();
        List<GenericArgument> sortArgs = new LinkedList<>();
        for (int i = 0; i < params().size(); i++) {
            instMap.put(params().get(i), args.get(i));
            sortArgs.add(args.get(i).sortArg(services));
        }
        var vars = new Variant[variants.size()];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = variants.get(i).instantiate(instMap, services);
        }
        return new Enum(name, ImmutableList.fromArray(vars),
            ParametricSortInstance.get(sortDecl, ImmutableList.fromList(sortArgs)));
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
