/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.LogicVariableTable;
import org.key_project.rusty.logic.op.BoundVariable;
import org.key_project.rusty.logic.op.sv.VariableSV;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.util.collection.ImmutableList;

/// Opens a binding scope for a term with bound variables (a quantifier, a substitution, ...): it
/// extends the renaming table and matches the pattern's bound variables pairwise against the source
/// term's own bound variables: a concrete `LogicVariable` in the pattern by consistent
/// renaming (same sort, same abstract name), a `VariableSV` by instantiating it with the
/// source variable. The scope is closed again after the term has been matched
/// (`MatchConditions.shrinkRenameTable()`, called through the `BinderMatcher`).
public class BindVariablesInstruction implements MatchInstruction {
    private final VariableBinderSubinstruction[] boundVarBinders;

    public BindVariablesInstruction(ImmutableList<QuantifiableVariable> boundVars) {
        boundVarBinders = new VariableBinderSubinstruction[boundVars.size()];
        int i = 0;
        for (QuantifiableVariable boundVar : boundVars) {
            if (boundVar instanceof BoundVariable lv) {
                boundVarBinders[i] = new LogicVariableBinder(lv);
            } else {
                boundVarBinders[i] = new VariableSVBinder((VariableSV) boundVar);
            }
            i++;
        }
    }

    private interface VariableBinderSubinstruction {
        MatchConditions match(BoundVariable instantiationCandidate,
                MatchConditions matchCond, LogicServices services);
    }

    private record LogicVariableBinder(BoundVariable templateVar)
            implements VariableBinderSubinstruction {
        /// Two logic variables match if they are the same variable, or if the consistent renaming
        /// maps both to the same abstract name and their sorts are equal.
        public MatchConditions match(BoundVariable instantiationCandidate,
                MatchConditions matchCond, LogicServices services) {
            final LogicVariableTable rt = matchCond.logicVariableTable();
            if (!rt.containsLocally(templateVar) && !rt.containsLocally(instantiationCandidate)) {
                matchCond = matchCond.extendLogicVarTable(templateVar);
            }

            if (templateVar != instantiationCandidate) {
                if (instantiationCandidate.sort() != templateVar.sort()) {
                    matchCond = null;
                }
            }
            return matchCond;
        }
    }

    private static class VariableSVBinder extends MatchSchemaVariableInstruction
            implements VariableBinderSubinstruction {
        public VariableSVBinder(VariableSV templateVar) {
            super(templateVar);
        }

        @Override
        public MatchConditions match(BoundVariable instantiationCandidate,
                MatchConditions matchCond, LogicServices p_services) {
            final Services services = (Services) p_services;
            final Object foundMapping = matchCond.getInstantiations().getInstantiation(op);
            matchCond = matchCond.extendLogicVarTable(instantiationCandidate);
            if (foundMapping == null) {
                final Term substTerm = services.getTermBuilder().var(instantiationCandidate);
                return addInstantiation(substTerm, matchCond, services);
            } else if (((Term) foundMapping).op() != instantiationCandidate) {
                return null;
            } else {
                return matchCond;
            }
        }

        @Override
        protected MatchResultInfo match(Term instantiationCandidate,
                MatchResultInfo matchConditions,
                LogicServices services) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo matchResult,
            LogicServices services) {
        MatchConditions matchConditions = (MatchConditions) matchResult;
        final var variablesToMatchAndBind = ((Term) actualElement).boundVars();
        if (variablesToMatchAndBind.size() == boundVarBinders.length) {
            for (int i = 0; i < boundVarBinders.length && matchConditions != null; i++) {
                // concrete variables must be logic variables
                final var qVar = (BoundVariable) variablesToMatchAndBind.get(i);
                matchConditions = boundVarBinders[i].match(qVar, matchConditions, services);
            }
            return matchConditions;
        }
        return null;
    }
}
