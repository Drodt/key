/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import de.uka.ilkd.key.control.AutoModeListener;
import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.core.KeYSelectionEvent;
import de.uka.ilkd.key.core.KeYSelectionListener;
import de.uka.ilkd.key.gui.configuration.Config;
import de.uka.ilkd.key.gui.extension.api.DefaultContextMenuKind;
import de.uka.ilkd.key.gui.extension.impl.KeYGuiExtensionFacade;
import de.uka.ilkd.key.gui.fonticons.IconFactory;
import de.uka.ilkd.key.gui.notification.events.AbandonTaskEvent;
import de.uka.ilkd.key.proof.*;
import de.uka.ilkd.key.proof.mgt.BasicTask;
import de.uka.ilkd.key.proof.mgt.EnvNode;
import de.uka.ilkd.key.proof.mgt.ProofEnvironment;
import de.uka.ilkd.key.proof.mgt.ProofStatus;
import de.uka.ilkd.key.proof.mgt.TaskTreeModel;
import de.uka.ilkd.key.proof.mgt.TaskTreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task tree panel, showing all currently opened proofs.
 * Usually located in the top left panel.
 */
public class TaskTree extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskTree.class);

    private final JTree delegateView;

    /** the KeYMediator */
    private final KeYMediator mediator;

    /** listener for mouse events of this gui component */
    private final MouseListener mouseListener = new TaskTreeMouseListener();

    /** listener to the prof tree events */
    private final ProofTreeListener proofTreeListener = new TaskTreeProofTreeListener();

    private final TreeSelectionListener selectionListener = new TreeSelectionListener() {
        /**
         * listen to changes in the delegateView {@link JTree} selections and initiate switch
         * between proofs if necessary
         *
         * @param e the event that characterizes the change.
         */
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (e.getSource() == delegateView) {
                problemChosen();
            }
        }
    };

    /** the list model to be used */
    private final TaskTreeModel model = new TaskTreeModel();

    public TaskTree(KeYMediator mediator) {
        super();
        this.mediator = mediator;
        mediator.addKeYSelectionListener(new TaskTreeSelectionListener());
        mediator.getUI().getProofControl().addAutoModeListener(new AutoModeListener() {
            // unregister completely when automode starts
            // reduce unnecessary event propagation and handling when in automode
            @Override
            public void autoModeStopped(ProofEvent e) {
                if (e.getSource() != null) { // == null can happen after problem loading
                    e.getSource().addProofTreeListener(proofTreeListener);
                }
            }

            @Override
            public void autoModeStarted(ProofEvent e) {
                if (e.getSource() != null) { // == null can happen after problem loading
                    e.getSource().removeProofTreeListener(proofTreeListener);
                }
            }
        });

        delegateView = new JTree();
        delegateView.setModel(model);
        delegateView.setCellRenderer(new TaskTreeIconCellRenderer());
        delegateView.addMouseListener(mouseListener);
        delegateView.addTreeSelectionListener(selectionListener);
        this.setLayout(new BorderLayout());
        this.add(delegateView, BorderLayout.CENTER);
        delegateView.setShowsRootHandles(false);
        delegateView.setRootVisible(false);
        delegateView.putClientProperty("JTree.lineStyle", "Horizontal");
        ToolTipManager.sharedInstance().registerComponent(delegateView);
    }

    public synchronized void addProof(de.uka.ilkd.key.proof.ProofAggregate plist) {
        TaskTreeNode bp = model.addProof(plist);
        Proof[] proofs = plist.getProofs();
        for (Proof proof : proofs) {
            proof.addProofTreeListener(proofTreeListener);
            mediator.getCurrentlyOpenedProofs().add(proof);
        }
        delegateView.validate();
        delegateView.scrollPathToVisible(new TreePath(bp.getPath()));
        delegateView.setVisible(true);
        setVisible(true);
    }

    public void removeTask(Proof p) {
        TaskTreeNode taskForProof = model.getTaskForProof(p);
        if (taskForProof instanceof BasicTask) {
            taskForProof = ((BasicTask) taskForProof).getRootTask();
        }
        removeTask(taskForProof);
    }

    public synchronized void removeTask(TaskTreeNode tn) {
        model.removeTask(tn);
        mediator.notify(new AbandonTaskEvent());
        for (int i = 0; i < tn.allProofs().length; i++) {
            tn.allProofs()[i].removeProofTreeListener(proofTreeListener);
            tn.allProofs()[i].mgt().removeProofListener();
        }
        MainWindow.getInstance().getProofTreeView().removeProofs(tn.allProofs());
        // go to some other node, take the last leaf.
        TreePath path = delegateView.getPathForRow(delegateView.getRowCount() - 1);

        if (path != null) {
            TaskTreeNode tn0 = (TaskTreeNode) path.getLastPathComponent();
            mediator.getSelectionModel().setSelectedProof(tn0.proof());
        } else {
            mediator.getSelectionModel().setSelectedProof(null);
        }
    }

    public void updateUI() {
        super.updateUI();
        Font myFont = UIManager.getFont(Config.KEY_FONT_PROOF_LIST_VIEW);
        if (myFont != null) {
            setFont(myFont);
        } else {
            LOGGER.debug(Config.KEY_FONT_PROOF_LIST_VIEW + " not available, use standard font.");
        }

        if (delegateView != null) {
            delegateView.setCellRenderer(new TaskTreeIconCellRenderer());
        }
    }

    /**
     * returns the first selected task which is instance of a task tree node
     */
    public TaskTreeNode getSelectedTask() {
        TreePath path = delegateView.getSelectionModel().getSelectionPath();
        if (path != null && path.getLastPathComponent() instanceof TaskTreeNode) {
            return (TaskTreeNode) path.getLastPathComponent();
        } else {
            return null;
        }
    }

    /** returns all selected basic tasks */
    public BasicTask[] getAllSelectedBasicTasks() {
        TreePath[] paths = delegateView.getSelectionModel().getSelectionPaths();
        if (paths == null) {
            return new BasicTask[0];
        }
        final List<BasicTask> result = new LinkedList<>();
        for (TreePath path : paths) {
            if (path.getLastPathComponent() instanceof BasicTask) {
                result.add((BasicTask) path.getLastPathComponent());
            }
        }
        return result.toArray(new BasicTask[0]);
    }

    /** called when the user has clicked on a problem */
    private void problemChosen() {
        TaskTreeNode prob = getSelectedTask();
        if (prob != null && prob.proof() != null && mediator != null &&
                mediator.getSelectedProof() != prob.proof()) {
            mediator.getSelectionModel().setSelectedProof(prob.proof());
        }
    }

    /**
     * <p>
     * Checks if the given {@link Proof} is contained in the model.
     * </p>
     * <p>
     * This functionality is required for instance for the symbolic execution debugger to check if a
     * {@link Proof} is still available in KeY's {@link MainWindow} or not, because if it was
     * removed the auto mode is no longer available.
     * </p>
     *
     * @param proof The {@link Proof} to check.
     * @return {@code true} proof is available in model, {@code false} proof is not available in
     *         model.
     */
    public synchronized boolean containsProof(Proof proof) {
        boolean contains = false;
        int i = 0;
        while (!contains && i < model.getChildCount(model.getRoot())) {
            Object rootChild = model.getChild(model.getRoot(), i);
            if (rootChild instanceof EnvNode envNode) {
                int j = 0;
                while (!contains && j < envNode.getChildCount()) {
                    Object envChild = envNode.getChildAt(j);
                    if (envChild instanceof TaskTreeNode taskChild) {
                        contains = taskChild.proof() == proof;
                    }
                    j++;
                }
            }
            i++;
        }
        return contains;
    }

    /**
     * Removes the given proof from the model.
     *
     * @param proof The proof to remove.
     */
    public synchronized void removeProof(Proof proof) {
        if (proof != null) {
            ProofEnvironment env = proof.getEnv();
            // Search EnvNode which contains the environment of the given proof.
            EnvNode envNode = null;
            for (int i = 0; i < model.getChildCount(model.getRoot()); i++) {
                Object child = model.getChild(model.getRoot(), i);
                if (child instanceof EnvNode envChild) {
                    if (env != null ? env.equals(envChild.getProofEnv())
                            : envChild.getProofEnv() == null) {
                        envNode = envChild;
                    }
                }
            }
            // Remove proof from found environment node.
            if (envNode != null) {
                for (int i = 0; i < envNode.getChildCount(); i++) {
                    Object child = envNode.getChildAt(i);
                    if (child instanceof TaskTreeNode taskChild) {
                        if (taskChild.proof() == proof) {
                            removeTask(taskChild);
                        }
                    }
                }
            }
        }
    }


    /**
     * inner class implementing the mouse listener that is responsible for this gui component
     */
    class TaskTreeMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            checkPopup(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            checkPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            checkPopup(e);
        }

        /**
         * Checks whether the popup menu should be shown and does so if necessary.
         * <br>
         * <b>Important:</b><br>
         * For the platform specific popup trigger to work, we need to check the popup in pressed,
         * released, and clicked event. For example, on Windows the e.isPopupTrigger() information
         * is only available in the released event.
         *
         * @param e the mouse event that may create the popup
         */
        private void checkPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                TreePath selPath = delegateView.getPathForLocation(e.getX(), e.getY());
                if (selPath != null && selPath.getLastPathComponent() instanceof BasicTask task) {
                    Proof p = task.proof();
                    delegateView.setSelectionPath(selPath);
                    JPopupMenu menu = KeYGuiExtensionFacade.createContextMenu(
                        DefaultContextMenuKind.PROOF_LIST, p, mediator);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }


    /**
     * a prooftree listener, so that it is known when the proof has closed
     */
    class TaskTreeProofTreeListener implements ProofTreeListener {

        /**
         * invoked if all goals of the proof are closed
         */
        @Override
        public void proofClosed(ProofTreeEvent e) {
            delegateView.repaint();
        }

        /**
         * invoked if a proof has been pruned, potentially reopening branches
         */
        @Override
        public void proofPruned(ProofTreeEvent e) {
            delegateView.repaint();
        }

        /**
         * The structure of the proof has changed radically. Any client should rescan the whole
         * proof
         * tree.
         */
        @Override
        public void proofStructureChanged(ProofTreeEvent e) {
            delegateView.repaint();
        }
    } // end of prooftreelistener


    private static final class TaskTreeIconCellRenderer extends DefaultTreeCellRenderer {
        private static final Icon KEY_ICON = IconFactory.keyHole(20, 20);
        private static final Icon KEY_CLOSED_ICON = IconFactory.keyHoleClosed(20);
        private static final Icon KEY_ALMOST_CLOSED_ICON = IconFactory.keyHoleAlmostClosed(20, 20);
        private static final Icon KEY_CACHED_CLOSED_ICON = IconFactory.keyCachedClosed(20, 20);


        private TaskTreeIconCellRenderer() {
        }

        @Override
        public Component getTreeCellRendererComponent(JTree list, Object value, boolean selected,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object newValue;
            if (value instanceof TaskTreeNode) {
                newValue = ((TaskTreeNode) value).shortDescr();
            } else {
                newValue = value;
            }
            super.getTreeCellRendererComponent(list, newValue, selected, expanded, leaf, row,
                hasFocus);
            setIcon(null);
            if (value instanceof TaskTreeNode) {
                ProofStatus ps = ((TaskTreeNode) value).getStatus();
                if (ps != null) {
                    if (ps.getProofClosedButLemmasLeft()) {
                        setIcon(KEY_ALMOST_CLOSED_ICON);
                        setToolTipText("Closed proof (depends on other contracts)");
                    }
                    if (ps.getProofClosedByCache()) {
                        setIcon(KEY_CACHED_CLOSED_ICON);
                        setToolTipText("Closed proof (using proof cache)");
                    }
                    if (ps.getProofClosed()) {
                        setIcon(KEY_CLOSED_ICON);
                        setToolTipText("Closed proof");
                    }
                    if (ps.getProofOpen()) {
                        setIcon(KEY_ICON);
                        setToolTipText("Open proof");
                    }
                }

            }
            return this;
        }
    }


    class TaskTreeSelectionListener implements KeYSelectionListener {
        /** focused node has changed */
        public void selectedNodeChanged(KeYSelectionEvent<Node> e) {
            // empty
        }

        /**
         * the selected proof has changed (e.g. a new proof has been loaded)
         */
        public void selectedProofChanged(KeYSelectionEvent<Proof> e) {
            if (e.getSource().getSelectedProof() == null) {
                return;
            }
            TaskTreeNode ttn = model.getTaskForProof(e.getSource().getSelectedProof());
            delegateView.setSelectionPath(new TreePath(ttn.getPath()));
            validate();
        }

    }

    /**
     * Returns the shown {@link TaskTreeModel}.
     *
     * @return The shown {@link TaskTreeModel}.
     */
    public TaskTreeModel getModel() {
        return model;
    }
} // end of TaskTree
