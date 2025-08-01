/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.conditions;


import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.FormulaSV;
import de.uka.ilkd.key.logic.op.TermSV;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;


public final class EqualUniqueCondition implements VariableCondition {
    private final TermSV t;
    private final TermSV t2;
    private final FormulaSV res;


    public EqualUniqueCondition(TermSV t, TermSV t2, FormulaSV res) {
        this.t = t;
        this.t2 = t2;
        this.res = res;
    }


    private static JTerm equalUnique(JTerm t1, JTerm t2, TermServices services) {
        if (!(t1.op() instanceof Function && t2.op() instanceof Function
                && ((Function) t1.op()).isUnique() && ((Function) t2.op()).isUnique())) {
            return null;
        } else if (t1.op() == t2.op()) {
            JTerm result = services.getTermBuilder().tt();
            for (int i = 0, n = t1.arity(); i < n; i++) {
                result = services.getTermBuilder().and(result,
                    services.getTermBuilder().equals(t1.sub(i), t2.sub(i)));
            }
            return result;
        } else {
            return services.getTermBuilder().ff();
        }
    }


    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo mc,
            LogicServices services) {
        var svInst = (SVInstantiations) mc.getInstantiations();
        JTerm tInst = svInst.getInstantiation(t);
        JTerm t2Inst = svInst.getInstantiation(t2);
        JTerm resInst = (JTerm) svInst.getInstantiation(res);
        if (tInst == null || t2Inst == null) {
            return mc;
        }

        JTerm properResInst = equalUnique(tInst, t2Inst, (TermServices) services);
        if (properResInst == null) {
            return null;
        } else if (resInst == null) {
            svInst = svInst.add(res, properResInst, services);
            return mc.setInstantiations(svInst);
        } else if (resInst.equals(properResInst)) {
            return mc;
        } else {
            return null;
        }
    }


    @Override
    public String toString() {
        return "\\equalUnique (" + t + ", " + t2 + ", " + res + ")";
    }
}
