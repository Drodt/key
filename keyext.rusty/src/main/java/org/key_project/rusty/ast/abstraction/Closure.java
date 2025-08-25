/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.RustType;

import org.jspecify.annotations.NonNull;

public record Closure() implements Type {
    @Override
    public Sort getSort(Services services) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RustType toRustType(Services services) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public @NonNull Name name() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Type instantiate(Map<GenericTyParam, GenericTyArg> instMap, Services services) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
