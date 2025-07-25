/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.informationflow.po.snippet;

import java.util.Iterator;

import de.uka.ilkd.key.java.Label;
import de.uka.ilkd.key.java.Statement;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.reference.ExecutionContext;
import de.uka.ilkd.key.java.statement.MethodFrame;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.proof.init.ProofObligationVars;
import de.uka.ilkd.key.rule.AuxiliaryContractBuilders;
import de.uka.ilkd.key.speclang.AuxiliaryContract;

import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;


/**
 *
 * @author christoph
 */
class BasicBlockExecutionSnippet extends ReplaceAndRegisterMethod implements FactoryMethod {

    @Override
    public JTerm produce(BasicSnippetData d, ProofObligationVars poVars)
            throws UnsupportedOperationException {
        ImmutableList<JTerm> posts = ImmutableSLList.nil();
        if (poVars.post.self != null) {
            posts = posts.append(d.tb.equals(poVars.post.self, poVars.pre.self));
        }
        Iterator<JTerm> localVars = d.origVars.localVars.iterator();
        Iterator<JTerm> localPostVars = poVars.post.localVars.iterator();
        while (localVars.hasNext()) {
            posts = posts.append(d.tb.equals(localPostVars.next(), localVars.next()));
        }
        if (poVars.post.result != null) {
            posts = posts.append(d.tb.equals(poVars.post.result, poVars.pre.result));
        }
        if (poVars.pre.exception != null && poVars.post.exception != null) {
            posts = posts.append(d.tb.equals(poVars.post.exception, poVars.pre.exception));
        }
        posts = posts.append(d.tb.equals(poVars.post.heap, d.tb.getBaseHeap()));
        final JTerm prog = buildProgramTerm(d, poVars, d.tb.and(posts), d.tb);
        return prog;
    }

    private JTerm buildProgramTerm(BasicSnippetData d, ProofObligationVars vs, JTerm postTerm,
            TermBuilder tb) {
        if (d.get(BasicSnippetData.Key.MODALITY) == null) {
            throw new UnsupportedOperationException(
                "Tried to produce a " + "program-term for a " + "contract without modality.");
        }

        // create java block
        JModality.JavaModalityKind kind =
            (JModality.JavaModalityKind) d.get(BasicSnippetData.Key.MODALITY);
        final JavaBlock jb = buildJavaBlock(d, vs);

        // create program term
        final JModality.JavaModalityKind symbExecMod;
        if (kind == JModality.JavaModalityKind.BOX) {
            symbExecMod = JModality.JavaModalityKind.DIA;
        } else {
            symbExecMod = JModality.JavaModalityKind.BOX;
        }
        final JTerm programTerm = tb.prog(symbExecMod, jb, postTerm);

        // create update
        JTerm update = tb.skip();
        Iterator<JTerm> paramIt = vs.pre.localVars.iterator();
        Iterator<JTerm> origParamIt = d.origVars.localVars.iterator();
        while (paramIt.hasNext()) {
            JTerm paramUpdate = d.tb.elementary(origParamIt.next(), paramIt.next());
            update = tb.parallel(update, paramUpdate);
        }
        if (vs.post.self != null) {
            final JTerm selfTerm = (JTerm) d.get(BasicSnippetData.Key.BLOCK_SELF);
            final JTerm selfUpdate = d.tb.elementary(selfTerm, vs.pre.self);
            update = tb.parallel(selfUpdate, update);
        }
        return tb.apply(update, programTerm);
    }


    private JavaBlock buildJavaBlock(BasicSnippetData d, ProofObligationVars poVars) {
        final ExecutionContext context =
            (ExecutionContext) d.get(BasicSnippetData.Key.EXECUTION_CONTEXT);
        final ProgramVariable exceptionParameter =
            poVars.exceptionParameter.op(ProgramVariable.class);

        // create block call
        final Label[] labelsArray = (Label[]) d.get(BasicSnippetData.Key.LABELS);
        final ImmutableArray<Label> labels = new ImmutableArray<>(labelsArray);
        final AuxiliaryContract.Variables variables =
            (AuxiliaryContract.Variables) d.get(BasicSnippetData.Key.BLOCK_VARS);
        final StatementBlock block = (StatementBlock) d.get(BasicSnippetData.Key.TARGET_BLOCK);
        final StatementBlock sb = new AuxiliaryContractBuilders.ValidityProgramConstructor(labels,
            block, variables, exceptionParameter, d.services).construct();
        final Statement s = new MethodFrame(null, context, sb);
        final JavaBlock result = JavaBlock.createJavaBlock(new StatementBlock(s));

        return result;
    }
}
