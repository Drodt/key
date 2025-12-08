/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.jspecify.annotations.Nullable;

public interface ParamName {
    record Plain(Ident ident) implements ParamName {
    }

    record Fresh() implements ParamName {
    }

    record Error() implements ParamName {
    }

    class Adapter extends HirAdapter<ParamName> {

        @Override
        public @Nullable Class<? extends ParamName> getType(String tag) {
            return switch (tag) {
                case "Plain" -> Plain.class;
                case "Fresh" -> Fresh.class;
                case "Error" -> Error.class;
                default -> null;
            };
        }
    }
}
