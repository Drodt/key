/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.ast.abstraction;

public record TyConstValue(int value) implements TyConst {
    @Override
    public ArrayLen toLength() {
        return new IntArrayLen(value);
    }
}
