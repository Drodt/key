/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.settings;

import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Top of the proof independent settings.
///
/// You can add your own settings by calling [#addSettings(Settings)].
///
/// @see Settings
public class ProofIndependentSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProofIndependentSettings.class);
    public static final ProofIndependentSettings DEFAULT_INSTANCE;

    static {
        var write = PathConfig.currentPaths.proofIndependentSettings;
        var read = PathConfig.previousPaths.proofIndependentSettings;

        if (Files.exists(write)) {
            read = write;
        }

        // read from the old/new place
        DEFAULT_INSTANCE = new ProofIndependentSettings(read.toFile());

        // set the write place
        DEFAULT_INSTANCE.filename = write.toFile();
    }

    private File filename;
    private final GeneralSettings generalSettings = new GeneralSettings();
    private final List<Settings> settings = new LinkedList<>();
    private final PropertyChangeListener settingsListener = e -> saveSettings();
    private Configuration lastReadConfiguration;

    private ProofIndependentSettings() {
        addSettings(generalSettings);
    }

    private ProofIndependentSettings(File filename) {
        this();
        this.filename = filename;
        loadSettings();
    }

    // Legacy Properties-based snapshot from a .props/.key load; re-apply to settings added later.
    public void addSettings(Settings settings) {
        if (!this.settings.contains(settings)) {
            this.settings.add(settings);
            if (lastReadConfiguration != null) {
                settings.readSettings(lastReadConfiguration);
            }
        }
    }

    private void loadSettings() {
        try {
            if (filename.exists()) {
                if (PathConfig.DISREGARD_SETTINGS) {
                    LOGGER.warn("The settings in {} are *not* read due to flag '{}'", filename,
                        PathConfig.DISREGARD_SETTINGS_PROPERTY);
                } else {
                    load(filename);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not load settings from {}", filename, e);
        }
    }

    private void load(File file) throws IOException {
        this.lastReadConfiguration = Configuration.load(file.toPath());
        for (Settings settings : settings) {
            settings.readSettings(lastReadConfiguration);
        }
    }

    // Legacy non-JSON branch writes .key/.props files via the deprecated Properties Settings API.
    public void saveSettings() {
        Configuration config = new Configuration();
        if (lastReadConfiguration != null) {
            config.overwriteWith(lastReadConfiguration);
        }

        for (var settings : settings)
            settings.writeSettings(config);
        if (!filename.exists()) {
            filename.getParentFile().mkdirs();
        }

        try (var out = new BufferedWriter(new FileWriter(
            filename.toString().replace(".props", ".json"), StandardCharsets.UTF_8))) {
            config.save(out, "Proof-Independent-Settings-File. Generated " + new Date());
        } catch (IOException e) {
            LOGGER.error("Could not store settings to {}", filename, e);
        }
    }

    public GeneralSettings getGeneralSettings() {
        return generalSettings;
    }
}
