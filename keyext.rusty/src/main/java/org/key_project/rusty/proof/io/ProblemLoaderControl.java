/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.io;


import org.key_project.rusty.proof.ProofAggregate;
import org.key_project.rusty.proof.init.IPersistablePO;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.proof.io.AbstractProblemLoader.ReplayResult;
import org.key_project.util.collection.ImmutableSet;

/// Allows to observe and control the loading performed by an [AbstractProblemLoader].
///
/// @author Martin Hentschel
public interface ProblemLoaderControl {
    /// The loading has started.
    ///
    /// @param loader The source [AbstractProblemLoader].
    void loadingStarted(AbstractProblemLoader loader);

    /// The loading has stopped.
    ///
    /// @param loader The source [AbstractProblemLoader].
    /// @param poContainer The loaded [LoadedPOContainer].
    /// @param proofList The created [ProofAggregate].
    /// @param result The occurred [ReplayResult].
    /// @throws ProblemLoaderException Occurred Exception.
    void loadingFinished(AbstractProblemLoader loader, IPersistablePO.LoadedPOContainer poContainer,
            ProofAggregate proofList, ReplayResult result) throws ProblemLoaderException;

    /// This method is called if no [LoadedPOContainer] was created via
    /// [#createProofObligationContainer()] and can be overwritten for
    /// instance to open the proof management dialog as done by [AbstractProblemLoader].
    ///
    /// @return true if the proof obligation was selected, and false if action was aborted
    boolean selectProofObligation(InitConfig initConfig);

    /// Report the occurred warnings.
    ///
    /// @param warnings The occurred warnings.
    void reportWarnings(ImmutableSet<String> warnings);
}
