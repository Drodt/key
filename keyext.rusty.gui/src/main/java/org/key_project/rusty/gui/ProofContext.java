/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.util.ArrayList;
import java.util.List;

import org.key_project.rusty.control.KeYEnvironment;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;

import org.jspecify.annotations.Nullable;

/// The single-proof "mediator" of the minimal GUI.
///
/// Unlike KeY-Java's mediator this holds at most one open proof and only the few pieces of state
/// the MVP views need: the loaded environment/proof and the currently selected proof node. Views
/// register as [Listener]s and are notified when the proof or the selection changes.
public final class ProofContext {

    /// Notified about changes to the open proof or the selected node.
    public interface Listener {
        /// A new proof was loaded (or the previous one was closed).
        default void proofLoaded() {}

        /// The proof tree changed (a rule was applied, auto mode ran, ...).
        default void proofChanged() {}

        /// The selected proof node changed.
        default void selectedNodeChanged() {}
    }

    private final List<Listener> listeners = new ArrayList<>();

    private @Nullable KeYEnvironment<?> environment;
    private @Nullable Proof proof;
    private @Nullable Node selectedNode;

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public @Nullable Proof getProof() {
        return proof;
    }

    public @Nullable KeYEnvironment<?> getEnvironment() {
        return environment;
    }

    public @Nullable Node getSelectedNode() {
        return selectedNode;
    }

    /// Replaces the open proof, selects its root and notifies all listeners.
    public void setProof(KeYEnvironment<?> environment, Proof proof) {
        this.environment = environment;
        this.proof = proof;
        this.selectedNode = proof.root();
        listeners.forEach(Listener::proofLoaded);
        listeners.forEach(Listener::selectedNodeChanged);
    }

    public void setSelectedNode(@Nullable Node node) {
        if (node != selectedNode) {
            this.selectedNode = node;
            listeners.forEach(Listener::selectedNodeChanged);
        }
    }

    /// Signals that the proof tree was modified (e.g. after a rule application or auto mode).
    public void fireProofChanged() {
        listeners.forEach(Listener::proofChanged);
    }
}
