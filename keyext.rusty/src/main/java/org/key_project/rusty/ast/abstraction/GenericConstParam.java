/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.Function;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.logic.sort.ConstParam;
import org.key_project.rusty.logic.sort.GenericParameter;

public record GenericConstParam(Name name, Function fn) implements GenericParam {
    @Override
    public GenericParameter toSortParam(Services services) {
        return new ConstParam(name, fn.sort());
    }

    @Override
    public void visit(Visitor v) {

    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
