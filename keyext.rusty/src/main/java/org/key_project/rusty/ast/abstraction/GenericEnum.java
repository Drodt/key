/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.HashMap;

import org.key_project.logic.Name;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.GenericParameter;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;

public record GenericEnum(Name name, ImmutableArray<Variant> variants,
        ImmutableArray<GenericTyParam> params) implements GenericAdt {
    public GenericEnum(String name, ImmutableArray<Variant> variants,
            ImmutableArray<GenericTyParam> params) {
        this(new Name(name), variants, params);
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
    public Type instantiate(ImmutableArray<GenericTyArg> args, Services services) {
        assert args.size() == params().size();
        var instMap = new HashMap<GenericTyParam, GenericTyArg>();
        for (int i = 0; i < params().size(); i++) {
            instMap.put(params().get(i), args.get(i));
        }
        var vars = new Variant[variants.size()];
        for (int i = 0; i < vars.length; i++) {
            vars[i] = variants.get(i).instantiate(instMap, services);
        }
        return new Enum(name, new ImmutableArray<>(vars), sortDecl(services), args);
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
