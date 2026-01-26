/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.util;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.prover.engine.ProofSearchInformation;
import org.key_project.prover.engine.ProverCore;
import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.ProofAggregate;
import org.key_project.rusty.proof.calculus.RustySequentKit;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.proof.init.Profile;
import org.key_project.rusty.proof.init.ProofInputException;
import org.key_project.rusty.proof.init.ProofOblInput;
import org.key_project.rusty.proof.io.ProofSaver;
import org.key_project.rusty.proof.mgt.ProofEnvironment;
import org.key_project.rusty.prover.impl.ApplyStrategy;
import org.key_project.rusty.strategy.Strategy;
import org.key_project.rusty.strategy.StrategyFactory;
import org.key_project.rusty.strategy.StrategyProperties;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// This class encapsulates the registration of a proof for a given problem. It then starts a proof
/// attempt.
///
/// After the proof attempt stops (successfully or not) the side proof is by default unregistered,
/// but can be accessed via this class.
///
/// @author Richard Bubel
public class ProofStarter {
    private @Nullable Proof proof;

    private int maxSteps = 2000;

    private long timeout = -1L;

    private @Nullable final ProverTaskListener ptl;

    // private @Nullable AutoSaver autoSaver;

    private @Nullable Strategy<@NonNull Goal> strategy;

    /// creates an instance of the ProofStarter
    ///
    /// @param useAutoSaver boolean indicating whether the proof shall be auto saved
    public ProofStarter(boolean useAutoSaver) {
        this(null, useAutoSaver);
    }

    /// creates an instance of the ProofStarter
    ///
    /// @param ptl the ProverTaskListener to be informed about certain events
    /// @param useAutoSaver boolean indicating whether the proof shall be auto saved
    public ProofStarter(@Nullable ProverTaskListener ptl, boolean useAutoSaver) {
        this.ptl = ptl;
        if (useAutoSaver) {
            // autoSaver = AutoSaver.getDefaultInstance();
        }
    }

    /// creates a new proof object for formulaToProve and registers it in the given environment
    ///
    /// @throws ProofInputException if the proof obligation generation fails
    public void init(Term formulaToProve, ProofEnvironment env) throws ProofInputException {
        final ProofOblInput input = new UserProvidedInput(formulaToProve, env);
        proof = input.getPO().getFirstProof();
        proof.setEnv(env);
    }

    /// Initializes the proof starter with the provided proof object. All settings for the proof
    /// search
    /// are queried from the proof object
    ///
    /// @param proof the [Proof] on which to run the proof attempt
    public void init(Proof proof) {
        this.proof = proof;
        this.setMaxRuleApplications(proof.getSettings().getStrategySettings().getMaxSteps());
        this.setTimeout(proof.getSettings().getStrategySettings().getTimeout());
        this.setStrategy(proof.getActiveStrategy());
    }

    /// set maximal steps to be performed
    ///
    /// @param maxSteps the maximal proof steps (rule applications) to be applied during proof
    /// search
    public void setMaxRuleApplications(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    /// set timeout for the proof search; the value `-1` disables the timeout
    ///
    /// @param timeout long specifying the time limit in milliseconds (ms)
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setStrategy(Strategy<@NonNull Goal> strategy) {
        this.strategy = strategy;
    }

    /// Starts proof attempt. The proof object must be created at that point.
    ///
    /// @return the proof after the attempt terminated
    ///
    /// @throws NullPointerException if the proof object is not yet created
    public ProofSearchInformation<Proof, Goal> start() {
        return start(proof.openGoals());
    }

    /// Starts the proof attempt. The proof object must be created at that point.
    ///
    /// @return the proof after the attempt terminated
    /// @throws NullPointerException if the proof object is not yet created
    public ProofSearchInformation<@NonNull Proof, Goal> start(ImmutableList<Goal> goals) {
        try {
            final Profile profile = proof.getInitConfig().getProfile();

            if (strategy == null) {
                StrategyFactory factory = profile.getDefaultStrategyFactory();
                StrategyProperties sp = factory.getSettingsDefinition()
                        .getDefaultPropertiesFactory().createDefaultStrategyProperties();
                strategy = factory.create(proof, sp);
            }

            proof.setActiveStrategy(strategy);

            var goalChooser = profile.<Proof, Goal>getSelectedGoalChooserBuilder().create();
            ProverCore<@NonNull Proof, Goal> prover = new ApplyStrategy(goalChooser);
            if (ptl != null) {
                prover.addProverTaskObserver(ptl);
            }
            // if (autoSaver != null) {
            // autoSaver.setProof(proof);
            // prover.addProverTaskObserver(autoSaver);
            // }

            ProofSearchInformation<@NonNull Proof, Goal> result;
            proof.setRuleAppIndexToAutoMode();

            result = prover.start(proof, goals, maxSteps, timeout,
                strategy.isStopAtFirstNonCloseableGoal());

            if (result.isError()) {
                throw new RuntimeException(
                    "Proof attempt failed due to exception:" + result.getException(),
                    result.getException());
            }

            if (ptl != null) {
                prover.removeProverTaskObserver(ptl);
            }
            // if (autoSaver != null) {
            // prover.removeProverTaskObserver(autoSaver);
            // autoSaver.setProof(null);
            // }

            return result;
        } finally {
            proof.setRuleAppIndexToInteractiveMode();
        }
    }

    /// Proof obligation for a given formula or sequent
    public static class UserProvidedInput implements ProofOblInput {
        private static final String EMPTY_PROOF_HEADER = "";
        private final ProofEnvironment env;
        private final Sequent seq;
        private final String proofName;

        public UserProvidedInput(Sequent seq, ProofEnvironment env) {
            this(seq, env, null);
        }

        public UserProvidedInput(Sequent seq, ProofEnvironment env, @Nullable String proofName) {
            this.seq = seq;
            this.env = env;
            this.proofName = proofName != null ? proofName
                    : "ProofObligation for " + ProofSaver.printAnything(seq, null);
        }

        public UserProvidedInput(Term formula, ProofEnvironment env) {
            this(
                RustySequentKit.createSuccSequent(
                    ImmutableSLList.<SequentFormula>nil().prepend(new SequentFormula(formula))),
                env);
        }

        @Override
        public @NonNull Name name() {
            return new Name(proofName);
        }

        @Override
        public void readProblem() throws ProofInputException {
        }

        private Proof createProof(String proofName) {
            final InitConfig initConfig = env.getInitConfigForEnvironment().deepCopy();

            return new Proof(proofName, seq, initConfig.createTacletIndex(),
                initConfig.createBuiltInRuleIndex(), initConfig);
        }


        @Override
        public ProofAggregate getPO() {
            final Proof proof = createProof(proofName);
            return ProofAggregate.createProofAggregate(proof,
                "ProofAggregate for claim: " + proof.name());
        }

        @Override
        public boolean implies(ProofOblInput po) {
            return this == po;
        }
    }
}
