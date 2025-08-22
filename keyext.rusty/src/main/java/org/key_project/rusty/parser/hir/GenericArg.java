/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.key_project.rusty.parser.hir.hirty.HirTy;

import org.jspecify.annotations.Nullable;

public interface GenericArg {
    record Lifetime(org.key_project.rusty.parser.hir.Lifetime lifetime) implements GenericArg {

    }

    record Type(HirTy ty) implements GenericArg {
    }

    record Const(ConstArg c) implements GenericArg {
    }

    record Infer() implements GenericArg {
    }

    class Adapter extends HirAdapter<GenericArg> {
        @Override
        public @Nullable Class<? extends GenericArg> getType(String tag) {
            return switch (tag) {
                case "Lifetime" -> Lifetime.Lifetime.class;
                case "Type" -> Type.class;
                case "Const" -> Const.class;
                case "Infer" -> Infer.class;
                default -> null;
            };
        }
    }
}
