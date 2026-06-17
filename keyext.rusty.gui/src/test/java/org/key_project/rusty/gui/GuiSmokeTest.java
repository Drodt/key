/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.rusty.control.KeYEnvironment;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.io.OutputStreamProofSaver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Headless smoke test: load an example, wire it through the [ProofContext] and the views, run auto
/// mode and check the model/views update without error. Top-level windows are not created (they
/// need
/// a display), but the panels are exercised.
public class GuiSmokeTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static File example() {
        Path p = Path.of("keyext.rusty/src/test/resources/testcase/examples/if.key");
        Path q = Files.exists(p) ? p
                : Path.of("../keyext.rusty/src/test/resources/testcase/examples/if.key");
        return q.toFile();
    }

    @Test
    void loadsWiresAndRefreshesViews() throws Exception {
        File file = example();
        assertTrue(file.exists(), "example must exist: " + file.getAbsolutePath());

        KeYEnvironment<?> env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        assertNotNull(proof);

        ProofContext context = new ProofContext();
        // Construct the views (registers them as listeners); panels are headless-safe.
        ProofTreePanel tree = new ProofTreePanel(context);
        GoalsView goals = new GoalsView(context);
        SequentView sequent = new SequentView(context);
        NodeInfoView info = new NodeInfoView(context);
        EditorArea editor = new EditorArea(context, () -> {
        });
        StrategyView strategy = new StrategyView(context);
        assertNotNull(tree);
        assertNotNull(goals);
        assertNotNull(sequent);
        assertNotNull(info);
        assertNotNull(editor);
        assertNotNull(strategy);

        context.setProof(env, proof);
        assertTrue(context.getSelectedNode() == proof.root(), "root should be selected on load");
        assertFalse(proof.openGoals().isEmpty(), "freshly loaded proof should have an open goal");

        // The selected node renders to a non-empty sequent.
        Node root = proof.root();
        String rendered =
            OutputStreamProofSaver.printSequent(root.sequent(), root.proof().getServices());
        assertTrue(rendered.contains("=") || rendered.contains("\\<"),
            "root sequent should render: " + rendered);

        // Run auto mode and refresh; it should apply at least one rule (the proof grows).
        env.getProofControl().startAndWaitForAutoMode(proof);
        context.fireProofChanged();
        assertTrue(proof.countNodes() > 1, "auto mode should apply rules");

        env.dispose();
    }
}
