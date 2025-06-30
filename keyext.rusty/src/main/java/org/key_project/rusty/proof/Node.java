/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import java.util.*;

import org.key_project.logic.op.Function;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.Sequent;
import org.key_project.rusty.logic.RenamingTable;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.proof.calculus.RustySequentKit;
import org.key_project.rusty.rule.NoPosTacletApp;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.Nullable;

public class Node implements Iterable<Node> {
    private static final String NODES = "nodes";

    /// the proof the node belongs to
    private final Proof proof;

    /// The parent node.
    private @Nullable Node parent = null;

    private Sequent seq = RustySequentKit.getInstance().getEmptySequent();

    private final ArrayList<Node> children = new ArrayList<>(1);

    private boolean closed = false;

    private RuleApp appliedRuleApp;

    /// Serial number of this proof node.
    /// For each proof, serial numbers are assigned to nodes as they are created:
    /// the first step is assigned number 0, the next step number 1, and so on.
    private final int serialNr;

    /// Sibling number of this proof node.
    /// If the [#parent()] proof node has more than one child node,
    /// each child node receives an index (starting at 0, incrementing by 1 for each sibling).
    private int siblingNr = -1;

    /// If the rule base has been extended e.g. by loading a new taclet as lemma or by applying a
    /// taclet with an addrule section on this node, then these taclets are stored in this list
    private ImmutableSet<NoPosTacletApp> localIntroducedRules =
        DefaultImmutableSet.nil();
    private ImmutableList<RenamingTable> renamings;

    /// a linked list of the locally generated program variables. It extends the list of the parent
    /// node.
    private ImmutableList<ProgramVariable> localProgVars = ImmutableSLList.nil();

    /// a linked list of the locally generated function symbols. It extends the list of the parent
    /// node.
    private ImmutableList<Function> localFunctions = ImmutableSLList.nil();

    private NameRecorder nameRecorder;

    /// creates an empty node that is root and leaf.
    private Node(Proof proof) {
        this.proof = proof;
        serialNr = proof.getServices().getCounter(NODES).getCountPlusPlus();
    }

    /// creates a node with the given contents and associated proof
    public Node(Proof proof, Sequent seq) {
        this(proof);
        this.seq = seq;
    }

    /// creates a node with the given contents, the given collection of children (all elements must
    /// be of class Node) and the given parent node.
    public Node(Proof proof, Sequent seq, @Nullable Node parent) {
        this(proof, seq);
        this.parent = parent;
        if (parent != null) {
            localProgVars = parent.localProgVars;
            localFunctions = parent.localFunctions;
        }
    }

    /// sets the sequent at this node
    public void setSequent(Sequent seq) {
        this.seq = seq;
    }

    /// returns the sequent of this node
    public Sequent sequent() {
        return seq;
    }

    /// @return the parent node of this node.
    public @Nullable Node parent() {
        return parent;
    }

    public void setAppliedRuleApp(RuleApp ruleApp) {
        // this.nodeInfo.updateNoteInfo();
        this.appliedRuleApp = ruleApp;
        // clearNameCache();
    }

    public Proof proof() {
        return proof;
    }

    public int getSerialNr() {
        return serialNr;
    }

    /// Makes the given node a child of this node.
    ///
    /// @param newChild the node to make a child of this node.
    public void add(Node newChild) {
        newChild.siblingNr = children.size();
        children.add(newChild);
        newChild.parent = this;
    }

    /// Makes the given node children of this node.
    ///
    /// @param newChildren the node to make into children of this node.
    public void addAll(Node[] newChildren) {
        final int size = children.size();
        for (int i = 0; i < newChildren.length; i++) {
            newChildren[i].siblingNr = i + size;
            newChildren[i].parent = this;
        }

        Collections.addAll(children, newChildren);
        children.trimToSize();
    }

    /// @param i an index (starting at 0).
    /// @return the i-th child of this node.
    public Node child(int i) {
        return children.get(i);
    }

    /// @param child a child of this node.
    /// @return the number of the node <code>child</code>, if it is a child of this node (starting
    /// with <code>0</code>), <code>-1</code> otherwise
    public int getChildNr(Node child) {
        int res = 0;
        final Iterator<Node> it = childrenIterator();

        while (it.hasNext()) {
            if (it.next() == child) {
                return res;
            }
            ++res;
        }

        return -1;
    }

    public StringBuffer getUniqueTacletId() {
        StringBuffer id = new StringBuffer();
        int c = 0;
        Node n = this;

        while (n != null) {
            // c += n.localIntroducedRules.size();

            if (n.parent != null && n.parent.childrenCount() > 1) {
                id.append(n.siblingNr);
            }

            n = n.parent;
        }

        id.append("_").append(c);

        return id;
    }

    /// @return number of children
    public int childrenCount() {
        return children.size();
    }

    public RuleApp getAppliedRuleApp() {
        return appliedRuleApp;
    }

    /// adds a new NoPosTacletApp to the set of available NoPosTacletApps at this node
    ///
    /// @param s the app to add.
    public void addNoPosTacletApp(NoPosTacletApp s) {
        localIntroducedRules = localIntroducedRules.add(s);
    }

    /// marks a node as closed
    Node close() {
        closed = true;
        Node tmp = parent;
        Node result = this;
        while (tmp != null && tmp.isCloseable()) {
            tmp.closed = true;
            result = tmp;
            tmp = tmp.parent();
        }
        // clearNameCache();
        return result;
    }

    /// @return true iff this inner node is closeable
    private boolean isCloseable() {
        assert childrenCount() > 0;
        for (Node child : children) {
            if (!child.isClosed()) {
                return false;
            }
        }
        return true;
    }

    public boolean isClosed() {
        return closed;
    }

    /// Returns the set of created program variables known in this node.
    ///
    /// In the resulting list, the newest additions come first.
    ///
    /// @return a non-null immutable list of program variables.
    public ImmutableList<ProgramVariable> getLocalProgVars() {
        return localProgVars;
    }

    public void addLocalProgVars(Iterable<? extends ProgramVariable> elements) {
        for (var pv : elements) {
            localProgVars = localProgVars.prepend(pv);
        }
    }

    public void addLocalFunctions(Collection<? extends Function> elements) {
        for (Function op : elements) {
            localFunctions = localFunctions.prepend(op);
        }
    }

    /// Returns the set of freshly created function symbols known to this node.
    ///
    /// In the resulting list, the newest additions come first.
    ///
    /// @return a non-null immutable list of function symbols.
    public Iterable<Function> getLocalFunctions() {
        return localFunctions;
    }

    /// @return an iterator for the direct children of this node.
    public Iterator<Node> childrenIterator() {
        return new NodeIterator(children.iterator());
    }

    /// @return an iterator for all nodes in the subtree (including this node).
    public Iterator<Node> subtreeIterator() {
        return new SubtreeIterator(this);
    }

    /// Returns an iterator over this node's children. Use [#leavesIterator()] if you need to
    /// iterate over leaves instead.
    ///
    /// @return iterator over children.
    public Iterator<Node> iterator() {
        return childrenIterator();
    }

    /// Computes the leaves of the current subtree and returns them.
    ///
    /// @return the leaves of the current subtree.
    List<Node> getLeaves() {
        final List<Node> leaves = new LinkedList<>();
        final LinkedList<Node> nodesToCheck = new LinkedList<>();
        nodesToCheck.add(this);
        do {
            final Node n = nodesToCheck.poll();
            if (n.leaf()) {
                leaves.add(n);
            } else {
                nodesToCheck.addAll(0, n.children);
            }
        } while (!nodesToCheck.isEmpty());
        return leaves;
    }

    public boolean leaf() {
        return children.isEmpty();
    }

    /// @return an iterator for the leaves of the subtree below this node. The computation is called
    /// at every call!
    public Iterator<Node> leavesIterator() {
        return new NodeIterator(getLeaves().iterator());
    }

    /// @return number of nodes in the subtree below this node.
    public int countNodes() {
        Iterator<Node> it = subtreeIterator();
        int res = 0;
        for (; it.hasNext(); it.next()) {
            res++;
        }
        return res;
    }

    public void setRenamings(ImmutableList<RenamingTable> list) {
        renamings = list;
    }

    public NameRecorder getNameRecorder() {
        return nameRecorder;
    }

    public void setNameRecorder(NameRecorder nameRecorder) {
        this.nameRecorder = nameRecorder;
    }
}
