/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.control;

import org.key_project.rusty.proof.ProofEvent;

public interface AutoModeListener {
    /// invoked if automatic execution has started
    void autoModeStarted(ProofEvent e);

    /// invoked if automatic execution has stopped
    void autoModeStopped(ProofEvent e);
}
