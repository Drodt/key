/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import com.google.gson.annotations.SerializedName;

public interface GenericTyArgKind {
    record Lifetime() implements GenericTyArgKind {
    }

    record Type(Ty ty) implements GenericTyArgKind {
    }

    record Const(@SerializedName("const") TyConst _const) implements GenericTyArgKind {
    }
}
