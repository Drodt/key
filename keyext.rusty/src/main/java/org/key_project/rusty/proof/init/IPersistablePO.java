/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.init;

import java.io.IOException;
import java.util.Properties;

import org.key_project.prover.sequent.Sequent;
import org.key_project.rusty.proof.io.AbstractProblemLoader;
import org.key_project.rusty.proof.io.ProofSaver;
import org.key_project.rusty.settings.Configuration;

///
/// This interface extends the standard proof obligation
/// ([de.uka.ilkd.key.proof.init.ProofOblInput]) with functionality
/// to define the individual parameters which are required for loading and saving `*.proof`
/// files.
///
///
/// During save process the [ProofSaver] calls method [#createLoaderConfig()].
/// This proof obligation has to store all information in the given [Properties] which are
/// required to reconstruct it. The class ([#getClass()]) of this class must be stored in
/// the
/// [Properties] with key [#PROPERTY_CLASS].
///
///
/// During load process the [de.uka.ilkd.key.proof.io.AbstractProblemLoader] tries to execute a
/// static method on the class
/// defined via [Properties] key [#PROPERTY_CLASS] with the following signature:
/// `public static LoadedPOContainer loadFrom(InitConfig initConfig, Properties properties) throws IOException`
/// The returned [de.uka.ilkd.key.proof.init.IPersistablePO.LoadedPOContainer] contains the
/// instantiated [de.uka.ilkd.key.proof.init.ProofOblInput] together
/// with the proof index.
///
///
/// @author Martin Hentschel
/// @see ProofSaver
/// @see AbstractProblemLoader
public interface IPersistablePO extends ProofOblInput {
    /// The key used to store [#getClass()].
    String PROPERTY_CLASS = "class";

    /// The key used to store [#name()].
    String PROPERTY_NAME = "name";

    /// The key used to store the file name under which a PO is loaded. This key is set during
    /// loading by the loader and needs not be saved.
    String PROPERTY_FILENAME = "#key.filename";

    /// This method is called by a [ProofSaver] to store the proof specific settings in the
    /// given [Properties]. The stored settings have to contain all information required to
    /// instantiate the proof obligation again and this instance should create the same
    /// [Sequent] (if code and specifications are unchanged).
    ///
    /// @return
    /// @throws IOException Occurred Exception.
    Configuration createLoaderConfig() throws IOException;

    /// The class stored in a [Properties] instance via key must provide the static method with
    /// the following signature:
    /// `public static LoadedPOContainer loadFrom(InitConfig initConfig, Properties properties) throws IOException`
    /// This method is called by the [AbstractProblemLoader] to
    /// recreate a proof obligation. This class
    /// defines the result of this method which is the created proof obligation and its proof number.
    ///
    /// @author Martin Hentschel
    class LoadedPOContainer {
        /// The created [ProofOblInput].
        private final ProofOblInput proofOblInput;

        /// The proof number which is `0` by default.
        private final int proofNum;

        /// Constructor.
        ///
        /// @param proofOblInput The created [ProofOblInput].
        public LoadedPOContainer(ProofOblInput proofOblInput) {
            this(proofOblInput, 0);
        }

        /// Constructor.
        ///
        /// @param proofOblInput The created [ProofOblInput].
        /// @param proofNum The proof number which is `0` by default.
        public LoadedPOContainer(ProofOblInput proofOblInput, int proofNum) {
            super();
            this.proofOblInput = proofOblInput;
            this.proofNum = proofNum;
        }

        /// Returns the created [ProofOblInput].
        ///
        /// @return The created [ProofOblInput].
        public ProofOblInput getProofOblInput() {
            return proofOblInput;
        }

        /// Returns the proof number which is `0` by default.
        ///
        /// @return The proof number which is `0` by default.
        public int getProofNum() {
            return proofNum;
        }
    }
}
