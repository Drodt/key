/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.DefId;

import org.jspecify.annotations.Nullable;

public record TyGenerics(@Nullable DefId parent, int parentCount, TyGenericParamDef[] params,
        boolean hasSelf) {
    public boolean isEmpty() {
        return parentCount == 0 && params.length == 0;
    }
}
