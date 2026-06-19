/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.jspecify.annotations.Nullable;

public sealed interface PreciseCapturingArg {
    record Lifetime(org.key_project.rusty.parser.hir.Lifetime lifetime)
            implements PreciseCapturingArg {
    }

    record Param(PreciseCapturingNonLifetimeArg arg) implements PreciseCapturingArg {
    }

    class Adapter extends HirAdapter<PreciseCapturingArg> {
        @Override
        public @Nullable Class<? extends PreciseCapturingArg> getType(String tag) {
            return switch (tag) {
                case "Lifetime" -> Lifetime.class;
                case "Param" -> Param.class;
                default -> null;
            };
        }
    }
}
