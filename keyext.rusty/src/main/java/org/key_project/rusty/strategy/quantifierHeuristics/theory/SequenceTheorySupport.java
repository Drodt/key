/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics.theory;

import java.util.List;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.ldt.SeqLDT;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.util.collection.ImmutableSet;

/// Support for the sequence theory: a sequence read with a compound index is registered as a
/// trigger together with its index.
///
/// Trigger selection registers the smallest subterm that contains the quantified variables of the
/// clause. For `seqGet(s, k + t)` with quantified `t` this is the sum `k + t`.
/// The sum matches every sum of that shape on the sequent and does not determine the sequence
/// `s`. The read determines `s`. This support therefore registers the read as well.
/// Matching the read against `seqGet(s, x)` fails at the index, and the integer theory
/// solves `k + t = x` for `t` there (see [TheoryReasoning#solveForVariable]).
///
/// Compound indices are the normal case for sequences. The rules for subsequence, concatenation
/// and reversal rewrite a read into a read of the underlying sequence at `idx + from`,
/// `idx - seqLen(first)` and `seqLen(seq) - 1 - idx`. Reads `pseqGet` of
/// typed sequences are registered in the same way.
///
/// This support provides no derived triggers and decides no literals.
final class SequenceTheorySupport implements QuantifierTheorySupport {
    /// A candidate in the index position of a sequence read is `PREFER_ENCLOSING`: it
    /// becomes a trigger, and the search continues with the read. Every other candidate is
    /// `ACCEPTABLE`.
    ///
    /// A bare variable is never a candidate, so for a read with a variable as index the read
    /// itself is the smallest candidate. A candidate in the sequence position, for example a
    /// subsequence term, determines the sequence itself.
    ///
    /// @param candidate a trigger candidate that contains the quantified variables
    /// @param enclosing the term the candidate is an argument of, null at the top of a literal
    /// @param services access to the sequence theory operators
    /// @return the verdict
    @Override
    public CandidateVerdict verdictOn(Term candidate, Term enclosing, Services services) {
        final SeqLDT seqLDT = services.getLDTs().getSeqLDT();
        // the index is the second argument of seqGet
        if (enclosing != null && (seqLDT.isSeqGetOp(enclosing.op()))
                && enclosing.sub(1) == candidate) {
            return CandidateVerdict.PREFER_ENCLOSING;
        }
        return CandidateVerdict.ACCEPTABLE;
    }

    /// Provides no derived triggers.
    ///
    /// @param term an accepted trigger term
    /// @param clauseVariables the quantified variables of the clause the trigger belongs to
    /// @param services access to the sequence theory operators
    /// @return the empty list
    @Override
    public List<Term> provideTriggers(Term term,
            ImmutableSet<LogicVariable> clauseVariables, Services services,
            MetavariableFactory metavariableFactory) {
        return List.of();
    }
}
