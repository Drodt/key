/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.ast.expr.BlockExpression;
import org.key_project.rusty.logic.RustyBlock;
import org.key_project.rusty.logic.op.BoundVariable;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.logic.op.SubstOp;
import org.key_project.rusty.logic.op.WarySubstOp;
import org.key_project.rusty.util.TacletForTests;
import org.key_project.util.collection.ImmutableSLList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/// Parser tests for de Bruijn substitution: concrete `{\subst …}` parsing, innermost binding of
/// shadowed quantified variables, and the wariness of the substitution operator (a non-rigid
/// replacement is not pushed across a modality). Uses the `testrules.key` signature (sort `s`,
/// predicate `p(s)`, constant `cnst -> s`, program variable `i: u32`).
public class WarySubstAndShadowingTest {

    @BeforeAll
    static void setUp() {
        TacletForTests.parse();
    }

    @Test
    void shadowedQuantifiedVariableBindsInnermost() {
        // the x in p(x) binds to the inner \forall (de Bruijn index 1), not the outer one
        Term t = TacletForTests.parseTerm("\\forall s x;\\forall s x; p(x)");
        Term body = t.sub(0).sub(0); // p(x)
        assertEquals(1, ((LogicVariable) body.sub(0).op()).getIndex());
    }

    @Test
    void parseConcreteSubstitution() {
        Term t = TacletForTests.parseTerm("{\\subst s x; cnst} p(x)");
        assertInstanceOf(SubstOp.class, t.op());
        Term body = t.sub(1);
        assertEquals(1, ((LogicVariable) body.sub(0).op()).getIndex());

        // cnst is rigid, so it is substituted: p(cnst)
        var tb = TacletForTests.services().getTermBuilder();
        Term applied = WarySubstOp.SUBST.apply(t, tb);
        Term expected = TacletForTests.parseTerm("p(cnst)");
        assertEquals(expected, applied);
    }

    @Test
    void nonRigidReplacementIsNotPushedAcrossModality() {
        // {\subst u32 x; i}\<{}\>(x = i): i is a (non-rigid) program variable, so it is not pushed
        // into the modality — the substitution is left as a residual. Built programmatically to
        // avoid the empty-modality concrete syntax.
        var services = TacletForTests.services();
        var tb = services.getTermBuilder();
        ProgramVariable i =
            (ProgramVariable) services.getNamespaces().programVariables().lookup(new Name("i"));
        Sort u32 = i.sort();

        BoundVariable x = new BoundVariable(new Name("x"), u32);
        Term lvX = tb.var(LogicVariable.create(1, u32));
        Term iTerm = tb.var(i);
        RustyBlock emptyBlock =
            new RustyBlock(new BlockExpression(ImmutableSLList.nil(), null));
        Term body = tb.dia(emptyBlock, tb.equals(lvX, iTerm)); // \<{}\>(x = i)
        Term substTerm = tb.subst(WarySubstOp.SUBST, x, iTerm, body);

        Term applied = WarySubstOp.SUBST.apply(substTerm, tb);
        assertEquals(substTerm, applied,
            "a non-rigid replacement must not be pushed across a modality");
    }
}
