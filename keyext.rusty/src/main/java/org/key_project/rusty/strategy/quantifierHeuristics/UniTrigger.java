/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics;

import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.logic.op.Quantifier;
import org.key_project.util.ConcurrentLruCache;
import org.key_project.util.collection.*;

import org.jspecify.annotations.NonNull;

/// A trigger consisting of a single term. Matching it against a target term yields the
/// substitutions
/// for the quantified variables; results are cached per target term.
class UniTrigger implements Trigger {
    private final Term trigger;
    /// The universal variables this trigger binds.
    private final ImmutableSet<LogicVariable> universalVars;

    private final TriggersSet triggerSetThisBelongsTo;

    /// How this trigger is matched, see [TriggerKind].
    private final TriggerKind kind;
    private final boolean isElementOfMultitrigger;
    /// Whether this trigger is a theory's fallback for a clause without a covering trigger.
    private final boolean fallback;

    // A TriggersSet is cached per proof (ServiceCaches.triggerSetCache) and thus shared across the
    // parallel-prover workers, so this match-result cache is hit concurrently on the cost path. The
    // exact ConcurrentLruCache is used (not the striped one): the cached substitutions are
    // expensive
    // to recompute, so the better hit rate of exact LRU eviction outweighs the trivial contention
    // on get/put. The get-then-put below stays non-atomic on purpose (the expensive matching
    // run outside the lock); at worst two workers redundantly compute the same (pure) result.
    private final ConcurrentLruCache<Term, ImmutableSet<Substitution>> matchResults =
        new ConcurrentLruCache<>(1000);

    /// The results of the same matching with basic matching allowed. That matching produces
    /// substitutions unification alone does not, so which of the two ran is part of what was
    /// computed and
    /// has to be part of the key: sharing one cache would hand the caller whichever mode happened
    /// to fill the entry first. Only a generalized trigger distinguishes the two modes, see
    /// [#computeSubstitutionsForTerm].
    private final ConcurrentLruCache<Term, ImmutableSet<Substitution>> matchResultsByBasicMatching =
        new ConcurrentLruCache<>(1000);

    UniTrigger(Term trigger, ImmutableSet<LogicVariable> universalVars, TriggerKind kind,
            boolean isElementOfMultitrigger, boolean fallback,
            TriggersSet owningTriggerSet) {
        this.trigger = trigger;
        this.universalVars = universalVars;
        this.kind = kind;
        this.isElementOfMultitrigger = isElementOfMultitrigger;
        this.fallback = fallback;
        this.triggerSetThisBelongsTo = owningTriggerSet;
    }

    @Override
    public ImmutableSet<Substitution> getSubstitutionsFromTerms(ImmutableSet<Term> targetTerms,
            Services services) {
        return getSubstitutionsFromTerms(targetTerms, services, true);
    }

    @Override
    public ImmutableSet<Substitution> getSubstitutionsFromTerms(ImmutableSet<Term> targetTerm,
            Services services, boolean basicMatching) {
        ImmutableSet<Substitution> allsubs = DefaultImmutableSet.nil();
        for (Term aTargetTerm : targetTerm) {
            allsubs = allsubs.union(getSubstitutionsFromTerm(aTargetTerm, services, basicMatching));
        }
        return allsubs;
    }

    private ImmutableSet<Substitution> getSubstitutionsFromTerm(Term target, Services services,
            boolean basicMatching) {
        // A plain trigger is matched basically whenever it is not unified, so the mode leaves its
        // result untouched and both callers share the one cache.
        final ConcurrentLruCache<Term, ImmutableSet<Substitution>> cache =
            basicMatching && kind.isTheoryProvided() ? matchResultsByBasicMatching : matchResults;
        ImmutableSet<Substitution> subs = cache.get(target);
        if (subs == null) {
            subs = computeSubstitutionsForTerm(target, services, basicMatching);
            cache.put(target, subs);
        }
        return subs;
    }

    private ImmutableSet<Substitution> computeSubstitutionsForTerm(Term target,
            Services services, boolean basicMatching) {
        final boolean groundTarget =
            target.freeVars().isEmpty() && !(target.op() instanceof Quantifier);
        // A quantified target is unified whatever the kind: its own variables are unknowns
        // that structural matching cannot handle.
        if (!groundTarget) {
            return Matching.twoSidedMatching(this, target, services);
        }
        // Against a ground target the kind decides. Only structural matching lets a theory
        // solve an array index: unification decides a pair of terms as a whole and offers no
        // point at which a failing index could be solved.
        switch (kind) {
            case PATTERN:
                return Matching.basicMatching(this, target, services);
            case GENERALIZED:
                ImmutableSet<Substitution> subs = Matching.twoSidedMatching(this, target, services);
                if (basicMatching) {
                    final ImmutableSet<Substitution> basicSubs =
                        Matching.basicMatching(this, target, services);
                    if (!basicSubs.isEmpty()) {
                        subs = subs.union(basicSubs);
                    }
                }
                return subs;
            case GENERALIZED_UNIFY:
                return Matching.twoSidedMatching(this, target, services);
            case NEEDS_UNIFY:
            default:
                return DefaultImmutableSet.nil();
        }
    }

    @Override
    public boolean isTheoryProvided() {
        return kind.isTheoryProvided();
    }

    @Override
    public boolean isFallback() {
        return fallback;
    }

    @Override
    public Term getTriggerTerm() {
        return trigger;
    }

    public boolean equals(Object arg0) {
        if (!(arg0 instanceof UniTrigger a)) {
            return false;
        }
        return a.trigger.equals(trigger);
    }

    public int hashCode() {
        return trigger.hashCode();
    }

    public String toString() {
        return String.valueOf(trigger);
    }

    ImmutableSet<LogicVariable> getUniVariables() {
        return universalVars;
    }

    public TriggersSet getTriggerSetThisBelongsTo() {
        return triggerSetThisBelongsTo;
    }

    /// use similar algorithm as basic matching to detect loop
    ///
    /// @param candidate
    /// @param searchTerm
    public static boolean passedLoopTest(Term candidate, Term searchTerm) {
        final ImmutableSet<Substitution> substs =
            BasicMatching.getSyntacticSubstitutions(candidate, searchTerm);

        for (Substitution subst1 : substs) {
            final Substitution subst = subst1;
            if (containsLoop(subst)) {
                return false;
            }
        }
        return true;
    }

    /// Test whether this substitution constains loop. It is mainly used for unitrigger's loop test.
    private static boolean containsLoop(Substitution subst) {
        final var it = subst.getVarMap().keyIterator();
        while (it.hasNext()) {
            if (containsLoop(subst.getVarMap(), it.next())) {
                return true;
            }
        }
        return false;
    }

    /// Code copied from logic.EqualityConstraint
    private static boolean containsLoop(
            ImmutableMap<@NonNull LogicVariable, Term> varMap,
            LogicVariable var) {
        ImmutableList<QuantifiableVariable> body = ImmutableList.nil();
        ImmutableList<Term> fringe = ImmutableList.nil();
        Term checkForCycle = varMap.get(var);

        if (checkForCycle.op() == var) {
            return false;
        }

        while (true) {
            for (var quantifiableVariable : checkForCycle.freeVars()) {
                final var termVar = (LogicVariable) quantifiableVariable;
                if (!body.contains(termVar)) {
                    final var termVarterm = varMap.get(termVar);
                    if (termVarterm != null) {
                        if (termVarterm.freeVars().contains(var)) {
                            return true;
                        }
                        fringe = fringe.prepend(termVarterm);
                    }

                    if (termVar == var) {
                        return true;
                    }

                    body = body.prepend(termVar);
                }
            }

            if (fringe.isEmpty()) {
                return false;
            }

            checkForCycle = fringe.head();
            fringe = fringe.tail();
        }
    }

    boolean isElementOfMultitrigger() {
        return isElementOfMultitrigger;
    }
}
