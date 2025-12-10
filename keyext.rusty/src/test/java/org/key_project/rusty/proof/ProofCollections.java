/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import java.io.IOException;
import java.util.Date;

public class ProofCollections {
    public static ProofCollection automaticRustyDL() throws IOException {
        var settings = new ProofCollectionSettings(new Date());

        /*
         * Defines a base directory.
         * All paths in this file are treated relative to base directory (except rustSrc for base
         * directory itself).
         */
        settings.setBaseDirectory("src/test/resources/testcase/examples");

        /*
         * Defines a statistics file.
         * Path is relative to base directory.
         */
        settings.setStatisticsFile("build/reports/runallproofs/runStatistics.csv");

        /*
         * Enable or disable proof reloading.
         * If enabled, closed proofs will be saved and reloaded after prover is finished.
         */
        settings.setReloadEnabled(true);

        /*
         * By default, runAllProofs does not print a lot of information.
         * Set this to true to get more output.
         */
        settings.setVerboseOutput(true);

        var c = new ProofCollection(settings);
        var simple = c.group("simple");
        simple.provable("simple.key");
        simple.loadable("man-simple.proof");
        simple.provable("if.key");
        simple.loadable("man-if.proof");
        simple.provable("iflet.key");
        simple.loadable("man-iflet.proof");
        simple.provable("auto-if.key");

        var refs = c.group("references");
        refs.provable("shared-ref.key");
        refs.loadable("man-shared-ref.proof");
        refs.provable("mutable-ref.key");
        refs.loadable("man-mutable-ref.proof");
        refs.notprovable("mutable-ref-wrong.key");
        refs.loadable("man-mutable-ref-wrong.proof");

        var choices = c.group("choices");
        choices.provable("sub-no-check.key");
        choices.loadable("man-sub-no-check.proof");

        var contracts = c.group("contracts");
        contracts.provable("use-contract.key");
        contracts.loadable("man-use-contract.proof");

        var rustSrc = c.group("rustSrc");
        rustSrc.provable("loop-mul.key");
        rustSrc.loadable("man-loop-mul.proof");
        rustSrc.provable("add-no-bounds.key");
        rustSrc.loadable("man-add-no-bounds.proof");
        rustSrc.provable("mut-ref-src.key");
        rustSrc.loadable("man-mut-ref-src.proof");
        rustSrc.provable("if-src.key");
        rustSrc.loadable("man-if-src.proof");

        var array = c.group("array");
        array.provable("array-get-of-repeat.key");
        array.loadable("man-array-get-of-repeat.proof");
        array.provable("array-get-of-set.key");
        array.loadable("man-array-get-of-set.proof");
        array.provable("array-test.key");
        array.loadable("man-array-test.proof");
        array.provable("array-enumerate.key");
        array.loadable("man-array-enumerate.proof");

        var tuples = c.group("tuples");
        tuples.provable("tuple-test.key");
        tuples.loadable("man-tuple-test.proof");

        var option = c.group("option");
        option.loadable("option.key");

        var algos = c.group("algorithms");
        algos.provable("binary-search/binary-search.key");

        return c;
    }
}
