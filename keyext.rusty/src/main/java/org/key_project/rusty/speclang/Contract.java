/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.speclang;

import java.util.function.UnaryOperator;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.IObserverFunction;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.proof.init.ContractPO;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.proof.init.ProofOblInput;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

/// A contractual agreement about an ObserverFunction.
public interface Contract extends SpecificationElement {
    int INVALID_ID = Integer.MIN_VALUE;

    /// @return `true` if any only if this contract does not necessarily need to be proven in
    /// its own proof obligation. E.g., this is true for [LoopSpecification]s.
    default boolean isAuxiliary() {
        return false;
    }

    /// Returns the id number of the contract. If a contract has instances for several methods
    /// (inheritance!), all instances have the same id. The id is either non-negative or equal to
    /// INVALID_ID.
    int id();

    /// Returns the precondition of the contract.
    ///
    /// @param selfVar self variable
    /// @param paramVars parameter variables
    /// @param services services object
    /// @return precondition
    Term getPre(ProgramVariable selfVar,
            ImmutableList<ProgramVariable> paramVars,
            Services services);

    /// Returns the contracted function symbol.
    IObserverFunction getTarget();

    /// Tells whether the contract contains a measured_by clause.
    boolean hasMby();

    @Nullable
    Term getMby();

    /// Returns the contract in pretty HTML format.
    ///
    /// @param services services instance
    /// @return the html representation
    String getHTMLText(Services services);

    /// Returns the contract in pretty plain text format.
    ///
    /// @param services services instance
    /// @return the plain text representation
    String getPlainText(Services services);

    /// Tells whether, on saving a proof where this contract is available, the contract should be
    /// saved too. (this is currently true for contracts specified directly in DL, but not for JML
    /// contracts)
    ///
    /// @return see above
    boolean toBeSaved();

    @Override
    Contract map(UnaryOperator<Term> op, Services services);

    Contract setID(int newId);

    @Nullable
    Term getGlobalDefs();

    /// Returns the measured_by clause of the contract.
    ///
    /// @param selfVar the self variable
    /// @param paramVars the parameter variables
    /// @param services services object
    /// @return the measured-by term
    @Nullable
    Term getMby(ProgramVariable selfVar, ImmutableList<ProgramVariable> paramVars,
            Services services);

    /// Returns a proof obligation to the passed initConfig.
    ///
    /// @param initConfig the initial configuration
    /// @return the proof obligation
    ContractPO createProofObl(InitConfig initConfig);

    /// Returns a proof obligation to the passed contract and initConfig.
    ///
    /// @param initConfig the initial configuration
    /// @param contract the contract
    /// @return the proof obligation
    ProofOblInput createProofObl(InitConfig initConfig, Contract contract);

    /// Returns a parseable String representation of the contract. Precondition: toBeSaved() must be
    /// true.
    ///
    /// @param services the services instance
    /// @return the (parseable) String representation
    String proofToString(Services services);
}
