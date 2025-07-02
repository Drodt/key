/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.settings;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.key_project.rusty.util.KeYResourceManager;

import org.antlr.v4.runtime.CharStreams;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.jspecify.annotations.Nullable;

public class ProofSettings {
    public static final ProofSettings DEFAULT_SETTINGS = ProofSettings.loadedSettings();

    public static final File PROVER_CONFIG_FILE_NEW =
        new File(PathConfig.getKeyConfigDir(), "proof-settings.json");

    public static final @Nullable URL PROVER_CONFIG_FILE_TEMPLATE = KeYResourceManager.getManager()
            .getResourceFile(ProofSettings.class, "default-proof-settings.json");

    private @Nullable Properties lastLoadedProperties = null;
    private @Nullable Configuration lastLoadedConfiguration = null;

    /// all setting objects in the following order: heuristicSettings
    private final List<Settings> settings = new LinkedList<>();

    private final ChoiceSettings choiceSettings = new ChoiceSettings();

    /// create a proof settings object. When you add a new settings object, PLEASE UPDATE THE LIST
    /// ABOVE AND USE THOSE CONSTANTS INSTEAD OF USING INTEGERS DIRECTLY
    private ProofSettings() {
        addSettings(choiceSettings);
    }

    /*
     * copy constructor - substitutes .clone() in classes implementing Settings
     */
    public ProofSettings(ProofSettings toCopy) {
        this();
        var result = new Configuration();
        lastLoadedProperties = toCopy.lastLoadedProperties;
        lastLoadedConfiguration = toCopy.lastLoadedConfiguration;
        for (Settings s : toCopy.settings) {
            s.writeSettings(result);
        }
        for (Settings s : settings) {
            s.readSettings(result);
        }
    }

    public void addSettings(@UnknownInitialization ProofSettings this, Settings settings) {
        assert settings != null;
        this.settings.add(settings);
        if (lastLoadedConfiguration != null) {
            settings.readSettings(lastLoadedConfiguration);
        }
    }

    private static ProofSettings loadedSettings() {
        ProofSettings ps = new ProofSettings();
        ps.loadSettings();
        return ps;
    }

    /// Loads the former settings from configuration file.
    public void loadSettings() {
        if (Boolean.getBoolean(PathConfig.DISREGARD_SETTINGS_PROPERTY)) {
            // LOGGER.warn("The settings in {} are *not* read.", PROVER_CONFIG_FILE);
        } else {
            var isOldFormat = false;
            try (var in = new BufferedReader(
                new FileReader(PROVER_CONFIG_FILE_NEW, StandardCharsets.UTF_8))) {
                // LOGGER.info("Load proof dependent settings from file {}", fileToUse);
                loadDefaultJSONSettings();
                loadSettingsFromJSONStream(in);
            } catch (IOException e) {
                // LOGGER.warn("No proof-settings could be loaded, using defaults", e);
            }
        }
    }

    public void loadSettingsFromJSONStream(Reader in) throws IOException {
        var config = Configuration.load(CharStreams.fromReader(in));
        readSettings(config);
    }

    public void loadDefaultJSONSettings() {
        if (PROVER_CONFIG_FILE_TEMPLATE == null) {
            // LOGGER.warn(
            // "default proof-settings file 'default-proof-settings.json' could not be found.");
        } else {
            try (var in = new InputStreamReader(PROVER_CONFIG_FILE_TEMPLATE.openStream())) {
                loadSettingsFromJSONStream(in);
            } catch (IOException e) {
                // LOGGER.error("Default proof-settings could not be loaded.", e);
            }
        }
    }

    public void readSettings(Configuration c) {
        lastLoadedProperties = null;
        lastLoadedConfiguration = c;
        for (Settings setting : settings)
            setting.readSettings(c);
    }

    /// returns the ChoiceSettings object
    ///
    /// @return the ChoiceSettings object
    public ChoiceSettings getChoiceSettings() {
        return choiceSettings;
    }
}
