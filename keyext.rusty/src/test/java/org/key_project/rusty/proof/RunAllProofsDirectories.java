/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.util.helper.FindResources;

public class RunAllProofsDirectories {
    public static final Path RUNALLPROOFS_DIR = FindResources.getTestResultForRunAllProofs();

    public RunAllProofsDirectories() throws IOException {
        init();
    }

    public static void init() throws IOException {
        Files.createDirectories(RUNALLPROOFS_DIR);
    }
}
