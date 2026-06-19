/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.item;


import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.Span;
import org.key_project.rusty.parser.hir.hirty.HirTy;

import org.jspecify.annotations.Nullable;


public sealed interface FnRetTy {
    record Return(HirTy ty) implements FnRetTy {
    }

    record DefaultReturn(Span span) implements FnRetTy {
    }

    class Adapter extends HirAdapter<FnRetTy> {
        @Override
        public @Nullable Class<? extends FnRetTy> getType(String tag) {
            return switch (tag) {
                case "Return" -> Return.class;
                case "DefaultReturn" -> DefaultReturn.class;
                default -> null;
            };
        }
    }
}
