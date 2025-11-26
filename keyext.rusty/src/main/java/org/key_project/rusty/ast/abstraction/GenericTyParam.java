/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.GenericParameter;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.GenericSortParam;

import org.jspecify.annotations.Nullable;

public record GenericTyParam(Name name, GenericSort sort)
        implements Named, Type, GenericParam {
    @Override
    public GenericParameter toSortParam(Services services) {
        return new GenericSortParam(sort);
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return sort;
    }

    @Override
    public RustType toRustType(Services services) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type instantiate(Map<GenericParam, GenericTyArg> instMap, Services services) {
        var arg = instMap.get(this);
        return switch (arg) {
            case null -> this;
            case GenericTyArgType(var ty) -> ty;
            default -> throw new IllegalArgumentException(
                String.format("Unrecognized argument type: %s", arg));
        };
    }
}
