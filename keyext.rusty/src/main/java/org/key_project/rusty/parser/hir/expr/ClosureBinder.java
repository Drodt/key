/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.Span;

import org.jspecify.annotations.Nullable;

public interface ClosureBinder {
    record Default() implements ClosureBinder {
    }

    record For(Span span) implements ClosureBinder {
    }

    class Adapter extends HirAdapter<ClosureBinder> {
        @Override
        public @Nullable Class<? extends ClosureBinder> getType(String tag) {
            return switch (tag) {
                case "Default" -> Default.class;
                case "For" -> For.class;
                default -> null;
            };
        }
    }
}
