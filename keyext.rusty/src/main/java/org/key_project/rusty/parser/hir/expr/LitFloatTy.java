/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.FloatTy;
import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public interface LitFloatTy {
    record Suffixed(FloatTy ty) implements LitFloatTy {
    }

    record Unsuffixed() implements LitFloatTy {
    }

    class Adapter extends HirAdapter<LitFloatTy> {
        @Override
        public @Nullable Class<? extends LitFloatTy> getType(String tag) {
            return switch (tag) {
                case "Suffixed" -> Suffixed.class;
                case "Unsuffixed" -> Unsuffixed.class;
                default -> null;
            };
        }
    }
}
