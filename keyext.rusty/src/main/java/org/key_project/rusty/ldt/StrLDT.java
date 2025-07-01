/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ldt;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.expr.BinaryExpression;
import org.key_project.rusty.ast.expr.LiteralExpression;

public class StrLDT extends LDT {
    public static final Name NAME = new Name("str");

    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    public StrLDT(Services services) {
        super(NAME, services);
    }

    @Override
    public Term translateLiteral(LiteralExpression lit, Services services) {
        return null;
    }

    @Override
    public Function getFunctionFor(BinaryExpression.Operator op, Services services) {
        return null;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term[] subs, Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term sub, Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term left, Term right,
            Services services) {
        return false;
    }

    @Override
    public Name name() {
        return NAME;
    }
}
