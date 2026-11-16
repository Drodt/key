/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match;

import org.key_project.logic.Term;
import org.key_project.logic.op.Modality;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.matcher.compiler.*;
import org.key_project.prover.rules.matcher.vm.MatchProgram;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.logic.op.ElementaryUpdate;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.rusty.logic.op.sv.ModalOperatorSV;

import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.rule.match.instructions.RustyDLMatchVMInstructionSet.*;

/// The RustyDL front-end of the term-side [MatchPlanBuilder]: it supplies the Java-DL
/// schema-variable instructions, the per-operator head dispatch, the [RustBinderMatcher]; the
/// recursion over the pattern and the derivation of both back-ends are
/// inherited from the framework. Describing a construct here gives both back-ends at once -- this
/// is the sole source of truth for find-matching; there is no second matcher implementation to
/// keep in sync.
///
/// The dispatch covers the whole Java-DL find-taclet base: the FOL term skeleton (schema variables,
/// ordinary operators with their subterms, bound variables), term labels, elementary updates,
/// parametric function instances and modalities (the Java program is matched through a
/// [org.key_project.prover.rules.matcher.compiler.ProgramMatchHook]). A pattern the dispatch
/// genuinely cannot model yields `null` and the [facades][#interpreterProgram(Term)] raise a clear
/// error pointing at the missing head (no current taclet hits this; adding a
/// construct means adding one head, see the developer docs).
public final class RustMatchPlanBuilder extends MatchPlanBuilder {
    private RustMatchPlanBuilder() {}

    private static final RustMatchPlanBuilder INSTANCE = new RustMatchPlanBuilder();

    /// Builds the cursor-free compiled matcher for `pattern` through the match-plan
    /// framework.
    ///
    /// @param pattern the find pattern
    /// @return the compiled matcher
    public static MatchProgram getCompiledProgram(Term pattern) {
        return INSTANCE.compiledProgram(pattern);
    }

    @Override
    protected MatchInstruction instructionForSV(SchemaVariable sv) {
        return getMatchInstructionForSV(sv);
    }

    /// Summarizes the `\\assumes` pattern `pattern` for indexing (see
    /// [PatternKeySource]), through the same
    /// dispatch ([#headFor]) the matcher is built from.
    ///
    /// @param pattern the assumes pattern
    /// @return the key source of `pattern`
    public static PatternKeySource keySource(Term pattern) {
        return INSTANCE.keySourceFor(pattern);
    }

    /// The operator family of a concrete operator, as a key for indexing: the counterpart of
    /// [org.key_project.prover.rules.matcher.compiler.MatchHead#topOperatorDescriptor()]
    /// for the operators of concrete terms (which have no match head). A key built at some
    /// position of a term names what the matcher observes at that position, so the family of an
    /// operator is the operator itself, up to the identifications the heads make (pinned by
    /// `QueueRuleApplicationManagerWakeKeyTest`):
    ///
    /// - a parametric function instance belongs to its base's family (the head accepts every
    /// instance of the base),
    /// - a modality belongs to its kind's family (the head accepts every modality of the
    /// kind),
    /// - every other operator, update applications and elementary updates included, is its own
    /// family (the generic head accepts by identity).
    ///
    /// Only schematic operators have no family (`null`): a schema variable and a
    /// schematic modality kind fix no operator a key could name.
    ///
    /// The family is position-relative in one respect. The top of an `\\assumes` candidate
    /// is matched after its leading updates have been removed (they are the find's update
    /// context, see `VMTacletMatcher.matchUpdateContext`), so a caller keying a top
    /// position strips leading updates before asking for the family. Every other position is
    /// matched as written, so updates keep their own family there.
    ///
    /// @param op the operator of a concrete term
    /// @return the operator's family, or `null` if the operator is schematic
    public static @Nullable Object matchFamilyOf(Operator op) {
        if (op instanceof SchemaVariable) {
            return null;
        }
        if (op instanceof ParametricFunctionInstance pfi) {
            return pfi.getBase();
        }
        if (op instanceof Modality mod) {
            return mod.kind() instanceof ModalOperatorSV ? null : mod.kind();
        }
        return op;
    }

    @Override
    protected @Nullable MatchHead headFor(Term pattern) {
        final Operator op = pattern.op();
        switch (op) {
            case ElementaryUpdate elUp -> {
                return ElementaryUpdateHead.of(elUp);
            }
            case ParametricFunctionInstance pfi -> {
                return ParametricFunctionHead.of(pfi);
            }
            case RModality mod -> {
                final RustProgramMatchHook hook =
                    RustProgramMatchHook.of(mod.programBlock().program());
                if (hook == null) {
                    return null; // the modality's program cannot be matched by the framework
                }
                final MatchInstruction kindInstr = mod.kind() instanceof ModalOperatorSV sv
                        ? matchModalOperatorSV(sv)
                        : getMatchIdentityInstruction(mod.kind());
                return new ModalityHead(mod.kind(), kindInstr, hook);
            }
            default -> {
            }
        }
        return new GenericOperatorHead(op);
    }

    @Override
    protected BinderMatcher binder() {
        return RustBinderMatcher.INSTANCE;
    }
}
