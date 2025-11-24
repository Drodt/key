/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.definition;

import java.util.ArrayList;

import org.key_project.rusty.strategy.StrategyProperties;
import org.key_project.util.collection.ImmutableArray;

///
/// Instances of this class defines how a user interfaces has to look like which edits
/// [StrategySettings].
///
///
/// The [StrategySettingsDefinition] itself and all its provided sub classes are read-only.
///
///
/// Each [StrategyFactory] should provide an instance of this class to define the user
/// interface which the user can use to edit supported [StrategySettings] in created
/// [Strategy] instances. If a [StrategyFactory] provides no
/// [StrategySettingsDefinition] an empty user interface or even bedder an error message should
/// be shown to the user.
///
///
/// @author Martin Hentschel
/// @see StrategyFactory
/// @see AbstractStrategyPropertyDefinition
/// @see OneOfStrategyPropertyDefinition
/// @see StrategyPropertyValueDefinition
public final class StrategySettingsDefinition {
    /// This class represents an attribute in the strategy settings.
    ///
    /// @param name name of the attribute
    /// @param order precedence for sorting
    /// @param factory factory for creating new settings
    public record StrategySettingEntry(String name, int order,
            IDefaultStrategyPropertiesFactory factory) {
    }

    private static final ArrayList<StrategySettingEntry> STD_FURTHER_DEFAULTS;

    /// Defines if a user interface control is shown to edit [#getMaxSteps()].
    private final boolean showMaxRuleApplications;

    /// The label shown in front of the control to edit [#getMaxSteps()].
    private final String maxRuleApplicationsLabel;

    /// The label shown in front of the controls to edit [StrategyProperties].
    private final String propertiesTitle;

    /// Defines the controls to edit [StrategyProperties].
    private final ImmutableArray<AbstractStrategyPropertyDefinition> properties;

    /// The default maximal rule applications.
    private final int defaultMaxRuleApplications;

    /// The [IDefaultStrategyPropertiesFactory] used to create default
    /// [StrategyProperties].
    private final IDefaultStrategyPropertiesFactory defaultPropertiesFactory;

    /// Further default settings, for example suitable for simplification. Consists of triples
    /// (DEFAULT_NAME, MAX_RULE_APPS, PROPERTIES).
    private final ArrayList<StrategySettingEntry> furtherDefaults;

    static {
        STD_FURTHER_DEFAULTS = new ArrayList<>();

        // Rust verification standard preset (tested in TimSort case study)
        STD_FURTHER_DEFAULTS.add(new StrategySettingEntry(
            "Rust verif. std.", 7000, () -> {
                final StrategyProperties newProps =
                    IDefaultStrategyPropertiesFactory.DEFAULT_FACTORY
                            .createDefaultStrategyProperties();

                newProps.setProperty(StrategyProperties.SPLITTING_OPTIONS_KEY,
                    StrategyProperties.SPLITTING_DELAYED);

                newProps.setProperty(StrategyProperties.LOOP_OPTIONS_KEY,
                    StrategyProperties.LOOP_INVARIANT);

                newProps.setProperty(StrategyProperties.FUNCTION_OPTIONS_KEY,
                    StrategyProperties.FUNCTION_CONTRACT);

                newProps.setProperty(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY,
                    StrategyProperties.NON_LIN_ARITH_DEF_OPS);

                newProps.setProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY,
                    StrategyProperties.QUANTIFIERS_NON_SPLITTING_WITH_PROGS);

                return newProps;
            }));

        // Simplification preset
        STD_FURTHER_DEFAULTS.add(new StrategySettingEntry(
            "Simplification", 5000, () -> {
                final StrategyProperties newProps =
                    IDefaultStrategyPropertiesFactory.DEFAULT_FACTORY
                            .createDefaultStrategyProperties();

                newProps.setProperty(StrategyProperties.SPLITTING_OPTIONS_KEY,
                    StrategyProperties.SPLITTING_OFF);

                newProps.setProperty(StrategyProperties.LOOP_OPTIONS_KEY,
                    StrategyProperties.LOOP_NONE);

                newProps.setProperty(StrategyProperties.FUNCTION_OPTIONS_KEY,
                    StrategyProperties.FUNCTION_NONE);

                newProps.setProperty(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY,
                    StrategyProperties.NON_LIN_ARITH_NONE);

                newProps.setProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY,
                    StrategyProperties.QUANTIFIERS_NONE);

                return newProps;
            }));
    }

    /// Constructor.
    ///
    /// @param propertiesTitle The label shown in front of the controls to edit
    /// [StrategyProperties].
    /// @param properties Defines the controls to edit [StrategyProperties].
    public StrategySettingsDefinition(String propertiesTitle,
            AbstractStrategyPropertyDefinition... properties) {
        this(true, "Max. Rule Applications", 10000, propertiesTitle,
            IDefaultStrategyPropertiesFactory.DEFAULT_FACTORY, STD_FURTHER_DEFAULTS, properties);
    }

    /// Constructor.
    ///
    /// @param showMaxRuleApplications Defines if a user interface control is shown to edit
    /// [#getMaxSteps()].
    /// @param maxRuleApplicationsLabel The label shown in front of the control to edit
    /// [#getMaxSteps()].
    /// @param defaultMaxRuleApplications The default maximal rule applications.
    /// @param propertiesTitle The label shown in front of the controls to edit
    /// [StrategyProperties].
    /// @param defaultPropertiesFactory The [IDefaultStrategyPropertiesFactory] used to create
    /// default [StrategyProperties].
    /// @param furtherDefaults further defaults used to create default [StrategyProperties].
    /// @param properties Defines the controls to edit [StrategyProperties].
    public StrategySettingsDefinition(boolean showMaxRuleApplications,
            String maxRuleApplicationsLabel, int defaultMaxRuleApplications, String propertiesTitle,
            IDefaultStrategyPropertiesFactory defaultPropertiesFactory,
            ArrayList<StrategySettingEntry> furtherDefaults,
            AbstractStrategyPropertyDefinition... properties) {
        assert defaultPropertiesFactory != null;
        this.showMaxRuleApplications = showMaxRuleApplications;
        this.maxRuleApplicationsLabel = maxRuleApplicationsLabel;
        this.defaultMaxRuleApplications = defaultMaxRuleApplications;
        this.propertiesTitle = propertiesTitle;
        this.defaultPropertiesFactory = defaultPropertiesFactory;
        this.furtherDefaults = furtherDefaults;
        this.properties = new ImmutableArray<>(properties);
    }

    /// Checks if the user interface control to edit [#getMaxSteps()] should be
    /// shown or not.
    ///
    /// @return `true` show control, `false` do not provide a control.
    public boolean isShowMaxRuleApplications() {
        return showMaxRuleApplications;
    }

    /// Returns the label shown in front of the control to edit
    /// [#getMaxSteps()].
    ///
    /// @return The label shown in front of the control to edit
    /// [#getMaxSteps()] or `null` if no label should be shown.
    public String getMaxRuleApplicationsLabel() {
        return maxRuleApplicationsLabel;
    }

    /// Returns the label shown in front of the controls to edit [StrategyProperties].
    ///
    /// @return The label shown in front of the controls to edit [StrategyProperties] or
    /// `null` if no label should be shown.
    public String getPropertiesTitle() {
        return propertiesTitle;
    }

    /// Returns the definition of controls to edit [StrategyProperties].
    ///
    /// @return The definition of controls to edit [StrategyProperties].
    public ImmutableArray<AbstractStrategyPropertyDefinition> getProperties() {
        return properties;
    }

    /// Returns the default maximal rule applications.
    ///
    /// @return The default maximal rule applications.
    public int getDefaultMaxRuleApplications() {
        return defaultMaxRuleApplications;
    }

    /// Returns the [IDefaultStrategyPropertiesFactory] used to create default
    /// [StrategyProperties].
    ///
    /// @return The [IDefaultStrategyPropertiesFactory] used to create default
    /// [StrategyProperties].
    public IDefaultStrategyPropertiesFactory getDefaultPropertiesFactory() {
        return defaultPropertiesFactory;
    }

    /// @return Further default settings, e.g. for simplification.
    public ArrayList<StrategySettingEntry> getFurtherDefaults() {
        return furtherDefaults;
    }
}
