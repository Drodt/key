/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.HashMap;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public record GenericStruct(Name name, ImmutableArray<GenericField> fields,
        ImmutableArray<GenericParam> params, ParametricSortDecl sortDecl) implements GenericAdt {
    @Override
    public Type instantiate(ImmutableArray<GenericTyArg> args, Services services) {
        assert args.size() == params().size();
        var instMap = new HashMap<GenericParam, GenericTyArg>();
        for (int i = 0; i < params().size(); i++) {
            instMap.put(params().get(i), args.get(i));
        }
        var fs = new Field[fields.size()];
        for (int i = 0; i < fs.length; i++) {
            fs[i] = fields.get(i).instantiate(instMap, services);
        }
        return new Struct(name, new ImmutableArray<>(fs), sortDecl, args);
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
