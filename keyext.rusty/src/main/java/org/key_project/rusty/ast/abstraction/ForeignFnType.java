/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.parser.hir.DefId;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ForeignFnType implements Type {
    private final DefId defId;
    private final ImmutableArray<GenericTyArg> args;
    private final Name name;

    public ForeignFnType(DefId defId, ImmutableArray<GenericTyArg> args) {
        this.defId = defId;
        this.args = args;
        name = new Name("Foreign(" + defId + ")" + args);
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RustType toRustType(Services services) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull Name name() {
        return name;
    }
}
