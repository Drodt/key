/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.ast.ty.TupleRustType;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TupleType implements Type {
    public static final TupleType UNIT = new TupleType();
    private static @Nullable Map<List<Type>, TupleType> TYPES = null;

    private final List<Type> types;
    private @MonotonicNonNull Sort sort = null;
    private final ImmutableArray<Field> fields;

    private TupleType() {
        types = new ArrayList<>();
        fields = new ImmutableArray<>();
    }

    private TupleType(List<Type> types, Services services) {
        this.types = types;

        var tupleLDT = services.getLDTs().getTupleLDT();
        var fields = new Field[types.size()];
        for (int i = 0; i < types.size(); i++) {
            var fn = tupleLDT.getFieldFunctions(i);
            Type type = types.get(i);
            ImmutableList<GenericArgument> args =
                ImmutableList.of(new SortArg(type.getSort(services)));
            fields[i] = new Field(new Name("" + i), type, ParametricFunctionInstance.get(fn, args));
        }
        this.fields = new ImmutableArray<>(fields);
    }

    public static TupleType getInstance(List<Type> types, Services services) {
        if (types.isEmpty()) {
            return UNIT;
        }
        if (TYPES == null) {
            TYPES = new HashMap<>();
        }
        return TYPES.computeIfAbsent(types, t -> new TupleType(types, services));
    }

    public List<Type> getTypes() {
        return types;
    }

    public ImmutableArray<Field> fields() {
        return fields;
    }

    @Override
    public Sort getSort(Services services) {
        if (sort == null) {
            if (types.isEmpty()) {
                return services.getNamespaces().sorts().lookup("Unit");
            }
            ImmutableList<GenericArgument> args = ImmutableList.of();
            for (int i = types.size() - 1; i >= 0; i--) {
                args = args.prepend(new SortArg(types.get(i).getSort(services)));
            }
            var psd = services.getNamespaces().parametricSorts().lookup("Tuple" + types.size());
            if (psd == null) {
                throw new UnsupportedOperationException(
                    "We do not (yet) support tuples of length " + types.size());
            }
            sort = ParametricSortInstance.get(psd, args);
        }
        return sort;
    }

    @Override
    public @NonNull Name name() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RustType toRustType(Services services) {
        return new TupleRustType(
            new ImmutableArray<>(types.stream().map(t -> t.toRustType(services)).toList()),
            services);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(types.get(i).toString());
        }
        return sb.append(")").toString();
    }

    @Override
    public Type instantiate(Map<GenericTyParam, GenericTyArg> instMap, Services services) {
        var its = new ArrayList<Type>();
        var changed = false;
        for (Type ty : types) {
            var it = ty.instantiate(instMap, services);
            if (it != ty)
                changed = true;
            its.add(it);
        }
        if (!changed)
            return this;
        return getInstance(its, services);
    }
}
