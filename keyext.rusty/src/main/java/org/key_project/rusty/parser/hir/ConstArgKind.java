/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.jspecify.annotations.Nullable;

public interface ConstArgKind {
    record Anon(AnonConst ac) implements ConstArgKind {
    }

    record Path(QPath path) implements ConstArgKind {
    }

    record Infer(Span span) implements ConstArgKind {
    }

    class Adapter extends HirAdapter<ConstArgKind> {
        @Override
        public @Nullable Class<? extends ConstArgKind> getType(String tag) {
            return switch (tag) {
                case "Anon" -> Anon.class;
                case "Path" -> Path.class;
                case "Infer" -> Infer.class;
                default -> null;
            };
        }
    }
}
