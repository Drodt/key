/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.ast;

import org.key_project.logic.SyntaxElement;

public record PathExprSegment(PathIdentSegment segment) implements SyntaxElement {
    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return segment.toString();
    }
}
