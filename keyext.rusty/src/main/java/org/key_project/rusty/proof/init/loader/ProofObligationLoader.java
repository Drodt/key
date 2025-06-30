/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.init.loader;

import org.key_project.rusty.proof.init.IPersistablePO;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.proof.init.KeYUserProblemFile;
import org.key_project.rusty.settings.Configuration;

import org.jspecify.annotations.NullMarked;

/// Interface for writing the handling of the creation of proof obligations.
///
/// A proof obligation load takes the environment loaded from a key file, and instantiates this into
/// the
/// initial sequent (called proof obligation). [ProofObligationLoader] are loaded with the help
/// of the
/// [java.util.ServiceLoader], hence you need to register them into the `META-INF`
/// folder.
///
/// A [ProofObligationLoader] decides by itself whether it can handle a certain
/// [KeYUserProblemFile],
/// by given the `class` entry from the file's `\proofObligation` configuration object.
///
/// @author Alexander Weigl
/// @version 1 (28.12.23)
@NullMarked
public interface ProofObligationLoader {
    /// Builds the PO from the given environment and `\proofObligation` configuration.
    ///
    /// @param initConfig the key environment
    /// @param properties the `\proofObligation` configuration
    /// @return always a valid PO
    /// @throws Exception in case of an arbitrary exception, e.g., missing information
    ///         `properties`
    IPersistablePO.LoadedPOContainer loadFrom(InitConfig initConfig, Configuration properties)
            throws Exception;

    /// Receiving an identifier (traditionally the fully qualified class name), this method decides
    /// whether it can handle the current situation.
    /// Currently, the identifier corresponds to the `class` entry in the
    /// `\proofObligation` entry in the
    /// [KeYUserProblemFile].
    ///
    /// @param identifier non-null string
    /// @return true if this load handles this type of PO
    boolean handles(String identifier);
}
