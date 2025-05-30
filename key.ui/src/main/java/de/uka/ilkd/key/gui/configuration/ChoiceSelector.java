/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.gui.configuration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;

import de.uka.ilkd.key.gui.fonticons.IconFactory;
import de.uka.ilkd.key.gui.utilities.GuiUtilities;
import de.uka.ilkd.key.settings.ChoiceSettings;

import org.key_project.util.java.ArrayUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChoiceSelector extends JDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceSelector.class);
    private static final long serialVersionUID = -4470713015801365801L;
    private static final String EXPLANATIONS_RESOURCE =
        "/de/uka/ilkd/key/gui/help/choiceExplanations.xml";
    private final ChoiceSettings settings;
    private final Map<String, String> category2DefaultChoice;
    private Map<String, Set<String>> category2Choices;
    private boolean changed = false;


    /** the JList with the categories of choices */
    private JList<String> catList;
    /** the JList with the choices for one category */
    private JList<ChoiceEntry> choiceList;
    private JTextArea explanationArea;
    private static Properties explanationMap;

    /**
     * Creates a new dialog for choosing taclet options.
     *
     * @param mainWindow the parent window (dialog is centered on this)
     * @param settings the currently selected settings
     */
    public ChoiceSelector(JFrame mainWindow, ChoiceSettings settings) {
        super(mainWindow, "Taclet Base Configuration", true);
        this.settings = settings;
        category2DefaultChoice = new HashMap<>(settings.getDefaultChoices());
        if (category2DefaultChoice.isEmpty()) {
            JOptionPane.showConfirmDialog(ChoiceSelector.this,
                "There are no Taclet Options available as the rule-files have not been parsed yet!",
                "No Options available", JOptionPane.DEFAULT_OPTION);
            dispose();
        } else {
            category2Choices = settings.getChoices();
            layoutChoiceSelector();
            setChoiceList();
            pack();
            setLocationRelativeTo(mainWindow);
            setVisible(true);
        }
    }

    /** layout */
    protected void layoutChoiceSelector() {
        setIconImage(IconFactory.keyLogo());
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BorderLayout());
        String[] cats = category2DefaultChoice.keySet().toArray(new String[0]);
        Arrays.sort(cats);
        {
            catList = new JList<>(cats);
            catList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            catList.setSelectedIndex(0);
            catList.addListSelectionListener(e -> setChoiceList());
            JScrollPane catListScroll =
                new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            catListScroll.setBorder(new TitledBorder("Category"));
            catListScroll.getViewport().setView(catList);
            Dimension paneDim = new Dimension(200, 300);
            catListScroll.setPreferredSize(paneDim);
            catListScroll.setMinimumSize(paneDim);
            listPanel.add(catListScroll, BorderLayout.WEST);
        }
        {
            choiceList = new JList<>();
            choiceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            choiceList.setSelectedValue(category2DefaultChoice.get(cats[0]), true);
            choiceList.addListSelectionListener(e -> {
                ChoiceEntry selectedValue = choiceList.getSelectedValue();
                if (selectedValue != null) {
                    setDefaultChoice(selectedValue.choice());

                } else {
                    setDefaultChoice(null);
                }
            });

            JScrollPane choiceScrollPane =
                new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            choiceScrollPane.getViewport().setView(choiceList);
            choiceScrollPane.setBorder(new TitledBorder("Choice"));
            Dimension paneDim = new Dimension(300, 300);
            choiceScrollPane.setPreferredSize(paneDim);
            choiceScrollPane.setMinimumSize(paneDim);
            listPanel.add(choiceScrollPane, BorderLayout.EAST);
        }
        {
            explanationArea = new JTextArea("Explanation!");
            explanationArea.setEditable(false);
            explanationArea.setLineWrap(true);
            explanationArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(explanationArea);
            Dimension paneDim = new Dimension(500, 200);
            scrollPane.setPreferredSize(paneDim);
            scrollPane.setMinimumSize(paneDim);
            listPanel.add(scrollPane, BorderLayout.SOUTH);
        }
        JPanel buttonPanel = new JPanel();
        {
            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> {
                if (changed) {
                    int res = JOptionPane.showOptionDialog(ChoiceSelector.this,
                        "Your changes will become effective when "
                            + "the next problem is loaded.\n",
                        "Taclet Options", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, new Object[] { "OK", "Cancel" }, "OK");
                    if (res == 0) {
                        settings.setDefaultChoices(category2DefaultChoice);
                    }
                }
                setVisible(false);
                dispose();
            });
            buttonPanel.add(okButton);
            getRootPane().setDefaultButton(okButton);
        }
        {
            final JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> {
                setVisible(false);
                dispose();
            });
            GuiUtilities.attachClickOnEscListener(cancelButton);
            buttonPanel.add(cancelButton);
        }

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(listPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        setResizable(false);
    }


    /**
     * is called to set the selected choice in <code>category2DefaultChoice</code>
     */
    private void setDefaultChoice(String sel) {
        String category = catList.getSelectedValue();
        if (sel != null) {
            category2DefaultChoice.put(category, sel);
            changed = true;
        }
    }

    /**
     * is called if the selection of left list has changed, and causes the right one to display the
     * possible choices for the category chosen on the left side
     */
    private void setChoiceList() {
        String selection = catList.getSelectedValue();
        ChoiceEntry[] choices = createChoiceEntries(category2Choices.get(selection));
        choiceList.setListData(choices);
        ChoiceEntry selectedChoice = findChoice(choices, category2DefaultChoice.get(selection));
        choiceList.setSelectedValue(selectedChoice, false);
        explanationArea.setBorder(BorderFactory.createTitledBorder(selection));
        explanationArea.setText(getExplanation(selection));
        explanationArea.setCaretPosition(0);
    }

    /**
     * <p>
     * Returns the explanation for the given category.
     * </p>
     * <p>
     * This method should be public and static because it is independent from the {@link JDialog}
     * and it is also used by the eclipse projects.
     * </p>
     *
     * @param category The category for which the explanation is requested.
     * @return The explanation for the given category.
     */
    public static String getExplanation(String category) {
        synchronized (ChoiceSelector.class) {
            if (explanationMap == null) {
                explanationMap = new Properties();
                InputStream is = ChoiceSelector.class.getResourceAsStream(EXPLANATIONS_RESOURCE);
                try {
                    if (is == null) {
                        throw new FileNotFoundException(EXPLANATIONS_RESOURCE + " not found");
                    }
                    explanationMap.loadFromXML(is);
                } catch (IOException e) {
                    LOGGER.warn("Cannot load help message in rule view.", e);
                }
            }
        }
        String result = explanationMap.getProperty(category);
        if (result == null) {
            result = "No explanation for " + category + " available.";
        }

        return result;
    }

    /**
     * Checks if the given choice makes a proof unsound.
     *
     * @param choice The choice to check.
     * @return {@code true} proof will be unsound, {@code false} proof will be sound as long as all
     *         other choices are sound.
     */
    public static boolean isUnsound(String choice) {
        return "runtimeExceptions:ignore".equals(choice)
                || "initialisation:disableStaticInitialisation".equals(choice)
                || "intRules:arithmeticSemanticsIgnoringOF".equals(choice);
    }

    /**
     * Checks if the given choice makes a proof incomplete.
     *
     * @param choice The choice to check.
     * @return {@code true} proof will be incomplete, {@code false} proof will be complete as long
     *         as all other choices are complete.
     */
    public static boolean isIncomplete(String choice) {
        return "runtimeExceptions:ban".equals(choice) || "Strings:off".equals(choice)
                || "intRules:arithmeticSemanticsCheckingOF".equals(choice)
                || "integerSimplificationRules:minimal".equals(choice)
                || "programRules:None".equals(choice);
    }

    /**
     * Checks if additional information for the choice are available.
     *
     * @param choice The choice to check.
     * @return The additional information or {@code null} if no information are available.
     */
    public static String getInformation(String choice) {
        if ("JavaCard:on".equals(choice)) {
            return "Sound if a JavaCard program is proven.";
        } else if ("JavaCard:off".equals(choice)) {
            return "Sound if a Java program is proven.";
        } else if ("assertions:on".equals(choice)) {
            return "Sound if JVM is started with enabled assertions for the whole system.";
        } else if ("assertions:off".equals(choice)) {
            return "Sound if JVM is started with disabled assertions for the whole system.";
        } else {
            return null;
        }
    }

    /**
     * Searches the choice in the given {@link ChoiceEntry}s.
     *
     * @param choices The {@link ChoiceEntry}s to search in.
     * @param choice The choice to search.
     * @return The found {@link ChoiceEntry} for the given choice or {@code null} otherwise.
     */
    public static ChoiceEntry findChoice(ChoiceEntry[] choices, final String choice) {
        return ArrayUtil.search(choices, element -> element.choice().equals(choice));
    }

    /**
     * Creates {@link ChoiceEntry}s for all given choices.
     *
     * @param choices The choices.
     * @return The created {@link ChoiceEntry}s.
     */
    public static ChoiceEntry[] createChoiceEntries(Set<String> choices) {
        if (choices != null) {
            ChoiceEntry[] entries = new ChoiceEntry[choices.size()];
            int i = 0;
            for (String choice : choices) {
                entries[i] = createChoiceEntry(choice);
                i++;
            }
            return entries;
        } else {
            return null;
        }
    }

    /**
     * Creates a {@link ChoiceEntry} for the given choice.
     *
     * @param choice The choice.
     * @return The created {@link ChoiceEntry}.
     */
    public static ChoiceEntry createChoiceEntry(String choice) {
        return new ChoiceEntry(choice, isUnsound(choice), isIncomplete(choice),
            getInformation(choice));
    }

    /**
     * Represents a choice with all its meta information.
     *
     * @param choice The choice.
     * @param unsound Is unsound?
     * @param incomplete Is incomplete?
     * @param information An optionally information.
     * @author Martin Hentschel
     */
    public record ChoiceEntry(String choice, boolean unsound, boolean incomplete,
            String information) {

        /**
         * Text shown to the user in case of incompletness.
         */
        public static final String INCOMPLETE_TEXT = "incomplete";

        /**
         * Text shown to the user in case of unsoundness.
         */
        public static final String UNSOUND_TEXT = "Java modeling unsound";

        /**
         * Constructor.
         *
         * @param choice The choice.
         * @param unsound Is unsound?
         * @param incomplete Is incomplete?
         * @param information An optionally information.
         */
        public ChoiceEntry {
            assert choice != null;
        }

        /**
         * Returns the choice.
         *
         * @return The choice.
         */
        @Override
        public String choice() {
            return choice;
        }

        /**
         * Checks for soundness.
         *
         * @return {@code true} unsound, {@code false} sound.
         */
        @Override
        public boolean unsound() {
            return unsound;
        }

        /**
         * Checks for completeness.
         *
         * @return {@code true} incomplete, {@code false} complete.
         */
        @Override
        public boolean incomplete() {
            return incomplete;
        }

        /**
         * Returns the optionally information.
         *
         * @return The optionally information.
         */
        @Override
        public String information() {
            return information;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ChoiceEntry(String choice1, boolean unsound1, boolean incomplete1, String information1)) {
                return choice.equals(choice1) && incomplete == incomplete1
                        && unsound == unsound1
                        && Objects.equals(information, information1);
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (unsound && incomplete) {
                if (information != null) {
                    return choice + " (" + UNSOUND_TEXT + " and " + INCOMPLETE_TEXT + ", "
                        + information + ")";
                } else {
                    return choice + " (" + UNSOUND_TEXT + " and " + INCOMPLETE_TEXT + ")";
                }
            } else if (unsound) {
                if (information != null) {
                    return choice + " (" + UNSOUND_TEXT + ", " + information + ")";
                } else {
                    return choice + " (" + UNSOUND_TEXT + ")";
                }
            } else if (incomplete) {
                if (information != null) {
                    return choice + " (" + INCOMPLETE_TEXT + ", " + information + ")";
                } else {
                    return choice + " (" + INCOMPLETE_TEXT + ")";
                }
            } else {
                if (information != null) {
                    return choice + " (" + information + ")";
                } else {
                    return choice;
                }
            }
        }
    }
}
