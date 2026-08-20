/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.ParametricFunctionDecl;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.util.collection.ImmutableList;

public record GenericVariant(Name name, ImmutableList<GenericParam> genericParams,
        ImmutableList<GenericField> fields, ParametricFunctionDecl ctor) {
    public Variant instantiate(Map<GenericParam, GenericTyArg> instMap, Services services) {
        var fields = new Field[this.fields.size()];
        for (int i = 0; i < this.fields.size(); i++) {
            fields[i] = this.fields.get(i).instantiate(instMap, services);
        }
        List<GenericArgument> args = new LinkedList<>();
        for (int i = 0; i < genericParams().size(); i++) {
            var param = genericParams.get(i);
            var inst = Objects.requireNonNull(instMap.get(param));
            GenericArgument arg = switch (inst) {
                case GenericTyArgType(var ty) -> new SortArg(ty.getSort(services));
                default -> throw new IllegalStateException("Unexpected value: " + inst);
            };
            args.add(arg);
        }
        return new Variant(name, ImmutableList.fromArray(fields),
            ParametricFunctionInstance.get(ctor, ImmutableList.fromList(args)));
    }
}
