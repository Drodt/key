/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.key_project.prover.engine.ProverTaskListener;
import org.key_project.prover.engine.TaskFinishedInfo;
import org.key_project.prover.engine.TaskStartedInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.ProofAggregate;
import org.key_project.rusty.proof.init.*;
import org.key_project.rusty.proof.io.AbstractProblemLoader;
import org.key_project.rusty.proof.io.ProblemLoaderControl;
import org.key_project.rusty.proof.io.ProblemLoaderException;
import org.key_project.rusty.proof.io.SingleThreadProblemLoader;
import org.key_project.rusty.proof.mgt.ProofEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUserInterfaceControl
        implements UserInterfaceControl, ProblemLoaderControl, ProverTaskListener {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(AbstractUserInterfaceControl.class);

    /// The registered [ProverTaskListener].
    private final List<ProverTaskListener> proverTaskListener = new CopyOnWriteArrayList<>();

    /// Constructor.
    protected AbstractUserInterfaceControl() {

    }

    /// {@inheritDoc}
    @Override
    public Proof createProof(InitConfig initConfig, ProofOblInput input)
            throws ProofInputException {
        ProblemInitializer init = createProblemInitializer(initConfig.getProfile());
        ProofAggregate proofList = init.startProver(initConfig, input);
        createProofEnvironmentAndRegisterProof(input, proofList, initConfig);
        return proofList.getFirstProof();
    }

    /// registers the proof aggregate at the UI
    ///
    /// @param proofOblInput the [ProofOblInput]
    /// @param proofList the [ProofAggregate]
    /// @param initConfig the [InitConfig] to be used
    /// @return the new [ProofEnvironment] where the [ProofAggregate] has been registered
    protected abstract ProofEnvironment createProofEnvironmentAndRegisterProof(
            ProofOblInput proofOblInput, ProofAggregate proofList, InitConfig initConfig);

    /// {@inheritDoc}
    @Override
    public AbstractProblemLoader load(Profile profile, File file, List<File> includes,
            Properties poPropertiesToForce,
            boolean forceNewProfileOfNewProofs,
            Consumer<Proof> callback) throws ProblemLoaderException {
        AbstractProblemLoader loader = null;
        try {
            loader = new SingleThreadProblemLoader(file, includes,
                profile, null);
            if (callback != null) {
                loader.load(callback);
            } else {
                loader.load();
            }
            return loader;
        } catch (ProblemLoaderException e) {
            if (loader.getProof() != null) {
                loader.getProof().dispose();
            }
            // rethrow that exception
            throw e;
        } catch (Throwable e) {
            if (loader != null && loader.getProof() != null) {
                loader.getProof().dispose();
            }
            throw new ProblemLoaderException(loader, e);
        }
    }

    ///
    /// Creates a new [ProblemInitializer] instance which is configured for this
    /// [UserInterfaceControl].
    ///
    ///
    /// This method is used by nearly all Eclipse based product that uses KeY.
    ///
    ///
    /// @param profile The [Profile] to use.
    /// @return The instantiated [ProblemInitializer].
    protected ProblemInitializer createProblemInitializer(Profile profile) {
        return new ProblemInitializer(new Services(profile));
    }

    @Override
    public void loadingStarted(AbstractProblemLoader loader) {

    }

    @Override
    public void loadingFinished(AbstractProblemLoader loader,
            IPersistablePO.LoadedPOContainer poContainer,
            ProofAggregate proofList, AbstractProblemLoader.ReplayResult result)
            throws ProblemLoaderException {
        if (proofList != null) {
            // avoid double registration at spec repos as that is done already earlier in
            // createProof
            // the UI method should just do the necessarily UI registrations
            createProofEnvironmentAndRegisterProof(poContainer.getProofOblInput(), proofList,
                loader.getInitConfig());
        }
    }

    @Override
    public void taskStarted(TaskStartedInfo info) {
        fireTaskStarted(info);
    }

    @Override
    public void taskProgress(int position) {
        fireTaskProgress(position);
    }

    @Override
    public void taskFinished(TaskFinishedInfo info) {
        fireTaskFinished(info);
    }

    /// Fires the event [#taskStarted(TaskStartedInfo)] to all listener.
    ///
    /// @param info the [TaskStartedInfo] containing general information about the task that is
    /// just about to start
    protected void fireTaskStarted(TaskStartedInfo info) {
        synchronized (proverTaskListener) {
            for (ProverTaskListener l : proverTaskListener) {
                l.taskStarted(info);
            }
        }
    }

    /// Fires the event [#taskProgress(int)] to all listener.
    ///
    /// @param position The current position.
    protected void fireTaskProgress(int position) {
        synchronized (proverTaskListener) {
            for (ProverTaskListener l : proverTaskListener) {
                l.taskProgress(position);
            }
        }
    }

    /// Fires the event [#taskFinished(TaskFinishedInfo)] to all listener.
    ///
    /// @param info The [TaskFinishedInfo].
    protected void fireTaskFinished(TaskFinishedInfo info) {
        try {
            synchronized (proverTaskListener) {
                for (ProverTaskListener l : proverTaskListener) {
                    l.taskFinished(info);
                }
            }
        } catch (Exception e) {
            LOGGER.error("failed to fire task finished event ", e);
        }
    }
}
