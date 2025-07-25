/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.strategy.feature;

import java.util.ArrayList;
import java.util.List;

import de.uka.ilkd.key.java.JavaInfo;
import de.uka.ilkd.key.java.ProgramElement;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.reference.ExecutionContext;
import de.uka.ilkd.key.java.statement.Throw;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.PosInProgram;
import de.uka.ilkd.key.logic.ProgramPrefix;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.rule.TacletApp;

import org.key_project.logic.sort.Sort;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.BinaryFeature;
import org.key_project.prover.strategy.costbased.feature.Feature;

import org.jspecify.annotations.NonNull;

public class ThrownExceptionFeature extends BinaryFeature {

    public static Feature create(String[] blockedExceptions, Services services) {
        return new ThrownExceptionFeature(blockedExceptions, services);
    }

    private final Sort[] filteredExceptions;

    /**
     * creates a feature filtering first active throw statements where the thrown exception is of
     * one of the given types (or their subtypes)
     *
     * @param p_filteredExceptions the String array with the types of the thrown exceptions
     * @param services the Services
     */
    private ThrownExceptionFeature(String[] p_filteredExceptions, Services services) {
        final List<Sort> filtered = new ArrayList<>();

        final JavaInfo javaInfo = services.getJavaInfo();

        for (String p_filteredException : p_filteredExceptions) {
            final KeYJavaType nullPointer = javaInfo.getKeYJavaType(p_filteredException);
            if (nullPointer != null) {
                filtered.add(nullPointer.getSort());
            }
        }
        filteredExceptions = filtered.toArray(new Sort[0]);
    }

    private boolean blockedExceptions(Sort excType) {
        for (Sort filteredException : filteredExceptions) {
            if (excType.extendsTrans(filteredException)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected <Goal extends ProofGoal<@NonNull Goal>> boolean filter(RuleApp app,
            PosInOccurrence pos, Goal goal, MutableState mState) {
        return app instanceof TacletApp tacletApp
                && filter((JTerm) pos.subTerm(), (Services) goal.proof().getServices(),
                    tacletApp.instantiations().getExecutionContext());
    }

    protected boolean filter(JTerm term, Services services, ExecutionContext ec) {
        if (term.op() instanceof JModality) {
            final ProgramElement fstActive = getFirstExecutableStatement(term);
            return fstActive instanceof Throw fstThrow && blockedExceptions(
                fstThrow.getExpressionAt(0).getKeYJavaType(services, ec).getSort());
        }
        return false;
    }

    /**
     * returns the first executable statement (often identical with the first active statement)
     *
     * @param term the Term with the program at top level
     * @return the first executable statement
     */
    private ProgramElement getFirstExecutableStatement(JTerm term) {
        if (term.javaBlock().isEmpty()) {
            return null;
        }

        final ProgramElement jb = term.javaBlock().program();
        final ProgramElement fstActive;

        if (jb instanceof ProgramPrefix prefix) {
            final ProgramPrefix pp = prefix.getLastPrefixElement();
            fstActive = PosInProgram.getProgramAt(pp.getFirstActiveChildPos(), pp);
        } else {
            fstActive = jb;
        }
        return fstActive;
    }

}
