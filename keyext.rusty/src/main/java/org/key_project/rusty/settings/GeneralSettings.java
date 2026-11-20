/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.settings;

import org.jspecify.annotations.NonNull;

public class GeneralSettings extends AbstractSettings {
    /// Whether automatic proof search uses the multicore (parallel) prover.
    public static final String PARALLEL_PROVER_ENABLED = "ParallelProverEnabled";
    /// The number of worker threads the multicore prover uses.
    public static final String PARALLEL_PROVER_THREADS = "ParallelProverThreadCount";

    /// Default worker count when the multicore prover is first enabled.
    public static final int PARALLEL_PROVER_THREADS_DEFAULT = 4;

    /// Whether automatic proof search uses the multicore (parallel) prover instead of the legacy
    /// single-threaded one. Single-core is the safe fallback and keeps the single-core-only
    /// features
    /// (proof caching, slicing, merge rule, ...) available.
    ///
    /// Single-core by default: the multicore prover is opt-in (enable it in the settings, or via
    /// `-Dkey.prover.parallel=true`). It has to stay disabled by default so proofs built with
    /// the single-core-only features keep working out of the box.
    private boolean parallelProverEnabled = false;

    /// Number of worker threads for the multicore prover. Only relevant when
    /// [#parallelProverEnabled] is set; the effective count is clamped to the available
    /// processors.
    private int parallelProverThreadCount = PARALLEL_PROVER_THREADS_DEFAULT;

    GeneralSettings() {
        // addSettingsListener(AutoSaver.settingsListener);
    }

    /// @return whether automatic proof search uses the multicore (parallel) prover
    public boolean isParallelProverEnabled() {
        return parallelProverEnabled;
    }

    /// @return the configured number of worker threads for the multicore prover (not yet clamped to
    /// the available processors)
    public int getParallelProverThreadCount() {
        return parallelProverThreadCount;
    }

    public void setParallelProverEnabled(boolean b) {
        var old = parallelProverEnabled;
        parallelProverEnabled = b;
        firePropertyChange(PARALLEL_PROVER_ENABLED, old, parallelProverEnabled);
    }

    public void setParallelProverThreadCount(int count) {
        var old = parallelProverThreadCount;
        parallelProverThreadCount = Math.max(1, count);
        firePropertyChange(PARALLEL_PROVER_THREADS, old, parallelProverThreadCount);
    }

    @Override
    public void writeSettings(@NonNull Configuration props) {
        props.set(PARALLEL_PROVER_ENABLED, parallelProverEnabled);
        props.set(PARALLEL_PROVER_THREADS, parallelProverThreadCount);
    }

    @Override
    public void readSettings(@NonNull Configuration props) {
        setParallelProverEnabled(props.getBool(PARALLEL_PROVER_ENABLED, false));
        setParallelProverThreadCount(
            props.getInt(PARALLEL_PROVER_THREADS, PARALLEL_PROVER_THREADS_DEFAULT));
    }
}
