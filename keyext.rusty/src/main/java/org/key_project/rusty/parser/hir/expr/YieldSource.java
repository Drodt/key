/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.HirId;

import org.jspecify.annotations.Nullable;

public sealed interface YieldSource {
    record Await(@Nullable HirId expr) implements YieldSource {
    }

    record Yield() implements YieldSource {
    }

    class Adapter extends HirAdapter<YieldSource> {
        @Override
        public @Nullable Class<? extends YieldSource> getType(String tag) {
            return switch (tag) {
                case "Await" -> Await.class;
                case "Yield" -> Yield.class;
                default -> null;
            };
        }
    }
}
