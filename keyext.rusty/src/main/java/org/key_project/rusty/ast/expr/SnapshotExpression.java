/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;


import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.logic.op.IProgramVariable;
import org.key_project.util.ExtList;

public class SnapshotExpression implements Expr {

    private final IProgramVariable pv;

    public SnapshotExpression(IProgramVariable pv) {
        this.pv = pv;
    }

    public SnapshotExpression(ExtList children) {
        pv = children.get(IProgramVariable.class);
    }


    @Override
    public void visit(Visitor v) {
        v.performActionOnSnapshotExpression(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("snapshot! (");
        sb.append(pv);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Type type(Services services) {
        return pv.type(services);
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return pv;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 1;
    }


    public IProgramVariable getPv() {
        return pv;
    }
}
