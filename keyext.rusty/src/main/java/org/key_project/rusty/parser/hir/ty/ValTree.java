/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.HirAdapter;

import org.jspecify.annotations.Nullable;

public interface ValTree {
    record Leaf(ScalarInt scalarInt) implements ValTree {
    }

    record Branch(ValTree[] branches) implements ValTree {
    }

    class Adapter extends HirAdapter<ValTree> {
        @Override
        public @Nullable Class<? extends ValTree> getType(String tag) {
            return switch (tag) {
                case "Leaf" -> Leaf.class;
                case "Branch" -> Branch.class;
                default -> null;
            };
        }
    }
}
