/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.init;

import java.io.IOException;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.Path;
import org.key_project.rusty.ast.PathSegment;
import org.key_project.rusty.ast.ResDef;
import org.key_project.rusty.ast.expr.*;
import org.key_project.rusty.ast.stmt.ExpressionStatement;
import org.key_project.rusty.logic.op.ProgramFunction;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.rusty.settings.Configuration;
import org.key_project.rusty.speclang.FunctionalOperationContract;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

///
/// The proof obligation for operation contracts.
///
///
/// The generated [Sequent] has the following form:
/// <pre>
///
/// `==><generalAssumptions>
/// &<preconditions>-><updatesToStoreInitialValues><modalityStart>exc=null;try{<methodBodyExpand>}catch(java.lang.Throwable
/// e){exc = e}<modalityEnd>(exc = null & <postconditions > & <optionalUninterpretedPredicate>)`
/// </pre>
///
public class FunctionalOperationContractPO extends AbstractOperationPO implements ContractPO {
    private final FunctionalOperationContract contract;
    private Term mbyAtPre;

    public FunctionalOperationContractPO(InitConfig initConfig,
            FunctionalOperationContract contract) {
        super(initConfig, new Name(contract.getName()));
        this.contract = contract;
    }

    @Override
    protected ProgramFunction getProgramFunction() {
        return getContract().getTarget();
    }

    @Override
    protected RModality.RustyModalityKind getTerminationMarker() {
        return getContract().getModalityKind();
    }

    @Override
    protected Term getPre(ImmutableList<ProgramVariable> paramVars, Services proofServices) {
        final Term freePre = contract.getFreePre(null, paramVars, proofServices);
        final Term pre = contract.getPre(null, paramVars, proofServices);
        return freePre != null ? tb.and(freePre, pre) : pre;
    }

    @Override
    protected Term generateMbyAtPreDef(ImmutableList<ProgramVariable> paramVars,
            Services services) {
        final Term mbyAtPreDef;
        if (contract.hasMby()) {
            final Term mby = contract.getMby(null, paramVars, services);
            mbyAtPreDef = tb.measuredBy(mby);
        } else {
            mbyAtPreDef = tb.measuredByEmpty();
        }
        return mbyAtPreDef;
    }

    @Override
    protected Term getPost(ImmutableList<ProgramVariable> paramVars, ProgramVariable resultVar,
            Services proofServices) {
        return contract.getPost(null, paramVars, resultVar, proofServices);
    }

    @Override
    protected BlockExpression buildOperationBlock(ImmutableList<ProgramVariable> formalParamVars,
            ProgramVariable resultVar, Services proofServices) {
        ProgramFunction target = contract.getTarget();
        var callee = new PathExpr(new Path<>(new ResDef(target), new ImmutableArray<>(
            new PathSegment(target.getFunction().name().toString(),
                new ResDef(target)))),
            target.getType().getRustyType());
        return new BlockExpression(ImmutableList.of(
            new ExpressionStatement(
                new FunctionBodyExpression(resultVar, target,
                    new CallExpression(callee, new ImmutableArray<>(formalParamVars.toList()))),
                true)),
            null);
    }

    /// {@inheritDoc}
    @Override
    public FunctionalOperationContract getContract() {
        return contract;
    }

    @Override
    public Configuration createLoaderConfig() throws IOException {
        var c = super.createLoaderConfig();
        c.set("contract", contract.getName());
        return c;
    }

    /// {@inheritDoc}
    @Override
    public Term getMbyAtPre() {
        return mbyAtPre;
    }
}
