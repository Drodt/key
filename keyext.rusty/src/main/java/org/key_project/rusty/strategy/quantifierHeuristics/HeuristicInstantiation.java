/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.quantifierHeuristics;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.key_project.logic.Term;
import org.key_project.logic.Visitor;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.termgenerator.TermGenerator;
import org.key_project.rusty.Services;
import org.key_project.rusty.proof.Goal;

public class HeuristicInstantiation implements TermGenerator<Goal> {
    public final static TermGenerator<Goal> INSTANCE = new HeuristicInstantiation();

    private HeuristicInstantiation() {}

    @Override
    public Iterator<Term> generate(RuleApp app, PosInOccurrence pos, Goal goal,
            MutableState mState) {
        assert pos != null : "Feature is only applicable to rules with find";

        final Term qf = pos.sequentFormula().formula();

        // final Instantiation ia =
        // Instantiation.create(qf, goal.sequent(), goal.proof().getServices());
        final QuantifiableVariable var = qf.varsBoundHere(0).last();
        assert var != null;
        var sttc = new StupidTermCollectorRemoveMePlease(var.sort());
        return new HIIterator(sttc.getTerms(goal.sequent()).iterator(), var,
            goal.proof().getServices());
        // return new HIIterator(ia.getSubstitution().iterator(), var, goal.proof().getServices());
    }

    private static class StupidTermCollectorRemoveMePlease implements Visitor<Term> {
        private Set<Term> terms = new HashSet<>();
        private final Sort sort;

        public StupidTermCollectorRemoveMePlease(Sort sort) {
            this.sort = sort;
        }

        @Override
        public void visit(Term visited) {
            if (visited.freeVars().isEmpty() && visited.sort().extendsTrans(sort)) {
                terms.add(visited);
            }
        }

        public Set<Term> getTerms(Sequent seq) {
            for (var t : seq.asList()) {
                t.formula().execPreOrder(this);
            }
            return terms;
        }
    }


    private static class HIIterator implements Iterator<Term> {
        private final Iterator<Term> instances;

        // private final Sort quantifiedVarSort;
        // private final Function quantifiedVarSortCast;

        private Term nextInst = null;
        // private final Services services;

        private HIIterator(Iterator<Term> it, QuantifiableVariable var, Services services) {
            this.instances = it;
            // this.services = services;
            // quantifiedVarSort = var.sort();
            // quantifiedVarSortCast =
            // services.getJavaDLTheory().getCastSymbol(quantifiedVarSort, services);
            findNextInst();
        }

        private void findNextInst() {
            while (nextInst == null && instances.hasNext()) {
                nextInst = instances.next();
                // if (!nextInst.sort().extendsTrans(quantifiedVarSort)) {
                // if (!quantifiedVarSort.extendsTrans(nextInst.sort())) {
                // nextInst = null;
                // continue;
                // }
                // nextInst = services.getTermBuilder().func(quantifiedVarSortCast,
                // nextInst);
                // }
            }
        }

        @Override
        public boolean hasNext() {
            return nextInst != null;
        }

        @Override
        public Term next() {
            final Term res = nextInst;
            nextInst = null;
            findNextInst();
            return res;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
