/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.ty;

import org.key_project.logic.TerminalSyntaxElement;
import org.key_project.rusty.ast.abstraction.Never;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

public class NeverRustType implements RustType, TerminalSyntaxElement {
    @Override
    public Type type() {
        return Never.INSTANCE;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnNeverRustType(this);
    }
}
