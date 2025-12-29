/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class GenericGhostType implements GenericAdt {
    private final ParametricSortDecl ghostSort;
    private final ImmutableArray<GenericParam> params;
    private final Name name = new Name("rml_contracts::Ghost");

    public GenericGhostType(ImmutableArray<GenericParam> params, Services services) {
        ghostSort = services.getLDTs().getGhostLDT().parametricSort();
        this.params = params;
    }

    @Override
    public @Nullable ParametricSortDecl sortDecl() {
        return ghostSort;
    }

    @Override
    public Type instantiate(ImmutableArray<GenericTyArg> args, Services services) {
        return GhostType.get(((GenericTyArgType) args.get(0)).type());
    }

    @Override
    public ImmutableArray<GenericParam> params() {
        return params;
    }

    @Override
    public @NonNull Name name() {
        return name;
    }
}
