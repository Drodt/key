/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.key_project.rusty.Services;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.proof.init.Profile;
import org.key_project.rusty.proof.io.AbstractProblemLoader;
import org.key_project.rusty.proof.io.AbstractProblemLoader.ReplayResult;
import org.key_project.rusty.proof.io.ProblemLoaderException;
import org.key_project.rusty.proof.io.SingleThreadProblemLoader;

/// Instances of this class are used to collect and access all relevant information for verification
/// with KeY.
///
/// @author Martin Hentschel
public class KeYEnvironment {
    /// The loaded project.
    private final InitConfig initConfig;

    /// An optional [Proof] which was loaded by the specified proof file.
    private final Proof loadedProof;

    /// Indicates that this [KeYEnvironment] is disposed.
    private boolean disposed;

    /// The [ReplayResult] if available.
    private final ReplayResult replayResult;

    /// Constructor
    ///
    /// @param initConfig The loaded project.
    public KeYEnvironment(InitConfig initConfig, Proof loadedProof, ReplayResult replayResult) {
        this.initConfig = initConfig;
        this.loadedProof = loadedProof;
        this.replayResult = replayResult;
    }

    /// Returns the loaded project.
    ///
    /// @return The loaded project.
    public InitConfig getInitConfig() {
        return initConfig;
    }

    /// Returns the [Services] of [#getInitConfig()].
    ///
    /// @return The [Services] of [#getInitConfig()].
    public Services getServices() {
        return initConfig.getServices();
    }

    public Profile getProfile() {
        return getInitConfig().getProfile();
    }

    /// Returns the loaded [Proof] if a proof file was loaded.
    ///
    /// @return The loaded [Proof] if available and `null` otherwise.
    public Proof getLoadedProof() {
        return loadedProof;
    }

    /// Returns the [ReplayResult] if available.
    ///
    /// @return The [ReplayResult] or `null` if not available.
    public ReplayResult getReplayResult() {
        return replayResult;
    }

    /// Loads the given location and returns all required references as [KeYEnvironment]. The
    /// `MainWindow` is not involved in the whole process.
    ///
    /// @param profile The [Profile] to use.
    /// @param location The location to load.
    /// @param includes Optional includes to consider.
    /// @param poPropertiesToForce Some optional PO [Properties] to force.
    /// @param callbackProofLoaded An optional callback (called when the proof is loaded, before
    ///        replay)
    /// @param forceNewProfileOfNewProofs `` true
    ///        `AbstractProblemLoader.profileOfNewProofs` will be used as [Profile] of
    ///        new proofs, `false` [Profile] specified by problem file will be used for
    ///        new proofs.
    /// @return The [KeYEnvironment] which contains all references to the loaded location.
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment load(Profile profile, File location,
            List<File> includes,
            Properties poPropertiesToForce,
            Consumer<Proof> callbackProofLoaded,
            boolean forceNewProfileOfNewProofs) throws ProblemLoaderException {
        AbstractProblemLoader loader = null;
        try {
            loader = new SingleThreadProblemLoader(location, includes,
                profile, null);
            if (callbackProofLoaded != null) {
                loader.load(callbackProofLoaded);
            } else {
                loader.load();
            }
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
        InitConfig initConfig = loader.getInitConfig();

        return new KeYEnvironment(initConfig, loader.getProof(),
            loader.getResult());
    }

    /// Loads the given location and returns all required references as [KeYEnvironment]. The
    /// `MainWindow` is not involved in the whole process.
    ///
    /// @param profile The [Profile] to use.
    /// @param location The location to load.
    /// @param includes Optional includes to consider.
    /// @param poPropertiesToForce Some optional PO [Properties] to force.
    /// @param forceNewProfileOfNewProofs `` true
    ///        `AbstractProblemLoader.profileOfNewProofs` will be used as [Profile] of
    ///        new proofs, `false` [Profile] specified by problem file will be used for
    ///        new proofs.
    /// @return The [KeYEnvironment] which contains all references to the loaded location.
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment load(Profile profile, File location,
            List<File> includes,
            Properties poPropertiesToForce,
            boolean forceNewProfileOfNewProofs) throws ProblemLoaderException {
        return load(profile, location, includes, poPropertiesToForce,
            null, forceNewProfileOfNewProofs);
    }

    /// Loads the given location and returns all required references as [KeYEnvironment]. The
    /// `MainWindow` is not involved in the whole process.
    ///
    /// @param profile The [Profile] to use.
    /// @param location The location to load.
    /// @param includes Optional includes to consider.
    /// @param forceNewProfileOfNewProofs `` true
    ///        `AbstractProblemLoader.profileOfNewProofs` will be used as
    ///        [Profile] of new proofs, `false` [Profile] specified by problem file
    ///        will be used for new proofs.
    /// @return The [KeYEnvironment] which contains all references to the loaded location.
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment load(Profile profile, File location,
            List<File> includes,
            boolean forceNewProfileOfNewProofs) throws ProblemLoaderException {
        return load(profile, location, includes, null,
            forceNewProfileOfNewProofs);
    }

    /// Loads the given location and returns all required references as [KeYEnvironment]. The
    /// `MainWindow` is not involved in the whole process.
    ///
    /// @param location The location to load.
    /// @param includes Optional includes to consider.
    /// @return The [KeYEnvironment] which contains all references to the loaded location.
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment load(File location,
            List<File> includes)
            throws ProblemLoaderException {
        return load(null, location, includes, false);
    }

    public static KeYEnvironment load(File keyFile)
            throws ProblemLoaderException {
        return load(keyFile, null);
    }

    public void dispose() {
        // TODO
    }
}
