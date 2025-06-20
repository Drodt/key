/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.conditions;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.op.SchemaVariableFactory;
import de.uka.ilkd.key.logic.op.UpdateSV;
import de.uka.ilkd.key.pp.LogicPrinter;
import de.uka.ilkd.key.rule.MatchConditions;
import de.uka.ilkd.key.rule.TacletForTests;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import org.key_project.logic.Name;
import org.key_project.logic.op.sv.SchemaVariable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDropEffectlessElementary {

    @Test
    public void testSelfAssignments() {

        JTerm term = TacletForTests.parseTerm("{ i := i }(i=0)");
        JTerm result = applyDrop(term);
        assertEquals(term, result);

        term = TacletForTests.parseTerm("{ i := i || i := 0 }(i=0)");
        result = applyDrop(term);
        JTerm expected = TacletForTests.parseTerm("{i:=0}(i=0)");
        assertEquals(expected, result);

        term = TacletForTests.parseTerm("{ i := 0 || i := i }(i=0)");
        result = applyDrop(term);
        expected = TacletForTests.parseTerm("{i:=i}(i=0)");
        assertEquals(expected, result);
    }

    @Test
    public void testDoubleAssignment() {

        JTerm term = TacletForTests.parseTerm("{ i := j || j := i }(i=0)");
        JTerm result = applyDrop(term);
        JTerm expected = TacletForTests.parseTerm("{i := j}(i=0)");
        assertEquals(expected, result);

        term = TacletForTests.parseTerm("{ j := 5 || j := j }(i=0)");
        result = applyDrop(term);
        expected = TacletForTests.parseTerm("(i=0)");
        assertEquals(expected, result);

        term = TacletForTests.parseTerm("{ i:=i || j := 5 || i:=i || j := j }(i=0)");
        result = applyDrop(term);
        expected = TacletForTests.parseTerm("{i:=i}(i=0)");
        assertEquals(expected, result);
    }

    // this was bug #1269
    @Test
    public void testFaultyCase() {
        // The parser cannot parse this but this can appear as
        // result of the sequential to parallel of {i:=i+1}{i:=i}

        JTerm term;
        // Term term = TacletForTests.parseTerm("{ {i := i+1}i:=i }(i=0)");
        {
            JTerm t0 = TacletForTests.parseTerm("{i := i+1}0").sub(0);
            JTerm t1 = TacletForTests.parseTerm("{i := i}0").sub(0);
            JTerm t2 = TacletForTests.parseTerm("i=0");
            TermBuilder tb = TacletForTests.services().getTermBuilder();

            JTerm t3 = tb.apply(t0, t1, null);
            term = tb.apply(t3, t2, null);
        }
        assertEquals("{{i:=i + 1}i:=i}(i = 0)",
            LogicPrinter.quickPrintTerm(term, TacletForTests.services));

        JTerm result = applyDrop(term);
        assertEquals(term, result);
    }

    // the following cannot be parsed apparently.
    // public void testUpdatedUpdate() throws Exception {
    // Term term = TacletForTests.parseTerm("({i:=i}{i := i})(i=0)");
    // Term result = applyDrop(term);
    // Term expected = TacletForTests.parseTerm("i=0");
    // assertEquals(expected, result);
    //
    // term = TacletForTests.parseTerm("({i:=i}{j:=5})(i=0)");
    // result = applyDrop(term);
    // expected = TacletForTests.parseTerm("(i=0)");
    // assertEquals(expected, result);
    // }

    private JTerm applyDrop(JTerm term) {

        JTerm update = term.sub(0);
        JTerm arg = term.sub(1);

        UpdateSV u = SchemaVariableFactory.createUpdateSV(new Name("u"));
        SchemaVariable x = SchemaVariableFactory.createFormulaSV(new Name("x"));
        SchemaVariable result = SchemaVariableFactory.createFormulaSV(new Name("result"));
        DropEffectlessElementariesCondition cond =
            new DropEffectlessElementariesCondition(u, x, result);

        SVInstantiations svInst = SVInstantiations.EMPTY_SVINSTANTIATIONS;
        svInst = svInst.add(u, update, TacletForTests.services());
        svInst = svInst.add(x, arg, TacletForTests.services());

        MatchConditions mc = MatchConditions.EMPTY_MATCHCONDITIONS.setInstantiations(svInst);
        // first 2 args are not used in the following method, hence, can be null.
        mc = (MatchConditions) cond.check(null, null, mc, TacletForTests.services());

        if (mc == null) {
            return term;
        }

        return mc.getInstantiations().getTermInstantiation(result, null, TacletForTests.services());
    }

}
