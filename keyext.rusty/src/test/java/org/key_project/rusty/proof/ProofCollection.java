/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public final class ProofCollection {
    private final List<ProofCollectionUnit> units = new LinkedList<>();
    private final ProofCollectionSettings settings;

    public ProofCollection(ProofCollectionSettings settings) {
        this.settings = settings;
    }

    public ProofCollectionSettings getSettings() {
        return settings;
    }

    public GroupedProofCollectionUnit group(String name) {
        var settings = new ProofCollectionSettings(this.settings);
        var unit = new GroupedProofCollectionUnit(name, settings);
        units.add(unit);
        return unit;
    }

    /**
     * Create list of {@link RunAllProofsTestUnit}s from list of
     * {@link ProofCollectionUnit}s.
     *
     * @return A list of {@link RunAllProofsTestUnit}s.
     * @throws IOException Names of {@link SingletonProofCollectionUnit}s are
     *         determined by their
     *         corresponding file names. In case file name can't be read
     *         {@link IOException} may be
     *         thrown.
     */
    public List<RunAllProofsTestUnit> createRunAllProofsTestUnits() throws IOException {
        List<String> activeGroups = settings.getRunOnlyOn();

        List<RunAllProofsTestUnit> ret = new LinkedList<>();
        Set<String> testCaseNames = new LinkedHashSet<>();
        for (ProofCollectionUnit proofCollectionUnit : units) {
            if (activeGroups != null && !activeGroups.contains(proofCollectionUnit.getName())) {
                continue;
            }

            final String proposedTestCaseName = proofCollectionUnit.getName();
            String testCaseName = proposedTestCaseName;
            int counter = 0;
            while (testCaseNames.contains(testCaseName)) {
                counter++;
                testCaseName = proposedTestCaseName + "#" + counter;
            }
            testCaseNames.add(testCaseName);

            RunAllProofsTestUnit testUnit =
                proofCollectionUnit.createRunAllProofsTestUnit(testCaseName);
            ret.add(testUnit);
        }

        // Set<String> enabledTestCaseNames = settings.getEnabledTestCaseNames();
        // if (enabledTestCaseNames != null) {
        // ret.removeIf(unit -> !enabledTestCaseNames.contains(unit.getTestName()));
        // }
        return ret;
    }
}