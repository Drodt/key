/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.settings;

import java.io.File;

import org.jspecify.annotations.Nullable;
import org.key_project.util.java.IOUtil;

///
/// Keeps some central paths to files and directories.
///
///
/// By default all KeY configurations are stored in a directory named ".rusty-key" inside the user's
/// home
/// directory. In Microsoft windows operating systems this is directly the hard disc that contains
/// the KeY code. But the eclipse integration requires to change the default location. This is
/// possible via [#setKeyConfigDir(String)] which should be called once before something is
/// done with KeY (e.g. before the `MainWindow` is opened).
///
public final class PathConfig {
    /// The Java system property used to indicate that the settings in the KeY directory should not
    /// be consulted at startup.
    public static final String DISREGARD_SETTINGS_PROPERTY = "key.disregardSettings";

    /// The default name of the directory that contains KeY settings.
    public static final String KEY_DIRECTORY_NAME = ".rusty-key";

    /// In which file to store the recent files.
    private static @Nullable String recentFileStorage=null;

    /// In which file to store the proof-independent settings.
    private static @Nullable String proofIndependentSettings=null;

    /// directory where to find the KeY configuration files
    private static @Nullable String keyConfigDir=null;

    private PathConfig() {
    }

    /*
     * Initializes the instance variables with the default settings.
     */
    static {
        setKeyConfigDir(IOUtil.getHomeDirectory() + File.separator + KEY_DIRECTORY_NAME);
    }

    /// Returns the path to the directory that contains KeY configuration files.
    ///
    /// @return The directory.
    public static @Nullable String getKeyConfigDir() {
        return keyConfigDir;
    }

    /// Sets the path to the directory that contains KeY configuration files.
    ///
    /// @param keyConfigDir The new directory to use.
    public static void setKeyConfigDir(String keyConfigDir) {
        PathConfig.keyConfigDir = keyConfigDir;
        PathConfig.recentFileStorage = getKeyConfigDir() + File.separator + "recentFiles.json";
        PathConfig.proofIndependentSettings =
            getKeyConfigDir() + File.separator + "proofIndependentSettings.props";
        // PathConfig.logDirectory = new File(keyConfigDir, "logs");
    }

    /// Returns the path to the file that is used to store recent files.
    ///
    /// @return The path to the file.
    public static @Nullable String getRecentFileStorage() {
        return recentFileStorage;
    }

    /// Returns the path to the file that is used to store proof independent settings.
    ///
    /// @return The path to the file.
    public static @Nullable String getProofIndependentSettings() {
        return proofIndependentSettings;
    }
}
