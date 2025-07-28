/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.Immutables;

import org.jspecify.annotations.NonNull;

public class ParametricSortDecl implements Named {
    private final Name name;
    private final boolean isAbstract;
    private final String documentation;

    private final ImmutableList<ParamSortParam> parameters;

    public ParametricSortDecl(Name name, boolean isAbstract,
            ImmutableList<ParamSortParam> sortParams, String documentation) {
        this.name = name;
        this.isAbstract = isAbstract;
        this.documentation = documentation;
        this.parameters = sortParams;
        assert Immutables.isDuplicateFree(parameters)
                : "The caller should have made sure that generic sorts are not duplicated";
    }

    public ImmutableList<ParamSortParam> getParameters() {
        return parameters;
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public String getDocumentation() {
        return documentation;
    }
}
