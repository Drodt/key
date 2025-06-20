/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.metaconstruct;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.label.ParameterlessTermLabel;
import de.uka.ilkd.key.logic.op.AbstractTermTransformer;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.rule.inst.SVInstantiations;
import de.uka.ilkd.key.util.MiscTools;

import org.key_project.logic.Name;
import org.key_project.logic.op.Modality;
import org.key_project.logic.op.Operator;

/**
 * Creates the wellformedness condition for the given anonymizing heap terms if they apply for the
 * current profile and modality type. At least generates the "wellFormed(anon_heap_LOOP)" condition
 * for the anonymized standard heap.
 *
 * @author Dominic Steinhoefel
 */
public final class CreateWellformedCond extends AbstractTermTransformer {

    public CreateWellformedCond() {
        super(new Name("#wellFormedCond"), 4);
    }

    @Override
    public JTerm transform(JTerm term, SVInstantiations svInst, Services services) {
        final JTerm anonHeapTerm = term.sub(1);
        final JTerm anonSavedHeapTerm = term.sub(2);
        final JTerm anonPermissionsHeapTerm = term.sub(3);

        final Operator op = term.sub(0).op();
        assert op instanceof JModality;

        return createWellformedCond(MiscTools.isTransaction(((Modality) op).kind()),
            MiscTools.isPermissions(services), anonHeapTerm, anonSavedHeapTerm,
            anonPermissionsHeapTerm, services);
    }

    /**
     * Creates a wellformedness condition containing the applicable heaps.
     *
     * @param isTransaction Signals a transaction modality.
     * @param isPermissions Signals the permission profile.
     * @param anonHeapTerm The Skolem term for the standard heap.
     * @param anonSavedHeapTerm The Skolem term for the saved (transaction) heap.
     * @param anonPermissionsHeapTerm The Skolem term for the permissions heap.
     * @param services The {@link Services} object.
     * @return The wellformedness condition.
     */
    private JTerm createWellformedCond(boolean isTransaction, boolean isPermissions,
            JTerm anonHeapTerm, JTerm anonSavedHeapTerm, JTerm anonPermissionsHeapTerm,
            Services services) {
        final TermBuilder tb = services.getTermBuilder();

        JTerm result =
            tb.label(tb.wellFormed(anonHeapTerm), ParameterlessTermLabel.ANON_HEAP_LABEL);

        if (isTransaction) {
            result = tb.and(result, tb.wellFormed(anonSavedHeapTerm));
        }

        if (isPermissions) {
            result = tb.and(result, tb.wellFormed(anonPermissionsHeapTerm));
        }

        return result;
    }
}
