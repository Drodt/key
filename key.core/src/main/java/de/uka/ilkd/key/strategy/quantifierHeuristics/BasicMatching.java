/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.strategy.quantifierHeuristics;

import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.op.Quantifier;
import de.uka.ilkd.key.logic.op.UpdateApplication;

import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.util.collection.DefaultImmutableMap;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

class BasicMatching {

    private BasicMatching() {}

    /**
     * matching <code>trigger</code> to <code>targetTerm</code> recursively
     *
     * @param trigger a uni-trigger
     * @param targetTerm a gound term
     * @return all substitution found from this matching
     */
    static ImmutableSet<Substitution> getSubstitutions(Term trigger, Term targetTerm) {
        ImmutableSet<Substitution> allsubs = DefaultImmutableSet.nil();
        if (targetTerm.freeVars().size() > 0 || targetTerm.op() instanceof Quantifier) {
            return allsubs;
        }
        final Substitution subst = match(trigger, targetTerm);
        if (subst != null) {
            allsubs = allsubs.add(subst);
        }
        final var op = targetTerm.op();
        if (!(op instanceof JModality || op instanceof UpdateApplication)) {
            for (int i = 0; i < targetTerm.arity(); i++) {
                allsubs = allsubs.union(getSubstitutions(trigger, targetTerm.sub(i)));
            }
        }
        return allsubs;
    }

    /**
     * @param pattern
     * @param instance
     * @return all substitution that a given pattern(ex: a term of a uniTrigger) match in the
     *         instance.
     */
    private static Substitution match(Term pattern, Term instance) {
        final ImmutableMap<QuantifiableVariable, Term> map =
            matchRec(DefaultImmutableMap.nilMap(), pattern, instance);
        if (map == null) {
            return null;
        }
        return new Substitution(map);
    }

    /**
     * match the pattern to instance recursively.
     */
    private static ImmutableMap<QuantifiableVariable, Term> matchRec(
            ImmutableMap<QuantifiableVariable, Term> varMap, Term pattern, Term instance) {
        final var patternOp = pattern.op();

        if (patternOp instanceof QuantifiableVariable) {
            return mapVarWithCheck(varMap, (QuantifiableVariable) patternOp, instance);
        }

        if (patternOp != instance.op()) {
            return null;
        }
        for (int i = 0; i < pattern.arity(); i++) {
            varMap = matchRec(varMap, pattern.sub(i), instance.sub(i));
            if (varMap == null) {
                return null;
            }
        }
        return varMap;
    }

    /**
     * match a variable to a instance.
     *
     * @return true if it is a new vaiable or the instance it matched is the same as that it matched
     *         before.
     */
    private static ImmutableMap<QuantifiableVariable, Term> mapVarWithCheck(
            ImmutableMap<QuantifiableVariable, Term> varMap, QuantifiableVariable var,
            Term instance) {
        final Term oldTerm = varMap.get(var);
        if (oldTerm == null) {
            return varMap.put(var, instance);
        }

        if (oldTerm.equals(instance)) {
            return varMap;
        }
        return null;
    }


}
