/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.PtrRustType;
import org.key_project.rusty.ast.ty.RustType;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PtrType implements Type {
    private final Name name;
    private final Type inner;

    private PtrType(Type inner) {
        this.inner = inner;
        name = new Name("*" + inner.name());
    }

    public static PtrType get(Type inner) {
        return new PtrType(inner);
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RustType toRustType(Services services) {
        return new PtrRustType(inner.toRustType(services));
    }

    @Override
    public @NonNull Name name() {
        return name;
    }
}
