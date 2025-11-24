/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.ParametricFunctionDecl;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.util.collection.ImmutableList;

public record GenericField(Name name, Type type, ParametricFunctionDecl fieldConst) {
    public Field instantiate(Map<GenericTyParam, GenericTyArg> instMap, Services services) {
        Type instantiated = type.instantiate(instMap, services);
        return new Field(name, instantiated, ParametricFunctionInstance.get(fieldConst,
            ImmutableList.of(new SortArg(instantiated.getSort(services)))));
    }
}
