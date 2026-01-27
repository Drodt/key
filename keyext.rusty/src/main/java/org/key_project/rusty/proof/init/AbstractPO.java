/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.init;

import java.io.IOException;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.rusty.RustInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.ProofAggregate;
import org.key_project.rusty.proof.RustModel;
import org.key_project.rusty.proof.mgt.SpecificationRepository;
import org.key_project.rusty.rule.NoPosTacletApp;
import org.key_project.rusty.settings.Configuration;
import org.key_project.rusty.speclang.Contract;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

/// An abstract proof obligation implementing common functionality.
public abstract class AbstractPO implements IPersistablePO {
    protected TermBuilder tb;
    protected final InitConfig environmentConfig;
    protected Services environmentServices;
    protected final RustInfo rustInfo;
    protected final SpecificationRepository specRepos;
    protected final Name name;
    protected ImmutableSet<NoPosTacletApp> taclets;
    protected Term poTerm;
    protected String poName;
    private String header;
    private ProofAggregate proofAggregate;

    /// number of currently visited nodes
    private int index = 0;

    protected AbstractPO(InitConfig initConfig, Name name) {
        this.environmentConfig = initConfig;
        this.environmentServices = initConfig.getServices();
        rustInfo = initConfig.getServices().getRustInfo();
        specRepos = initConfig.getServices().getSpecificationRepository();
        this.name = name;
        taclets = DefaultImmutableSet.nil();
    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    protected Proof createProof(String proofName, Term poTerm,
            InitConfig proofConfig) {
        if (proofConfig == null) {
            proofConfig = environmentConfig.deepCopy();
        }
        final var rustModel = proofConfig.getServices().getRustModel();
        createProofHeader(rustModel, proofConfig.getServices());

        final var proof = createProofObject(proofName, header, poTerm, proofConfig);

        assert proof.openGoals().size() == 1 : "expected one first open goal";
        return proof;
    }

    protected Proof createProofObject(String proofName, String proofHeader, Term poTerm,
            InitConfig proofConfig) {
        return new Proof(proofName, poTerm, proofHeader, proofConfig);
    }

    protected abstract InitConfig getCreatedInitConfigForSingleProof();

    @Override
    public ProofAggregate getPO() throws ProofInputException {
        if (proofAggregate != null) {
            return proofAggregate;
        }

        if (poTerm == null) {
            throw new IllegalStateException("No proof obligation terms.");
        }

        InitConfig ic = getCreatedInitConfigForSingleProof();

        var proof = createProof(poName != null ? poName : name.toString(), poTerm, ic);
        if (taclets != null) {
            proof.getOpenGoal(proof.root()).indexOfTaclets().addTaclets(taclets);
        }

        proofAggregate = ProofAggregate.createProofAggregate(proof, name.toString());

        return proofAggregate;
    }

    @Override
    public boolean implies(ProofOblInput po) {
        return equals(po);
    }

    protected void assignPOTerm(Term poTerm) {
        this.poTerm = poTerm;
    }

    @Override
    public Configuration createLoaderConfig() throws IOException {
        var c = new Configuration();
        c.set(IPersistablePO.PROPERTY_CLASS, getClass().getCanonicalName());
        c.set(IPersistablePO.PROPERTY_NAME, name.toString());
        return c;
    }

    /// Returns the name value from the given properties.
    ///
    /// @param properties The properties to read from.
    /// @return The name value.
    public static String getName(Configuration properties) {
        return properties.getString(IPersistablePO.PROPERTY_NAME);
    }

    protected final void register(ImmutableList<ProgramVariable> pvs, Services services) {
        for (var pv : pvs) {
            register(pv, services);
        }
    }

    protected final void register(ProgramVariable pv, Services services) {
        var progVarNames = services.getNamespaces().programVariables();
        if (pv != null && progVarNames.lookup(pv.name()) == null) {
            progVarNames.addSafely(pv);
        }
    }

    /// Creates declarations necessary to save/load proof in textual form (helper for
    /// createProof()).
    private void createProofHeader(
            RustModel model, Services services) {
        if (header != null) {
            return;
        }

        final StringBuilder sb = new StringBuilder(model.asKeYString());

        // contracts
        ImmutableSet<Contract> contractsToSave = specRepos.getAllContracts();
        for (Contract c : contractsToSave) {
            // TODO(DD): Fix proof loading
            if (true || !c.toBeSaved()) {
                contractsToSave = contractsToSave.remove(c);
            }
        }
        if (!contractsToSave.isEmpty()) {
            sb.append("\n\\contracts {\n");
            for (Contract c : contractsToSave) {
                sb.append(c.proofToString(services));
            }
            sb.append("}\n\n");
        }

        header = sb.toString();
    }
}
