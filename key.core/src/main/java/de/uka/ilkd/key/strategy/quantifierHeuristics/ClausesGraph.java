/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.strategy.quantifierHeuristics;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import de.uka.ilkd.key.java.ServiceCaches;
import de.uka.ilkd.key.logic.op.Junctor;
import de.uka.ilkd.key.logic.op.Quantifier;

import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

/**
 * This class describes the relation between different clauses in a CNF. If two clauses have the
 * same existential quantifiable variable, we say they are connected. And this property is
 * transitive.
 */
public class ClausesGraph {
    private final ImmutableSet<QuantifiableVariable> exVars;

    /**
     * Map from <code>Term</code> to <code>ImmutableSet<Term></code>
     */
    private final Map<Term, ImmutableSet<Term>> connections =
        new LinkedHashMap<>();

    private final ImmutableSet<Term> clauses;

    static ClausesGraph create(Term quantifiedFormula, ServiceCaches caches) {
        final Map<Term, ClausesGraph> graphCache = caches.getGraphCache();
        ClausesGraph graph;
        synchronized (graphCache) {
            graph = graphCache.get(quantifiedFormula);
        }
        if (graph == null) {
            graph = new ClausesGraph(quantifiedFormula);
            synchronized (graphCache) {
                graphCache.put(quantifiedFormula, graph);
            }
        }
        return graph;
    }

    private ClausesGraph(Term quantifiedFormula) {
        exVars = existentialVars(quantifiedFormula);
        clauses = computeClauses(TriggerUtils.discardQuantifiers(quantifiedFormula));
        buildInitialGraph();
        buildFixedPoint();
    }

    private void buildFixedPoint() {
        boolean changed;
        do {
            changed = false;

            for (final Term clause : clauses) {
                final ImmutableSet<Term> oldConnections = getConnections(clause);
                final ImmutableSet<Term> newConnections = getTransitiveConnections(oldConnections);

                if (newConnections.size() > oldConnections.size()) {
                    changed = true;
                    connections.put(clause, newConnections);
                }
            }

        } while (changed);
    }

    private ImmutableSet<Term> getTransitiveConnections(ImmutableSet<Term> formulas) {
        for (Term formula : formulas) {
            formulas = formulas.union(getConnections(formula));
        }
        return formulas;
    }

    /**
     *
     * @param formula0
     * @param formula1
     * @return ture if clause of formula0 and clause of formula1 are connected.
     */
    boolean connected(Term formula0, Term formula1) {
        final ImmutableSet<Term> subFormulas1 = computeClauses(formula1);
        for (final Term term : computeClauses(formula0)) {
            if (!intersect(getConnections(term), subFormulas1).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    boolean isFullGraph() {
        final Iterator<Term> it = clauses.iterator();
        if (it.hasNext()) {
            return getConnections(it.next()).size() >= clauses.size();
        }
        return true;
    }


    /**
     * @param formula
     * @return set of terms that connect to the formula.
     */
    private ImmutableSet<Term> getConnections(Term formula) {
        return connections.get(formula);
    }

    /**
     * initiate connection map.
     *
     */
    private void buildInitialGraph() {
        for (final Term clause : clauses) {
            connections.put(clause, directConnections(clause));
        }
    }

    /**
     *
     * @param formula
     * @return set of term that connect to formula.
     */
    private ImmutableSet<Term> directConnections(Term formula) {
        ImmutableSet<Term> res = DefaultImmutableSet.nil();
        for (final Term clause : clauses) {
            if (directlyConnected(clause, formula)) {
                res = res.add(clause);
            }
        }
        return res;
    }

    /**
     *
     * @param set
     * @return ture if set contains one or more exists varaible that are also in exVars
     */
    private boolean containsExistentialVariables(
            ImmutableSet<? extends QuantifiableVariable> set) {
        return !TriggerUtils.intersect(set, exVars).isEmpty();
    }

    /**
     * @param formula0
     * @param formula1
     * @return true if formula0 and formula1 have one or more exists varaible that are the same.
     */
    private boolean directlyConnected(Term formula0, Term formula1) {
        return containsExistentialVariables(
            TriggerUtils.intersect(formula0.freeVars(), formula1.freeVars()));
    }

    /**
     * @param formula
     * @return retrun set of terms of all clauses under the formula
     */

    private ImmutableSet<Term> computeClauses(Term formula) {
        final var op = formula.op();
        if (op == Junctor.NOT) {
            return computeClauses(formula.sub(0));
        } else if (op == Junctor.AND) {
            return computeClauses(formula.sub(0)).union(computeClauses(formula.sub(1)));
        } else {
            return DefaultImmutableSet.<Term>nil().add(formula);
        }
    }

    /**
     * return the exists variables bound in the top level of a given cnf formula.
     */
    private ImmutableSet<QuantifiableVariable> existentialVars(Term formula) {
        final var op = formula.op();
        if (op == Quantifier.ALL) {
            return existentialVars(formula.sub(0));
        }
        if (op == Quantifier.EX) {
            return existentialVars(formula.sub(0))
                    .add(formula.varsBoundHere(0).last());
        }
        return DefaultImmutableSet.nil();
    }

    /**
     *
     * @param set0
     * @param set1
     * @return a set of terms which are belonged to both set0 and set1.
     */
    private ImmutableSet<Term> intersect(ImmutableSet<Term> set0, ImmutableSet<Term> set1) {
        ImmutableSet<Term> res = DefaultImmutableSet.nil();
        if (set0 == null || set1 == null) {
            return res;
        }
        for (Term aSet0 : set0) {
            final Term el = aSet0;
            if (set1.contains(el)) {
                res = res.add(el);
            }
        }
        return res;
    }

}
