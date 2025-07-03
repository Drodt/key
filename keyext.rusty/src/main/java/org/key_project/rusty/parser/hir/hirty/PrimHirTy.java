/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.hirty;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public interface PrimHirTy {
    record Int(IntTy ty) implements PrimHirTy {
    }

    record Uint(UintTy ty) implements PrimHirTy {
    }

    record Bool() implements PrimHirTy {
    }

    class Adapter extends HirAdapter<PrimHirTy> {
        @Override
        public @Nullable Class<? extends PrimHirTy> getType(String tag) {
            return switch (tag) {
            case "Int" -> Int.class;
            case "Uint" -> Uint.class;
            case "Bool" -> Bool.class;
            default -> null;
            };
        }
    }
}
