/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy;

import org.key_project.logic.Name;
import org.key_project.logic.PosInTerm;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.proof.rulefilter.SetRuleFilter;
import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.feature.*;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.prover.strategy.costbased.termfeature.*;
import org.key_project.prover.strategy.costbased.termgenerator.SequentFormulasGenerator;
import org.key_project.prover.strategy.costbased.termgenerator.SubtermGenerator;
import org.key_project.prover.strategy.costbased.termgenerator.TermGenerator;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.rule.UseOperationContractRule;
import org.key_project.rusty.strategy.feature.*;
import org.key_project.rusty.strategy.quantifierHeuristics.LiteralsSmallerThanFeature;
import org.key_project.rusty.strategy.termProjection.*;
import org.key_project.rusty.strategy.termfeature.EqTermFeature;

/// Collection of strategy features that can be accessed statically. This class is essentially a
/// collection of constructors for various features.
///
/// @author Kai Wallisch <kai.wallisch@ira.uka.de>
public class StaticFeatureCollection {
    protected static Feature longConst(long a) {
        return ConstFeature.createConst(cost(a));
    }

    protected static RuleAppCost cost(long p) {
        return NumberRuleAppCost.create(p);
    }

    protected static TermFeature extendsTrans(Sort s) {
        return SortExtendsTransTermFeature.create(s);
    }

    protected static TermFeature op(Operator op) {
        return OperatorTF.create(op);
    }

    protected static TermFeature rec(TermFeature cond, TermFeature summand) {
        return RecSubTermFeature.create(cond, summand);
    }

    protected static TermFeature sub(TermFeature sub0) {
        return SubTermFeature.create(new TermFeature[] { sub0 });
    }

    protected static TermFeature sub(TermFeature sub0, TermFeature sub1) {
        return SubTermFeature.create(new TermFeature[] { sub0, sub1 });
    }

    protected static ProjectionToTerm<Goal> sub(ProjectionToTerm<Goal> t, int index) {
        return SubtermProjection.create(t, PosInTerm.getTopLevel().down(index));
    }

    protected static ProjectionToTerm<Goal> subAt(ProjectionToTerm<Goal> t, PosInTerm pit) {
        return SubtermProjection.create(t, pit);
    }

    protected static TermFeature opSub(Operator op, TermFeature sub0) {
        return add(op(op), sub(sub0));
    }

    protected static TermFeature opSub(Operator op, TermFeature sub0, TermFeature sub1) {
        return add(op(op), sub(sub0, sub1));
    }

    protected static ProjectionToTerm<Goal> opTerm(Operator op,
            ProjectionToTerm<Goal> subTerm0,
            ProjectionToTerm<Goal> subTerm1) {
        // noinspection unchecked
        return opTerm(op, new ProjectionToTerm[] { subTerm0, subTerm1 });
    }

    protected static ProjectionToTerm<Goal> opTerm(Operator op,
            ProjectionToTerm<Goal>[] subTerms) {
        return TermConstructionProjection.create(op, subTerms);
    }

    @SafeVarargs
    protected static Feature add(Feature... features) {
        return SumFeature.createSum(features);
    }

    protected static TermFeature add(TermFeature a, TermFeature b) {
        return BinarySumTermFeature.createSum(a, b);
    }

    protected static TermFeature add(TermFeature a, TermFeature b, TermFeature c) {
        // could be done more efficiently
        return add(a, add(b, c));
    }

    protected static Feature inftyConst() {
        return ConstFeature.createConst(infty());
    }

    private static RuleAppCost infty() {
        return TopRuleAppCost.INSTANCE;
    }

    protected static Feature ifZero(Feature cond, Feature thenFeature) {
        return ShannonFeature.createConditionalBinary(cond, thenFeature);
    }

    protected static Feature ifZero(Feature cond, Feature thenFeature,
            Feature elseFeature) {
        return ShannonFeature.createConditionalBinary(cond, thenFeature, elseFeature);
    }

    protected static TermFeature ifZero(TermFeature cond, TermFeature thenFeature) {
        return ShannonTermFeature.createConditionalBinary(cond, thenFeature);
    }

    protected static TermFeature ifZero(TermFeature cond, TermFeature thenFeature,
            TermFeature elseFeature) {
        return ShannonTermFeature.createConditionalBinary(cond, thenFeature, elseFeature);
    }

    protected static TermFeature not(TermFeature f) {
        return ifZero(f, ConstTermFeature.createConst(TopRuleAppCost.INSTANCE), longTermConst(0));
    }

    protected static Feature not(Feature f) {
        return ifZero(f, inftyConst(), longConst(0));
    }

    protected static TermFeature or(TermFeature a, TermFeature b) {
        return ifZero(a, longTermConst(0), b);
    }

    protected static TermFeature or(TermFeature a, TermFeature b, TermFeature c) {
        return or(a, or(b, c));
    }

    protected static TermFeature or(TermFeature... features) {
        TermFeature orFeature = inftyTermConst();
        for (var f : features) {
            orFeature = or(orFeature, f);
        }
        return orFeature;
    }

    protected static Feature or(Feature a, Feature b) {
        return ifZero(a, longConst(0), b);
    }

    protected static Feature or(Feature a, Feature b, Feature c) {
        return or(a, or(b, c));
    }

    protected static TermFeature inftyTermConst() {
        return ConstTermFeature.createConst(infty());
    }

    protected static TermFeature any() {
        return longTermConst(0);
    }

    protected static TermFeature longTermConst(long a) {
        return ConstTermFeature.createConst(cost(a));
    }

    protected static TermFeature constantTermFeature() {
        return TermPredicateTermFeature
                .create(term -> term.op() instanceof Function && term.arity() == 0);
    }

    protected static Feature sequentContainsNoPrograms() {
        return not(SeqContainsExecutableCodeFeature.PROGRAMS);
    }

    /// Create a projection of taclet applications to the instantiation of the schema variables
    /// <code>schemaVar</code>. If <code>schemaVar</code> is not instantiated for a particular
    /// taclet
    /// app, an error will be raised
    ///
    /// @param schemaVar schema variable
    /// @return projection of taclet applications
    protected static ProjectionToTerm<Goal> instOf(String schemaVar) {
        return SVInstantiationProjection.create(new Name(schemaVar), true);
    }

    /// Evaluate the term feature <code>tf</code> for the term described by the projection
    /// <code>term</code>. If <code>term</code> is undefined for a particular rule app, an exception
    /// is raised
    ///
    /// @param term term describing the projection
    /// @param tf term feature
    /// @return feature
    protected static Feature applyTF(ProjectionToTerm<Goal> term, TermFeature tf) {
        return ApplyTFFeature.create(term, tf);
    }

    /// Invoke the term feature <code>tf</code> on the term that instantiation of
    /// <code>schemaVar</code>. This is the strict/safe version that raises an error of
    /// <code>schemaVar</code> is not instantiated for a particular taclet app
    ///
    /// @param schemaVar schema variable
    /// @param tf term feature
    /// @return feature
    protected static Feature applyTF(String schemaVar, TermFeature tf) {
        return applyTF(instOf(schemaVar), tf);
    }

    protected static Feature functionSpecFeature(Feature cost) {
        SetRuleFilter filter = new SetRuleFilter();
        filter.addRuleToSet(UseOperationContractRule.INSTANCE);
        return ConditionalFeature.createConditional(filter, cost);
    }

    protected static Feature sum(TermBuffer x, TermGenerator<Goal> gen, Feature body) {
        return ComprehendedSumFeature.create(x, gen, body);
    }

    protected static Feature contains(ProjectionToTerm<Goal> bigTerm,
            ProjectionToTerm<Goal> searchedTerm) {
        final TermBuffer buf = new TermBuffer();
        return let(buf, searchedTerm, applyTF(bigTerm, not(rec(any(), not(eq(buf))))));
    }

    protected static Feature let(TermBuffer x, ProjectionToTerm<Goal> value,
            Feature body) {
        return LetFeature.create(x, value, body);
    }

    protected static TermFeature eq(TermBuffer t) {
        return EqTermFeature.create(t);
    }

    protected static Feature eq(ProjectionToTerm<Goal> t1, ProjectionToTerm<Goal> t2) {
        final TermBuffer buf = new TermBuffer();
        return let(buf, t1, applyTF(t2, eq(buf)));
    }

    protected static Feature isInstantiated(String schemaVar) {
        return InstantiatedSVFeature.create(new Name(schemaVar));
    }

    protected static Feature isTriggerVariableInstantiated() {
        return TriggerVarInstantiatedFeature.INSTANCE;
    }

    /// Create a projection of taclet applications to the instantiation of the trigger variable of a
    /// taclet. If the trigger variable is not instantiated for a particular taclet app, an error
    /// will be raised
    ///
    /// @return projection of taclet applications
    protected static ProjectionToTerm<Goal> instOfTriggerVariable() {
        return new TriggerVariableInstantiationProjection();
    }

    protected static Feature termSmallerThan(String smaller, String bigger) {
        return TermSmallerThanFeature.create(instOf(smaller), instOf(bigger));
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
            ifZero(applyTF(oneUp, symbolTF), eq(sub(oneUp, index), FocusProjection.INSTANCE),
                inftyConst()));
    }

    protected static TermFeature selectSkolemConstantTermFeature() {
        // TODO (DD): This feature uses term labels. What to do?
        return longTermConst(0);
    }

    protected static Feature countOccurrences(ProjectionToTerm<Goal> cutFormula) {
        final TermBuffer sf = new TermBuffer();
        final TermBuffer sub = new TermBuffer();

        return sum(sf, SequentFormulasGenerator.sequent(),
            sum(sub, SubtermGenerator.leftTraverse(sf, any()),
                // instead of any a condition which stops traversal when
                // depth(cutF) > depth(sub) would be better
                ifZero(applyTF(cutFormula, eq(sub)), longConst(1), longConst(0))));
    }

    /// Invoke the term feature <code>tf</code> on the term that instantiation of
    /// <code>schemaVar</code>. This is the non-strict/unsafe version that simply returns zero if
    /// <code>schemaVar</code> is not instantiated for a particular taclet app
    ///
    /// @param schemaVar schema variable
    /// @param tf term feature
    /// @return feature
    protected static Feature applyTFNonStrict(String schemaVar, TermFeature tf) {
        return applyTFNonStrict(instOfNonStrict(schemaVar), tf);
    }

    /// Evaluate the term feature <code>tf</code> for the term described by the projection
    /// <code>term</code>. If <code>term</code> is undefined for a particular rule app, zero is
    /// returned
    ///
    /// @param term term describing the projection
    /// @param tf term feature
    /// @return feature
    protected static Feature applyTFNonStrict(ProjectionToTerm<Goal> term, TermFeature tf) {
        return ApplyTFFeature.createNonStrict(term, tf, NumberRuleAppCost.getZeroCost());
    }

    /// Create a projection of taclet applications to the instantiation of the schema variables
    /// <code>schemaVar</code>. The projection will be partial and undefined for those taclet
    /// applications that do not instantiate <code>schemaVar</code>
    ///
    /// @param schemaVar schema variable
    /// @return projection of taclet applications
    protected static ProjectionToTerm<Goal> instOfNonStrict(String schemaVar) {
        return SVInstantiationProjection.create(new Name(schemaVar), false);
    }

    protected static Feature literalsSmallerThan(String smaller, String bigger,
            IntLDT numbers) {
        return LiteralsSmallerThanFeature.create(instOf(smaller), instOf(bigger), numbers);
    }

    protected static Feature monSmallerThan(String smaller, String bigger,
            IntLDT numbers) {
        return MonomialsSmallerThanFeature.create(instOf(smaller), instOf(bigger), numbers);
    }

    protected static Feature atomSmallerThan(String smaller, String bigger,
            IntLDT numbers) {
        return AtomsSmallerThanFeature.create(instOf(smaller), instOf(bigger), numbers);
    }
}
