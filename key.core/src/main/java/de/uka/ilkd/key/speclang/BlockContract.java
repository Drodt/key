/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.speclang;

import java.util.Map;
import java.util.function.UnaryOperator;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.op.IObserverFunction;
import de.uka.ilkd.key.logic.op.LocationVariable;
import de.uka.ilkd.key.util.InfFlowSpec;

import org.key_project.util.collection.ImmutableList;

/**
 * <p>
 * A block contract.
 * </p>
 *
 * <p>
 * When a block contract is encountered in an existing proof, a {@code BlockContract} is used. To
 * generate a new proof obligation for a block contract, use {@link FunctionalBlockContract}
 * instead.
 * </p>
 *
 * @author wacker, lanzinger
 */
public interface BlockContract extends AuxiliaryContract {

    /**
     *
     * @param newBlock the new block.
     * @param newPreconditions the new preconditions.
     * @param newPostconditions the new postconditions.
     * @param newModifiableClauses the new modifiable clauses.
     * @param newFreeModifiableClauses the new free modifiable clauses.
     * @param newInfFlowSpecs the new information flow specifications.
     * @param newVariables the new variables.
     * @param newMeasuredBy the new measured-by clause.
     * @return a new block contract with the specified attributes.
     */
    BlockContract update(StatementBlock newBlock,
            Map<LocationVariable, JTerm> newPreconditions,
            Map<LocationVariable, JTerm> newFreePreconditions,
            Map<LocationVariable, JTerm> newPostconditions,
            Map<LocationVariable, JTerm> newFreePostconditions,
            Map<LocationVariable, JTerm> newModifiableClauses,
            Map<LocationVariable, JTerm> newFreeModifiableClauses,
            final ImmutableList<InfFlowSpec> newInfFlowSpecs, Variables newVariables,
            JTerm newMeasuredBy);

    /**
     * @param newKJT the type containing the new target method.
     * @param newPM the new target method.
     * @return a new block contract equal to this one except that it belongs to a different target.
     */
    @Override
    BlockContract setTarget(KeYJavaType newKJT, IObserverFunction newPM);

    /**
     * @param newBlock the new block.
     * @return a new block contract equal to this one except that it belongs to a different block.
     */
    @Override
    BlockContract setBlock(StatementBlock newBlock);

    @Override
    BlockContract map(UnaryOperator<JTerm> op, Services services);

    /**
     *
     * @return the {@code LoopContract} from which this contract was generated, or {@code null}.
     * @see LoopContract#toBlockContract()
     */
    LoopContract toLoopContract();
}
