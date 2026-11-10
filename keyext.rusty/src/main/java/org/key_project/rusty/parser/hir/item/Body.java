/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.parser.hir.item;

import java.util.Arrays;

import org.key_project.rusty.parser.hir.expr.Expr;

import org.jspecify.annotations.NonNull;

public record Body(Param[] params, Expr value) {
    @Override
    public @NonNull String toString() {
        return "Body{" + "params=" + Arrays.toString(params) + ", value=" + value + '}';
    }
}
