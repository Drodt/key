/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import java.util.HashSet;
import java.util.Set;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.*;
import org.key_project.rusty.logic.op.sv.UpdateSV;
import org.key_project.rusty.proof.TermProgramVariableCollector;
import org.key_project.rusty.rule.inst.SVInstantiations;

import org.jspecify.annotations.Nullable;

public class DropEffectlessMutatingCondition implements VariableCondition {
    private final UpdateSV u;
    private final SchemaVariable x;
    private final SchemaVariable result;

    public DropEffectlessMutatingCondition(UpdateSV u,
            SchemaVariable x,
            SchemaVariable x2) {
        this.u = u;
        this.x = x;
        this.result = x2;
    }

    private static @Nullable Term dropMutatingHelper(Term mref,
            Set<ProgramVariable> relevantVars, Services services) {
        if (mref.op() instanceof ProgramVariable pv) {
            if (relevantVars.contains(pv)) {
                return null;
            } else {
                return null;
            }
        } else if (mref.op() instanceof ParametricFunctionInstance pfi
                && pfi.getBase() == services.getLDTs().getmRefLDT().getArrPlace()) {
            return dropMutatingHelper(mref.sub(0), relevantVars, services);
        }
        return null;
    }

    private static Term dropEffectlessMutatingHelper(Term update,
            Set<ProgramVariable> relevantVars, Services services) {
        if (update.op() instanceof ElementaryUpdate eu) {
            if (eu.lhs() instanceof ProgramVariable pv1 && relevantVars.contains(pv1)
                    && update.sub(0).op() instanceof ProgramVariable pv)
                relevantVars.add(pv);
            return null;
        } else if (update.op() instanceof MutatingUpdate) {
            if (relevantVars.isEmpty())
                return services.getTermBuilder().skip();
            return dropMutatingHelper(update.sub(0), relevantVars, services);
        } else if (update.op() == UpdateJunctor.PARALLEL_UPDATE) {
            Term sub0 = update.sub(0);
            Term sub1 = update.sub(1);
            Term newSub1 = dropEffectlessMutatingHelper(sub1, relevantVars, services);
            var overwritten = new HashSet<ProgramVariable>();
            collectOverwrittenPVs(sub1, overwritten);
            relevantVars.removeAll(overwritten);
            Term newSub0 = dropEffectlessMutatingHelper(sub0, relevantVars, services);
            if (newSub0 == null && newSub1 == null) {
                return null;
            } else {
                newSub0 = newSub0 == null ? sub0 : newSub0;
                newSub1 = newSub1 == null ? sub1 : newSub1;
                return services.getTermBuilder().parallel(newSub0, newSub1);
            }
        } else {
            return null;
        }
    }

    private static Term dropEffectlessMutating(Term update, Term target, Services services) {
        TermProgramVariableCollector collector = new TermProgramVariableCollector(services);
        target.execPostOrder(collector);
        Set<ProgramVariable> varsInTarget = collector.result();
        Term simplifiedUpdate = dropEffectlessMutatingHelper(update, varsInTarget, services);
        return simplifiedUpdate == null ? null
                : services.getTermBuilder().apply(simplifiedUpdate, target);
    }

    private static void collectOverwrittenPVs(Term update, Set<ProgramVariable> set) {
        if (update.op() instanceof ElementaryUpdate eu) {
            if (eu.lhs() instanceof ProgramVariable pv) {
                set.add(pv);
            }
        } else if (update.op() == UpdateJunctor.PARALLEL_UPDATE) {
            collectOverwrittenPVs(update.sub(0), set);
            collectOverwrittenPVs(update.sub(1), set);
        }
    }

    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo mc,
            LogicServices services) {
        SVInstantiations svInst = (SVInstantiations) mc.getInstantiations();
        Term uInst = (Term) svInst.getInstantiation(u);
        Term xInst = (Term) svInst.getInstantiation(x);
        Term resultInst = (Term) svInst.getInstantiation(result);
        if (uInst == null || xInst == null) {
            return mc;
        }

        Term properResultInst = dropEffectlessMutating(uInst, xInst, (Services) services);
        if (properResultInst == null) {
            return null;
        } else if (resultInst == null) {
            svInst = svInst.add(result, properResultInst, services);
            return mc.setInstantiations(svInst);
        } else if (resultInst.equals(properResultInst)) {
            return mc;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "\\dropEffectlessMutating(" + u + ", " + x + ", " + result + ")";
    }
}
