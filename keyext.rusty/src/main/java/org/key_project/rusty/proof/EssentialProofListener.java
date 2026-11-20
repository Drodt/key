/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.proof;

/// Marker for proof listeners that are part of the proving machinery itself and must keep receiving
/// events even while non-essential observers are suspended.
///
/// During automatic proof search — especially once goals are processed on worker threads
/// — we want to detach
/// everything that merely _observes_ the proof (caching, slicing, origin labels, GUI tree
/// models, …) so that nothing unrelated to proving runs on a worker thread or mutates shared
/// state concurrently. [Proof#suspendNonEssentialListeners()] removes exactly those listeners
/// that are **not** tagged with this marker, and re-attaches them when the run
/// finishes.
///
/// Apply this marker only to listeners that maintain state the prover itself relies on (e.g. proof
/// correctness / contract-dependency bookkeeping). Pure observers must _not_ implement it.
///
/// **Failure semantics.** The marker also decides what happens when a listener throws while
/// handling an event (see `Proof#notifyListeners`): a non-essential observer is logged and
/// unregistered and the proof search continues -- its brokenness cannot corrupt the proof. A
/// failing _essential_ listener, in contrast, means the proving machinery itself is broken
/// and the step that fired the event may already have left the proof or a goal inconsistent:
/// the proof is then marked [erroneous][Proof#isErroneous()], which cooperatively stops
/// the running search and refuses to start a new one, while the proof object stays intact so the
/// user can still save it for a later reload attempt.
///
/// @see Proof#suspendNonEssentialListeners()
/// @see Proof#isErroneous()
public interface EssentialProofListener {
}
