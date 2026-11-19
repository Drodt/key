/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match;

import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.compiler.BinderMatcher;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.util.collection.ImmutableList;

import static org.key_project.rusty.rule.match.instructions.RustyDLMatchVMInstructionSet.matchAndBindVariables;

/// RustyDL implementation of the [BinderMatcher] SPI (service provider interface): bound
/// variables are bound via the
/// `matchAndBindVariables` instruction, and the binding scope is closed via
/// [MatchConditions#shrinkLogicVarTable()]. The renaming table is scope-structured (closing
/// pops one scope), so the bound variables themselves are not needed for unbinding.
public final class RustBinderMatcher implements BinderMatcher {
    /// stateless; a single shared instance suffices.
    public static final RustBinderMatcher INSTANCE = new RustBinderMatcher();

    private RustBinderMatcher() {}

    @SuppressWarnings("unchecked")
    @Override
    public MatchInstruction binder(ImmutableList<? extends QuantifiableVariable> boundVars) {
        return matchAndBindVariables((ImmutableList<QuantifiableVariable>) boundVars);
    }

    @Override
    public MatchResultInfo unbind(MatchResultInfo mc,
            ImmutableList<? extends QuantifiableVariable> boundVars) {
        return ((MatchConditions) mc).shrinkLogicVarTable();
    }
}
