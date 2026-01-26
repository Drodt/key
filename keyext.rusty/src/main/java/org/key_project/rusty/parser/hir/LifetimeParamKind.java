/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.jspecify.annotations.Nullable;

public interface LifetimeParamKind {
    record Explicit() implements LifetimeParamKind {
    }

    record Elided(MissingLifetimeKind kind) implements LifetimeParamKind {
    }

    record Error() implements LifetimeParamKind {
    }

    class Adapter extends HirAdapter<LifetimeParamKind> {
        @Override
        public @Nullable Class<? extends LifetimeParamKind> getType(String tag) {
            return switch (tag) {
                case "Explicit" -> Explicit.class;
                case "Elided" -> Elided.class;
                case "Error" -> Error.class;
                default -> null;
            };
        }
    }
}
