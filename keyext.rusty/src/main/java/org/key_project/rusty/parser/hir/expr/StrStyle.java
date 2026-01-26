/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public interface StrStyle {
    record Cooked() implements StrStyle {
    }

    record Raw(char depth) implements StrStyle {
    }

    class Adapter extends HirAdapter<StrStyle> {
        @Override
        public @Nullable Class<? extends StrStyle> getType(String tag) {
            return switch (tag) {
                case "Cooked" -> Cooked.class;
                case "Raw" -> Raw.class;
                default -> null;
            };
        }
    }
}
