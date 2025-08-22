/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.GenericParameter;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;

public class Enum implements Adt, Named, HasGenerics {
    private final Name name;
    private final ImmutableArray<Variant> variants;
    private final ImmutableArray<GenericTyParam> params;

    public Enum(String pathStr, ImmutableArray<Variant> variants,
            ImmutableArray<GenericTyParam> params) {
        name = new Name(pathStr);
        this.variants = variants;
        this.params = params;
    }

    public ImmutableArray<GenericTyParam> params() {
        return params;
    }

    @Override
    public ParametricSortDecl sortDecl(Services services) {
        if (params.isEmpty())
            return null;
        ImmutableList<GenericParameter> sortParams = ImmutableSLList.nil();
        for (int i = params.size() - 1; i >= 0; i--) {
            sortParams = sortParams.prepend(params.get(i).toSortParam(services));
        }
        var psd = new ParametricSortDecl(name, false, sortParams, null);
        var alreadyDefined = services.getNamespaces().parametricSorts().lookup(psd.name());
        if (alreadyDefined != null) {
            return alreadyDefined;
        } else {
            services.getNamespaces().parametricSorts().addSafely(psd);
            return psd;
        }
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    @Override
    public String toString() {
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
