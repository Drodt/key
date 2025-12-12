/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.speclang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.Path;
import org.key_project.rusty.ast.PathSegment;
import org.key_project.rusty.ast.ResDef;
import org.key_project.rusty.ast.abstraction.GenericConstParam;
import org.key_project.rusty.ast.abstraction.PrimitiveType;
import org.key_project.rusty.ast.expr.*;
import org.key_project.rusty.ast.stmt.ExpressionStatement;
import org.key_project.rusty.logic.RustyBlock;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.op.*;
import org.key_project.rusty.pp.LogicPrinter;
import org.key_project.rusty.pp.NotationInfo;
import org.key_project.rusty.proof.OpReplacer;
import org.key_project.rusty.proof.init.ContractPO;
import org.key_project.rusty.proof.init.FunctionalOperationContractPO;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.util.Assert.assertSubSort;

/// Standard implementation of the OperationContract interface.
public class FunctionalOperationContractImpl implements FunctionalOperationContract {
    final String baseName;
    final String name;
    final ProgramFunction fn;
    final RModality.RustyModalityKind modalityKind;
    /// The original precondition terms.
    final Term originalPre;
    final @Nullable Term originalMby;
    /// The original postcondition term.
    final Term originalPost;
    /// The original modifiable clause term.
    final @Nullable Term originalModifiable;
    final ImmutableList<ProgramVariable> originalParamVars;
    final @Nullable ProgramVariable originalResultVar;
    final @Nullable ProgramVariable originalPanicVar;
    final @Nullable Term globalDefs;
    final int id;
    final boolean toBeSaved;

    /// The term builder.
    private final TermBuilder tb;
    /// The services object.
    private final Services services;

    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    FunctionalOperationContractImpl(String baseName, @Nullable String name,
            ProgramFunction fn, RModality.RustyModalityKind modalityKind,
            Term pre, @Nullable Term mby, Term post, @Nullable Term modifiables,
            ImmutableList<ProgramVariable> paramVars, @Nullable ProgramVariable resultVar, @Nullable ProgramVariable panicVar,
            @Nullable Term globalDefs,
            int id, boolean toBeSaved,
            Services services) {
        assert !(name == null && baseName == null);
        assert fn != null;
        assert modalityKind != null;
        assert pre != null;
        assert post != null;
        assert globalDefs == null || globalDefs.sort() == RustyDLTheory.UPDATE;
        assert paramVars != null;
        assert paramVars.size() == fn.getNumParams();
        assert services != null;
        this.services = services;
        tb = services.getTermBuilder();
        this.baseName = baseName;
        this.name = name != null ? name : ContractFactory.generateContractName(baseName, fn, id);
        this.fn = fn;
        this.modalityKind = modalityKind;
        this.originalPre = pre;
        this.originalMby = mby;
        this.originalPost = post;
        this.originalModifiable = modifiables;
        this.originalParamVars = paramVars;
        this.originalResultVar = resultVar;
        this.originalPanicVar = panicVar;
        this.globalDefs = globalDefs;
        this.id = id;
        this.toBeSaved = toBeSaved;
    }

    @Override
    public FunctionalOperationContract map(UnaryOperator<Term> op, Services services) {
        Term newPres = op.apply(originalPre);
        Term newMby = originalMby == null ? null : op.apply(originalMby);
        Term newPost = op.apply(originalPost);
        Term newModifiable = originalModifiable == null ? null : op.apply(originalModifiable);
        Term newGlobalDefs = globalDefs == null ? null : op.apply(globalDefs);

        return new FunctionalOperationContractImpl(baseName, name, fn,
            modalityKind,
            newPres, newMby, newPost, newModifiable,
            originalParamVars, originalResultVar, originalPanicVar, newGlobalDefs,
            id, toBeSaved, services);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public Term getPre(ProgramVariable selfVar, ImmutableList<ProgramVariable> paramVars,
            Services services) {
        assert paramVars != null;
        assert services != null;

        assert paramVars.size() == originalParamVars.size();

        final Map<ProgramVariable, ProgramVariable> replaceMap =
            getReplaceMap(selfVar, paramVars, null, null, services);
        final OpReplacer or = new OpReplacer(replaceMap, services.getTermFactory());
        return or.replace(originalPre);
    }

    @Override
    public ProgramFunction getTarget() {
        return fn;
    }

    @Override
    public boolean hasMby() {
        return originalMby != null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public boolean isPure() {
        return false;
    }

    @Override
    public @Nullable Term getModifiable(Term selfVar, ImmutableList<Term> paramVars,
            Services services) {
        assert paramVars != null;
        assert paramVars.size() == originalParamVars.size();
        assert services != null;
        if (originalModifiable == null) {
            return null;
        }
        final Map<Term, Term> replaceMap =
            getReplaceMap(selfVar, paramVars, null, services);
        final OpReplacer or =
            new OpReplacer(replaceMap, services.getTermFactory());
        return or.replace(originalModifiable);
    }

    @Override
    public @Nullable Term getFreePre(ProgramVariable selfVar,
            ImmutableList<ProgramVariable> paramVars,
            Services services) {
        if (getTarget().getFunction().getGenericParams().length == 0)
            return null;
        var pre = tb.tt();
        for (var genParam : getTarget().getFunction().getGenericParams()) {
            if (genParam instanceof GenericConstParam gcp) {
                // TODO: Get Real type from HIR
                pre = tb.and(pre, tb.reachableValue(tb.func(gcp.fn()),
                    services.getRustInfo().getKeYRustyType(PrimitiveType.USIZE)));
            }
        }
        return pre;
    }

    @Override
    public RModality.RustyModalityKind getModalityKind() {
        return modalityKind;
    }

    @Override
    public Term getEnsures() {
        return originalPost;
    }

    @Override
    public Term getPre(Term selfTerm, ImmutableList<Term> paramTerms, Services services) {
        assert paramTerms != null;
        assert paramTerms.size() == originalParamVars.size();
        assert services != null;
        final Map<Term, Term> replaceMap = getReplaceMap(selfTerm, paramTerms, null, services);
        final OpReplacer or =
            new OpReplacer(replaceMap, services.getTermFactory());
        return or.replace(originalPre);
    }

    @Override
    public Term getPost(Term selfVar, ImmutableList<Term> paramTerms,
            Term resultTerm, Services services) {
        assert paramTerms != null;
        assert paramTerms.size() == originalParamVars.size();
        assert services != null;
        final Map<Term, Term> replaceMap =
            getReplaceMap(selfVar, paramTerms, resultTerm, services);
        final OpReplacer or =
            new OpReplacer(replaceMap, services.getTermFactory());
        return or.replace(originalPost);
    }

    @Override
    public Term getPost(ProgramVariable selfVar, ImmutableList<ProgramVariable> paramVars,
            ProgramVariable resultVar, @Nullable ProgramVariable panicVar, Services services) {
        // assert (selfVar == null) == (originalSelfVar == null);
        assert paramVars != null;
        assert paramVars.size() == originalParamVars.size();
        assert resultVar != null;
        final var replaceMap = getReplaceMap(selfVar, paramVars, resultVar, panicVar, services);
        final OpReplacer or = new OpReplacer(replaceMap, services.getTermFactory());
        return or.replace(originalPost);
    }

    private Map<ProgramVariable, ProgramVariable> getReplaceMap(ProgramVariable selfVar,
            @Nullable ImmutableList<ProgramVariable> paramVars, @Nullable ProgramVariable resultVar, @Nullable ProgramVariable panicVar,
            Services services) {
        final Map<ProgramVariable, ProgramVariable> result = new HashMap<>();

        // TODO: self

        // params
        if (paramVars != null) {
            assert originalParamVars.size() == paramVars.size();
            final var it1 = originalParamVars.iterator();
            final var it2 = paramVars.iterator();
            while (it1.hasNext()) {
                var ogParamVar = it1.next();
                var paramVar = it2.next();
                // allow contravariant parameter types
                assertSubSort(ogParamVar, paramVar);
                result.put(ogParamVar, paramVar);
            }
        }

        // result
        if (resultVar != null) {
            assert originalResultVar != null;
            assertSubSort(resultVar, originalResultVar);
            result.put(originalResultVar, resultVar);
        }

        // panic
        if (panicVar != null) {
            assert originalPanicVar != null;
            assertSubSort(panicVar, originalPanicVar);
            result.put(originalPanicVar, panicVar);
        }

        return result;
    }

    @Override
    public String getBaseName() {
        return "";
    }

    @Override
    public Term getPre() {
        return originalPre;
    }

    @Override
    public Term getPost() {
        return originalPost;
    }

    @Override
    public @Nullable Term getModifiable() {
        return originalModifiable;
    }

    @Override
    public @Nullable Term getMby() {
        return originalMby;
    }

    @Override
    public String getHTMLText(Services services) {
        return "";
    }

    @Override
    public String getPlainText(Services services) {
        return "";
    }

    @Override
    public boolean toBeSaved() {
        return toBeSaved;
    }

    @Override
    public @Nullable Term getGlobalDefs() {
        return globalDefs;
    }

    @Override
    public @Nullable Term getMby(ProgramVariable selfVar, ImmutableList<ProgramVariable> paramVars,
            Services services) {
        return null;
    }

    @Override
    public ContractPO createProofObl(InitConfig initConfig) {
        return createProofObl(initConfig, this);
    }

    @Override
    public FunctionalOperationContractPO createProofObl(InitConfig initConfig, Contract contract) {
        return new FunctionalOperationContractPO(initConfig,
            (FunctionalOperationContract) contract);
    }

    @Override
    public @Nullable Term getSelf() {
        // TODO
        return null;
    }

    @Override
    public ImmutableList<Term> getParams() {
        return tb.var(originalParamVars);
    }

    @Override
    public @Nullable Term getResult() {
        if (originalResultVar == null)
            return null;
        return tb.var(originalResultVar);
    }

    /// Get the according replace-map for the given variables.
    ///
    /// @param selfVar the self variable
    /// @param paramVars the parameter variables
    /// @param resultVar the result variable
    /// @param services the services object
    /// @return the replacement map
    protected Map<Term, Term> getReplaceMap(@Nullable Term selfVar,
            ImmutableList<Term> paramVars, @Nullable Term resultVar,
            Services services) {
        final Map<Term, Term> result = new LinkedHashMap<>();

        // self
        // if (selfVar != null) {
        // assertSubSort(selfVar, originalSelfVar);
        // result.put(originalSelfVar, selfVar);
        // }

        // parameters
        if (paramVars != null) {
            assert originalParamVars.size() == paramVars.size();
            final Iterator<ProgramVariable> it1 = originalParamVars.iterator();
            final Iterator<Term> it2 = paramVars.iterator();
            while (it1.hasNext()) {
                ProgramVariable originalParamVar = it1.next();
                Term paramVar = it2.next();
                // allow contravariant parameter types
                assertSubSort(originalParamVar, paramVar);
                result.put(tb.var(originalParamVar), paramVar);
            }
        }

        // result
        if (resultVar != null) {
            // workaround to allow covariant return types (bug #1384)
            assert originalResultVar != null;
            assertSubSort(resultVar, originalResultVar);
            result.put(tb.var(originalResultVar), resultVar);
        }

        return result;
    }

    @Override
    public Contract setID(int newId) {
        return new FunctionalOperationContractImpl(baseName, null, fn, modalityKind, originalPre,
            originalMby, originalPost,
            originalModifiable, originalParamVars, originalResultVar, originalPanicVar, globalDefs, newId, toBeSaved,
            services);
    }

    @Override
    public String proofToString(Services services) {
        assert toBeSaved;
        final StringBuilder sb = new StringBuilder();
        sb.append('\"').append(baseName).append('\"').append(" {\n");

        // print var decls
        sb.append("  \\programVariables {\n");
        for (var originalParamVar : originalParamVars) {
            sb.append("    ").append(originalParamVar.proofToString());
        }
        if (originalResultVar != null) {
            sb.append("    ").append(originalResultVar.proofToString());
        }
        sb.append("  }\n");

        // prepare Rust program
        final Expr[] args = new ProgramVariable[originalParamVars.size()];
        int i = 0;
        for (var arg : originalParamVars) {
            args[i++] = arg;
        }
        ResDef resDef = new ResDef(fn);
        PathSegment segment = new PathSegment(fn.getFunction().name().toString(), resDef);
        PathExpr callee =
            new PathExpr(new Path<>(resDef, new ImmutableArray<>(segment)), fn.getType());
        final var ce = new CallExpression(callee, new ImmutableArray<>(args));
        final Expr call;
        if (originalResultVar == null) {
            call = ce;
        } else {
            call = new AssignmentExpression(originalResultVar, ce);
        }
        var callStatement = new ExpressionStatement(call, true);
        final BlockExpression sblock = new BlockExpression(ImmutableList.of(callStatement), null);
        final RustyBlock rb = new RustyBlock(sblock);

        // print contract term
        final Term modalityTerm =
            tb.prog(modalityKind, rb, originalPost);
        final Term contractTerm =
            tb.tf().createTerm(Junctor.IMP, originalPre, modalityTerm);
        final LogicPrinter lp = LogicPrinter.purePrinter(new NotationInfo(), null);
        lp.printTerm(contractTerm);
        sb.append(lp.result());

        if (originalModifiable != null) {
            // print modifiable
            lp.reset();
            lp.printTerm(originalModifiable);
            sb.append("  \\modifiable ").append(lp.result());
        }

        sb.append("};\n");
        return sb.toString();
    }
}
