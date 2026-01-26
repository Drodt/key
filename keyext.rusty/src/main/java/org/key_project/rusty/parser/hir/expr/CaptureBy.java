/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.Span;

import org.jspecify.annotations.Nullable;

public interface CaptureBy {
    record Value(Span moveKw) implements CaptureBy {
    }
    record Ref() implements CaptureBy {
    }
    record Use(Span useKw) implements CaptureBy {
    }

    class Adapter extends HirAdapter<CaptureBy> {
        @Override
        public @Nullable Class<? extends CaptureBy> getType(String tag) {
            return switch (tag) {
                case "Value" -> Value.class;
                case "Ref" -> Ref.class;
                case "Use" -> Use.class;
                default -> null;
            };
        }
    }
}
