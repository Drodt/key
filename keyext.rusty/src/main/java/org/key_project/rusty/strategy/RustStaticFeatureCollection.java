/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy;

import org.key_project.logic.TermOrdering;
import org.key_project.logic.op.Operator;
import org.key_project.prover.proof.rulefilter.SetRuleFilter;
import org.key_project.prover.strategy.StaticFeatureCollection;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.feature.*;
import org.key_project.prover.strategy.costbased.feature.TriggerVariableInstantiationProjection;
import org.key_project.prover.strategy.costbased.termProjection.*;
import org.key_project.prover.strategy.costbased.termfeature.*;
import org.key_project.prover.strategy.costbased.termgenerator.SequentFormulasGenerator;
import org.key_project.prover.strategy.costbased.termgenerator.SubtermGenerator;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.logic.LexPathOrdering;
import org.key_project.rusty.logic.equality.RenamingTermProperty;
import org.key_project.rusty.rule.UseOperationContractRule;
import org.key_project.rusty.strategy.feature.*;
import org.key_project.rusty.strategy.feature.TriggerVarInstantiatedFeature;
import org.key_project.rusty.strategy.quantifierHeuristics.LiteralsSmallerThanFeature;
import org.key_project.rusty.strategy.termProjection.*;

/// Collection of strategy features that can be accessed statically. This class is essentially a
/// collection of constructors for various features.
///
/// @author Kai Wallisch <kai.wallisch@ira.uka.de>
public class RustStaticFeatureCollection extends StaticFeatureCollection {
    public static TermOrdering createTermOrdering() {
        return new LexPathOrdering();
    }

    protected static ProjectionToTerm opTerm(Operator op,
            ProjectionToTerm subTerm0,
            ProjectionToTerm subTerm1) {
        // noinspection unchecked
        return opTerm(op, new ProjectionToTerm[] { subTerm0, subTerm1 });
    }

    protected static ProjectionToTerm opTerm(Operator op,
            ProjectionToTerm[] subTerms) {
        return TermConstructionProjection.create(op, subTerms);
    }

    private static RuleAppCost infty() {
        return TopRuleAppCost.INSTANCE;
    }

    protected static Feature sequentContainsNoPrograms() {
        return not(SeqContainsExecutableCodeFeature.PROGRAMS);
    }

    protected static Feature functionSpecFeature(Feature cost) {
        SetRuleFilter filter = new SetRuleFilter();
        filter.addRuleToSet(UseOperationContractRule.INSTANCE);
        return ConditionalFeature.createConditional(filter, cost);
    }

    protected static Feature contains(ProjectionToTerm bigTerm,
            ProjectionToTerm searchedTerm) {
        final TermBuffer buf = new TermBuffer();
        return let(buf, searchedTerm, applyTF(bigTerm, not(rec(any(), not(eq(buf))))));
    }

    protected static TermFeature eq(TermBuffer t) {
        return EqTermFeature.create(t, RenamingTermProperty.RENAMING_TERM_PROPERTY);
    }

    protected static Feature eq(ProjectionToTerm t1, ProjectionToTerm t2) {
        final TermBuffer buf = new TermBuffer();
        return let(buf, t1, applyTF(t2, eq(buf)));
    }

    protected static Feature isTriggerVariableInstantiated() {
        return TriggerVarInstantiatedFeature.INSTANCE;
    }

    /// Create a projection of taclet applications to the instantiation of the trigger variable of a
    /// taclet. If the trigger variable is not instantiated for a particular taclet app, an error
    /// will be raised
    ///
    /// @return projection of taclet applications
    protected static ProjectionToTerm instOfTriggerVariable() {
        return new TriggerVariableInstantiationProjection();
    }

    protected static Feature termSmallerThan(String smaller, String bigger) {
        return TermSmallerThanFeature.create(instOf(smaller), instOf(bigger), createTermOrdering());
    }

    protected static Feature directlyBelowSymbolAtIndex(Operator symbol, int index) {
        return directlyBelowSymbolAtIndex(op(symbol), index);
    }

    protected static Feature directlyBelowSymbolAtIndex(TermFeature symbolTF, int index) {
        final var oneUp = FocusProjection.create(1);
        if (index == -1) {
            return add(not(TopLevelFindFeature.ANTEC_OR_SUCC), applyTF(oneUp, symbolTF));
        }
        return add(not(TopLevelFindFeature.ANTEC_OR_SUCC),
            ifZero(applyTF(oneUp, symbolTF), eq(sub(oneUp, index), FocusProjection.create()),
                inftyConst()));
    }

    protected static Feature countOccurrences(ProjectionToTerm cutFormula) {
        final TermBuffer sf = new TermBuffer();
        final TermBuffer sub = new TermBuffer();

        return sum(sf, SequentFormulasGenerator.sequent(),
            sum(sub, SubtermGenerator.leftTraverse(sf, any()),
                // instead of any a condition which stops traversal when
                // depth(cutF) > depth(sub) would be better
                ifZero(applyTF(cutFormula, eq(sub)), longConst(1), longConst(0))));
    }

    protected static Feature literalsSmallerThan(String smaller, String bigger,
            IntLDT numbers) {
        return LiteralsSmallerThanFeature.create(instOf(smaller), instOf(bigger),
            createTermOrdering(), numbers);
    }

    protected static Feature monSmallerThan(String smaller, String bigger,
            IntLDT numbers) {
        return MonomialsSmallerThanFeature.create(instOf(smaller), instOf(bigger),
            createTermOrdering(), numbers);
    }

    protected static Feature atomSmallerThan(String smaller, String bigger,
            IntLDT numbers) {
        return AtomsSmallerThanFeature.create(instOf(smaller), instOf(bigger), createTermOrdering(),
            numbers);
    }
}
