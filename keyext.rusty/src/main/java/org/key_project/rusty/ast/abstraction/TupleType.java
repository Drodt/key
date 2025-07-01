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
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

public class TupleType implements Type {
    public static final TupleType UNIT = new TupleType(new ArrayList<>());
    private static @Nullable Map<List<Type>, TupleType> TYPES = null;

    private List<Type> types;

    private TupleType(List<Type> types) {
        this.types = types;
    }

    public static TupleType getInstance(List<Type> types) {
        if (types.isEmpty()) {
            return UNIT;
        }
        if (TYPES == null) {
            TYPES = new HashMap<>();
        }
        return TYPES.computeIfAbsent(types, t -> new TupleType(types));
    }

    public List<Type> getTypes() {
        return types;
    }

    @Override
    public Sort getSort(Services services) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Name name() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RustType toRustType(Services services) {
        return new TupleRustType(
            new ImmutableArray<>(types.stream().map(t -> t.toRustType(services)).toList()));
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
}
