/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast;

import org.key_project.rusty.ast.ty.RustType;

import org.jspecify.annotations.Nullable;

public interface VariableDeclaration {
    @Nullable
    RustType type();
}
