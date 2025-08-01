/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.informationflow.po.snippet;

import java.util.Set;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.proof.init.ProofObligationVars;

import org.key_project.logic.op.QuantifiableVariable;


public class SelfcomposedLoopSnippet extends ReplaceAndRegisterMethod
        implements InfFlowFactoryMethod {

    @Override
    public JTerm produce(BasicSnippetData d, ProofObligationVars poVars1,
            ProofObligationVars poVars2) {
        BasicPOSnippetFactory f1 = POSnippetFactory.getBasicFactory(d, poVars1);
        BasicPOSnippetFactory f2 = POSnippetFactory.getBasicFactory(d, poVars2);
        final JTerm exec1 = f1.create(BasicPOSnippetFactory.Snippet.LOOP_EXEC_WITH_INV);
        final Set<QuantifiableVariable> qvsToReplace = collectQuantifiableVariables(exec1);
        final JTerm updatedExec1 =
            d.tb.apply(d.tb.elementary(d.tb.getBaseHeap(), poVars1.pre.heap), exec1);
        final JTerm exec2 = replaceQuantifiableVariables(
            f2.create(BasicPOSnippetFactory.Snippet.LOOP_EXEC_WITH_INV), qvsToReplace, d.services);
        final JTerm updatedExec2 =
            d.tb.apply(d.tb.elementary(d.tb.getBaseHeap(), poVars2.pre.heap), exec2);

        return d.tb.and(updatedExec1, updatedExec2);
    }
}
