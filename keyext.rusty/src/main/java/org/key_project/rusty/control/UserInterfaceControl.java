/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.ProofAggregate;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.proof.init.Profile;
import org.key_project.rusty.proof.init.ProofInputException;
import org.key_project.rusty.proof.init.ProofOblInput;
import org.key_project.rusty.proof.io.AbstractProblemLoader;
import org.key_project.rusty.proof.io.ProblemLoaderException;

/// Provides the user interface independent logic to manage multiple proofs. This includes:
///
/// - Functionality to load files via
/// [#load(Profile,File,List,File,List,Properties,boolean,Consumer)].
/// - Functionality to instantiate new [Proof]s via
/// [#createProof(InitConfig,ProofOblInput)].
/// - Functionality to register existing [Proof]s in the user interface via
/// [#registerProofAggregate(ProofAggregate)].
///
///
/// @author Martin Hentschel
public interface UserInterfaceControl {
    ///
    /// Opens a java file in this [UserInterfaceControl] and returns the instantiated
    /// [AbstractProblemLoader] which can be used to instantiated proofs programmatically.
    ///
    ///
    /// **The loading is performed in the [Thread] of the caller!**
    ///
    ///
    /// @param profile An optional [Profile] to use. If it is `null` the default profile
    /// [#getDefaultProfile()] is used.
    /// @param file The java file to open.
    /// @param classPaths The class path entries to use.
    /// @param bootClassPath The boot class path to use.
    /// @param includes Optional includes to consider.
    /// @param poPropertiesToForce Some optional [Properties] for the PO which extend or
    /// overwrite saved PO [Properties].
    /// @param forceNewProfileOfNewProofs `` true
    /// `AbstractProblemLoader.profileOfNewProofs` will be used as [Profile] of
    /// new proofs, `false` [Profile] specified by problem file will be used for
    /// new proofs.
    /// @param callbackProofLoaded receives the proof after it is loaded, but before it is replayed
    /// @return The opened [AbstractProblemLoader].
    /// @throws ProblemLoaderException Occurred Exception.
    AbstractProblemLoader load(Profile profile, File file, List<File> classPaths,
            File bootClassPath, List<File> includes, Properties poPropertiesToForce,
            boolean forceNewProfileOfNewProofs,
            Consumer<Proof> callbackProofLoaded) throws ProblemLoaderException;

    /// Instantiates a new [Proof] in this [UserInterfaceControl] for the given
    /// [ProofOblInput] based on the [InitConfig].
    ///
    /// @param initConfig The [InitConfig] which provides the source code.
    /// @param input The description of the [Proof] to instantiate.
    /// @return The instantiated [Proof].
    /// @throws ProofInputException Occurred Exception.
    Proof createProof(InitConfig initConfig, ProofOblInput input) throws ProofInputException;

    /// Registers an already created [ProofAggregate] in this [UserInterfaceControl].
    ///
    /// @param pa The [ProofAggregate] to register.
    void registerProofAggregate(ProofAggregate pa);


}
