/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.termgenerator;

import java.util.Iterator;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.TermCreationException;
import org.key_project.logic.TerminalSyntaxElement;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.SortedOperator;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.feature.WeakStableCost;
import org.key_project.prover.strategy.costbased.termfeature.TermFeature;
import org.key_project.prover.strategy.costbased.termgenerator.TermGenerator;
import org.key_project.rusty.Services;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;

/// Generates the terms upward a given (usually the find-)position and
/// allows to iterate over those in order to check e.g. whether the
/// position is below an update or quantifier.
///
/// As it only looks upwards, the computed cost is weak stable
@WeakStableCost
public class SuperTermGenerator implements TermGenerator {
    private final TermFeature cond;
    protected final Services services;

    protected SuperTermGenerator(TermFeature cond, Services services) {
        this.cond = cond;
        this.services = services;
    }

    public static TermGenerator upwards(TermFeature cond, final Services services) {
        return new SuperTermGenerator(cond, services);
    }

    public static TermGenerator upwardsWithIndex(TermFeature cond, final Services services) {
        return new SuperTermWithIndexGenerator(cond, services);
    }

    @Override
    public Iterator<Term> generate(RuleApp app, PosInOccurrence pos,
            ProofGoal<?> goal,
            MutableState mState) {
        return new UpwardsIterator(pos, mState);
    }

    protected Term generateOneTerm(Term superterm, int child) {
        return superterm;
    }

    private boolean generateFurther(Term t, MutableState mState) {
        return !(cond.compute(t, mState, services) instanceof TopRuleAppCost);
    }

    @WeakStableCost
    public static class SuperTermWithIndexGenerator extends SuperTermGenerator {
        private final Operator binFunc;

        protected SuperTermWithIndexGenerator(TermFeature cond, Services services) {
            super(cond, services);
            final IntLDT numbers = services.getLDTs().getIntLDT();

            binFunc = new SuperTermWithIndexGenerator.SuperTermGeneratedOp(numbers);
        }

        @Override
        protected Term generateOneTerm(Term superterm, int child) {
            final var index = services.getTermBuilder().zTerm(String.valueOf(child));
            return services.getTermBuilder().tf().createTerm(binFunc,
                superterm, index);
        }

        private static class SuperTermGeneratedOp
                implements SortedOperator, TerminalSyntaxElement, Operator {
            private final Name NAME;
            private final IntLDT numbers;

            public SuperTermGeneratedOp(IntLDT numbers) {
                this.numbers = numbers;
                NAME = new Name("SuperTermGenerated");
            }

            @Override
            public @NonNull Name name() {
                return NAME;
            }

            @Override
            public int arity() {
                return 2;
            }

            @Override
            public @NonNull Sort sort(Sort[] sorts) {
                return RustyDLTheory.ANY;
            }

            @Override
            public @NonNull Sort sort() {
                return RustyDLTheory.ANY;
            }

            @Override
            public @NonNull Sort argSort(int i) {
                return RustyDLTheory.ANY;
            }

            @Override
            public ImmutableList<Sort> argSorts() {
                return null;
            }

            @Override
            public boolean bindVarsAt(int n) {
                return false;
            }

            @Override
            public @NonNull Modifier modifier() {
                return Modifier.RIGID;
            }

            @Override
            public boolean isRigid() {
                return true;
            }

            @Override
            public void validTopLevelException(Term term)
                    throws TermCreationException {
                if (!(term.arity() == 2 && term.sub(1).sort()
                        .extendsTrans(numbers.getNumberSymbol().sort()))) {
                    throw new TermCreationException(this, term);
                }
            }
        }
    }

    class UpwardsIterator implements Iterator<Term> {
        private PosInOccurrence currentPos;
        private final MutableState mState;

        private UpwardsIterator(PosInOccurrence startPos, MutableState mState) {
            this.currentPos = startPos;
            this.mState = mState;
        }

        @Override
        public boolean hasNext() {
            return currentPos != null && !currentPos.isTopLevel();
        }

        @Override
        public Term next() {
            final int child = currentPos.getIndex();
            currentPos = currentPos.up();
            final Term res = generateOneTerm(currentPos.subTerm(), child);
            if (!generateFurther(res, mState)) {
                currentPos = null;
            }
            return res;
        }

        /// throw an unsupported operation exception as generators do not remove
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
