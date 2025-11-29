/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.quantifierHeuristics;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.BinaryFeature;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.rusty.logic.op.Junctor;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.logic.op.Quantifier;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

public class SplittableQuantifiedFormulaFeature extends BinaryFeature {
    private SplittableQuantifiedFormulaFeature() {}

    public static final Feature INSTANCE = new SplittableQuantifiedFormulaFeature();

    @Override
    protected <Goal extends ProofGoal<@NonNull Goal>> boolean filter(RuleApp app,
            PosInOccurrence pos, Goal goal, MutableState mState) {
        assert pos != null : "Feature is only applicable to rules with find";

        final Analyser analyser = new Analyser();
        if (!analyser.analyse(pos.sequentFormula().formula())) {
            return false;
        }

        if (analyser.binOp == Junctor.AND) {
            return TriggerUtils.intersect((ImmutableSet<LogicVariable>) analyser.left.freeVars(),
                (ImmutableSet<LogicVariable>) analyser.right.freeVars(), analyser.existentialVars)
                    .isEmpty();
        } else if (analyser.binOp == Junctor.OR) {
            return TriggerUtils
                    .intersect((ImmutableSet<LogicVariable>) analyser.left.freeVars(),
                        (ImmutableSet<LogicVariable>) analyser.right.freeVars())
                    .union(analyser.existentialVars).size() == analyser.existentialVars.size();
        }

        return false;
    }

    private static class Analyser {
        public ImmutableSet<LogicVariable> existentialVars =
            DefaultImmutableSet.nil();
        public Operator binOp;
        public Term left, right;
        private int depth = 0;

        public boolean analyse(Term formula) {
            final Operator op = formula.op();

            if (op == Quantifier.ALL) {
                boolean result = analyse(formula.sub(0));
                depth++;
                return result;
            }

            if (op == Quantifier.EX) {
                boolean result = analyse(formula.sub(0));
                depth++;
                var sort = formula.varsBoundHere(0).last().sort();
                existentialVars = existentialVars.add(LogicVariable.create(depth, sort));
                return result;
            }

            if (op == Junctor.AND || op == Junctor.OR) {
                binOp = op;
                left = formula.sub(0);
                right = formula.sub(1);
                return true;
            }

            return false;
        }
    }
}
