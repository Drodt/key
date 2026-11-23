/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.macro;


import org.key_project.logic.Term;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.util.StripedLruCache;

/// Caches whether a Term contains a modality operation.
///
/// @author Julian Wiesler
public class ModalityCache {
    /// Number of independently locked segments of [#termCache].
    private static final int CACHE_STRIPES = 16;

    /// Caches, per term, whether it contains a modality. A [ModalityCache] is shared across
    /// the parallel prover's workers via the macro's filter strategy, and this is on the
    /// per-candidate hot path (`Strategy#isApprovedApp` runs off the commit lock on every worker),
    /// so a single-lock cache would serialize all workers here. The cached value is a pure function
    /// of the term (eviction can only force a recomputation of the same value, never change a
    /// result), so a [StripedLruCache] with per-segment locking is safe and far less
    /// contended.
    private final StripedLruCache<Term, Boolean> termCache =
        new StripedLruCache<>(2000, CACHE_STRIPES);

    private record SequentResult(Sequent sequent, boolean value) {
    }

    /// A single-element cache for the last sequent's result. A [ModalityCache] is shared
    /// across
    /// the parallel prover's workers via the macro's filter strategy -- [#hasModality] is
    /// reached from `Strategy#isApprovedApp`, which runs off the commit lock -- so this field
    /// is
    /// read and written by several threads. It holds an _immutable_ [SequentResult]
    /// record behind a `volatile` reference: a reader reads the reference once and then the
    /// sequent/value together from that one snapshot, so it can never pair one sequent with
    /// another's
    /// value (the torn-read a two-field pair would risk), and the `last.sequent() == sequent`
    /// guard recomputes whenever the snapshot belongs to a different sequent. This replaces a
    /// per-thread [ThreadLocal], which pinned the last sequent (and thus its proof) onto the
    /// long-lived worker-pool threads. Caching more than one sequent did not help: the autopilot
    /// rarely revisits nodes.
    private volatile SequentResult lastSequent;

    /// Constructor
    public ModalityCache() {}

    /*
     * find a modality term
     */
    private boolean termHasModality(Term term) {
        Boolean cached = termCache.get(term);
        if (cached != null) {
            return cached;
        }

        boolean hasModality;

        if (term.op() instanceof RModality) {
            hasModality = true;
        } else {
            hasModality = false;
            for (Term sub : term.subs()) {
                if (termHasModality(sub)) {
                    hasModality = true;
                    break;
                }
            }
        }

        termCache.put(term, hasModality);
        return hasModality;
    }

    /// Checks for a modality term in a sequent
    ///
    /// @param sequent the sequent
    /// @return whether the sequent contained a modality term
    public boolean hasModality(Sequent sequent) {
        final SequentResult last = lastSequent;
        if (last != null && last.sequent() == sequent) {
            return last.value();
        }

        var result = false;
        for (SequentFormula sequentFormula : sequent) {
            if (termHasModality(sequentFormula.formula())) {
                result = true;
                break;
            }
        }

        lastSequent = new SequentResult(sequent, result);
        return result;
    }
}
