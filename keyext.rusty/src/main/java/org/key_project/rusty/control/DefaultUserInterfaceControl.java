/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import org.key_project.rusty.proof.ProofAggregate;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.proof.init.ProofOblInput;
import org.key_project.rusty.proof.mgt.ProofEnvironment;
import org.key_project.util.collection.ImmutableSet;

/// The [DefaultUserInterfaceControl] which allows proving in case that no specific user
/// interface is available.
///
/// In case that no user interface should be used see also [KeYEnvironment] which provides
/// static methods to load source code and to instantiate this class.
///
/// @author Martin Hentschel
/// @see KeYEnvironment
public class DefaultUserInterfaceControl extends AbstractUserInterfaceControl {
    /// The used [DefaultProofControl].
    private final DefaultProofControl proofControl;

    /// Constructor.
    public DefaultUserInterfaceControl() {
        proofControl = new DefaultProofControl(this, this);
    }

    @Override
    protected ProofEnvironment createProofEnvironmentAndRegisterProof(
            ProofOblInput proofOblInput, ProofAggregate proofList, InitConfig initConfig) {
        initConfig.getServices().getSpecificationRepository().registerProof(proofOblInput,
            proofList.getFirstProof());
        return null;
    }

    /// {@inheritDoc}
    @Override
    public DefaultProofControl getProofControl() {
        return proofControl;
    }

    @Override
    public void registerProofAggregate(ProofAggregate pa) {
    }

    @Override
    public boolean selectProofObligation(InitConfig initConfig) {
        return false;
    }

    @Override
    public void reportWarnings(ImmutableSet<String> warnings) {
    }
}
