/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.logic.op.Quantifier;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.rusty.logic.op.UpdateApplication;
import org.key_project.rusty.strategy.quantifierHeuristics.theory.TheoryReasoning;
import org.key_project.util.collection.DefaultImmutableMap;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.Nullable;


public class BasicMatching {
    private BasicMatching() {}

    /// Matches `trigger` against `targetTerm` and its subterms, comparing
    /// the two structures alone.
    ///
    /// @param trigger a uni-trigger
    /// @param targetTerm a ground term
    /// @return all substitutions found
    static ImmutableSet<Substitution> getSyntacticSubstitutions(Term trigger, Term targetTerm) {
        return getSubstitutions(trigger, targetTerm, null);
    }

    /// matching <code>trigger</code> to <code>targetTerm</code> recursively
    ///
    /// @param trigger a uni-trigger
    /// @param targetTerm a gound term
    /// @return all substitution found from this matching
    static ImmutableSet<Substitution> getSubstitutions(Term trigger, Term targetTerm,
            Services services) {
        ImmutableSet<Substitution> allsubs = DefaultImmutableSet.nil();
        if (!targetTerm.freeVars().isEmpty() || targetTerm.op() instanceof Quantifier) {
            return allsubs;
        }
        final Substitution subst = match(trigger, targetTerm, services);
        if (subst != null) {
            allsubs = allsubs.add(subst);
        }
        final var op = targetTerm.op();
        if (!(op instanceof RModality || op instanceof UpdateApplication)) {
            for (int i = 0; i < targetTerm.arity(); i++) {
                allsubs = allsubs.union(getSubstitutions(trigger, targetTerm.sub(i), services));
            }
        }
        return allsubs;
    }

    /// @return all substitution that a given pattern(ex: a term of a uniTrigger) match in the
    /// instance.
    private static Substitution match(Term pattern, Term instance, Services services) {
        final var map =
            matchRec(Bindings.EMPTY, pattern, instance, services, false);
        if (map == null) {
            return null;
        }
        return new Substitution(map.variables(), map.solvedArrayIndex());
    }

    /// match the pattern to instance recursively.
    private static @Nullable Bindings matchRec(
            Bindings bindings, Term pattern, Term instance, Services services, boolean nested) {
        final var patternOp = pattern.op();

        if (patternOp instanceof LogicVariable lv) {
            return bindings.withVariable(lv, instance);
        }

        // A metavariable stands for any term of its sort, so comparing it as a rigid symbol fails
        // against every concrete heap. Bind it like a variable instead, but only when matching for
        // instantiation: trigger selection matches too, and binding there would change which
        // candidates become triggers.
        if (services != null && patternOp instanceof Metavariable metavariable
                && pattern.sort() == instance.sort()) {
            return bindings.withMetavariable(metavariable, instance);
        }

        if (patternOp != instance.op()) {
            // Only below a read that has matched so far. Solving a bare array index against an
            // arbitrary integer establishes nothing until the read around it is known to be the
            // same.
            return nested ? solveByTheory(bindings, pattern, instance, services) : null;
        }
        for (int i = 0; i < pattern.arity(); i++) {
            final Bindings matched =
                matchRec(bindings, pattern.sub(i), instance.sub(i), services, true);
            if (matched == null) {
                // The operators agree at the top and disagree below, which is what an array index
                // written against a different offset looks like: both sides are sums, but their
                // parts do not line up. Solving the two as one equation still succeeds.
                return nested ? solveByTheory(bindings, pattern, instance, services) : null;
            }
            bindings = matched;
        }
        return bindings;
    }

    /// Last resort when the structures disagree: ask the theories to solve the pattern for one of
    /// its variables. An array index written against an offset never matches an absolute one, so
    /// without this a fact about `base + t` cannot be used on a term about `x`.
    ///
    /// A pattern that holds a metavariable is not solved: the solution is built from the
    /// pattern's other parts, and an instance is a term of the proof, which a metavariable is not.
    private static Bindings solveByTheory(Bindings bindings, Term pattern, Term instance,
            Services services) {
        // No services means the caller asked to compare the structures alone.
        if (services == null || !(pattern instanceof Term patternTerm)
                || !(instance instanceof Term instanceTerm) || containsMetavariable(pattern)) {
            return null;
        }
        for (TheoryReasoning support : services.getProfile().getTheorySupports(false)) {
            final ImmutableMap<LogicVariable, Term> solved = support
                    .solveForVariable(patternTerm, instanceTerm, bindings.variables(), services);
            if (solved != null) {
                return bindings.withSolution(solved);
            }
        }
        return null;
    }

    private static boolean containsMetavariable(Term term) {
        if (term.op() instanceof Metavariable) {
            return true;
        }
        for (int i = 0; i < term.arity(); i++) {
            if (containsMetavariable(term.sub(i))) {
                return true;
            }
        }
        return false;
    }

    /// What a match has bound so far. Only `variables` is the result. A metavariable may
    /// occur at more than one position of a trigger and has to stand for the same term at each,
    /// which is what `metavariables` checks; it is dropped when the match ends.
    ///
    /// @param variables the instantiation of the trigger's quantified variables
    /// @param metavariables the terms the trigger's metavariables stand for
    private record Bindings(ImmutableMap<LogicVariable, Term> variables,
            ImmutableMap<Metavariable, Term> metavariables, boolean solvedArrayIndex) {

        static final Bindings EMPTY = new Bindings(DefaultImmutableMap.nilMap(),
            DefaultImmutableMap.nilMap(), false);

        Bindings withVariable(LogicVariable var, Term instance) {
            final Term bound = variables.get(var);
            if (bound == null) {
                return new Bindings(variables.put(var, instance), metavariables, solvedArrayIndex);
            }
            return bound.equals(instance) ? this : null;
        }

        Bindings withMetavariable(Metavariable metavariable, Term instance) {
            final Term bound = metavariables.get(metavariable);
            if (bound == null) {
                return new Bindings(variables, metavariables.put(metavariable, instance),
                    solvedArrayIndex);
            }
            return bound.equals(instance) ? this : null;
        }

        Bindings withSolution(ImmutableMap<LogicVariable, Term> solved) {
            return new Bindings(solved, metavariables, true);
        }
    }
}
