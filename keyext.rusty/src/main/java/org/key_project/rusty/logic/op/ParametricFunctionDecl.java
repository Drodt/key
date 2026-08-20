/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.Sorted;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.sort.GenericParameter;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ParametricFunctionDecl implements Named, Sorted {
    private final Name name;
    private final ImmutableList<GenericParameter> parameters;
    private final ImmutableList<Sort> argSorts;
    private final Sort sort;
    private final @Nullable ImmutableList<Boolean> whereToBind;
    private final boolean unique;
    private final boolean isRigid;
    private final boolean isSkolemConstant;

    public ParametricFunctionDecl(Name name, ImmutableList<GenericParameter> parameters,
            ImmutableList<Sort> argSorts, Sort sort,
            @Nullable ImmutableList<Boolean> whereToBind, boolean unique, boolean isRigid,
            boolean isSkolemConstant) {
        this.name = name;
        this.parameters = parameters;
        this.argSorts = argSorts;
        this.sort = sort;
        this.whereToBind = whereToBind;
        this.unique = unique;
        this.isRigid = isRigid;
        this.isSkolemConstant = isSkolemConstant;
    }

    @Override
    public @NonNull Sort sort() {
        return sort;
    }

    public ImmutableList<Sort> argSorts() {
        return argSorts;
    }

    public @Nullable ImmutableList<Boolean> getWhereToBind() {
        return whereToBind;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isRigid() {
        return isRigid;
    }

    public boolean isSkolemConstant() {
        return isSkolemConstant;
    }

    public ImmutableList<GenericParameter> getParameters() {
        return parameters;
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString() + "<" + parameters.toString() + ">";
    }
}
