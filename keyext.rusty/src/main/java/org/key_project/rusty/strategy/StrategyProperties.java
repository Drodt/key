/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy;

import java.util.Map;
import java.util.Properties;

import org.key_project.rusty.settings.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StrategyProperties extends Properties {
    public static final String STOPMODE_OPTIONS_KEY = "STOPMODE_OPTIONS_KEY";
    public static final String STOPMODE_DEFAULT = "STOPMODE_DEFAULT";
    public static final String STOPMODE_NONCLOSE = "STOPMODE_NONCLOSE";

    public static final String SPLITTING_OPTIONS_KEY = "SPLITTING_OPTIONS_KEY";
    public static final String SPLITTING_NORMAL = "SPLITTING_NORMAL";
    public static final String SPLITTING_OFF = "SPLITTING_OFF";
    public static final String SPLITTING_DELAYED = "SPLITTING_DELAYED";

    public static final String LOOP_OPTIONS_KEY = "LOOP_OPTIONS_KEY";
    public static final String LOOP_EXPAND = "LOOP_EXPAND";
    public static final String LOOP_INVARIANT = "LOOP_INVARIANT";
    public static final String LOOP_NONE = "LOOP_NONE";

    public static final String FUNCTION_OPTIONS_KEY = "FUNCTION_OPTIONS_KEY";
    public static final String FUNCTION_EXPAND = "FUNCTION_EXPAND";
    public static final String FUNCTION_CONTRACT = "FUNCTION_CONTRACT";
    public static final String FUNCTION_NONE = "FUNCTION_NONE";

    public static final String NON_LIN_ARITH_OPTIONS_KEY = "NON_LIN_ARITH_OPTIONS_KEY";
    public static final String NON_LIN_ARITH_NONE = "NON_LIN_ARITH_NONE";
    public static final String NON_LIN_ARITH_DEF_OPS = "NON_LIN_ARITH_DEF_OPS";
    public static final String NON_LIN_ARITH_COMPLETION = "NON_LIN_ARITH_COMPLETION";

    public static final String QUANTIFIERS_OPTIONS_KEY = "QUANTIFIERS_OPTIONS_KEY";
    public static final String QUANTIFIERS_NONE = "QUANTIFIERS_NONE";
    public static final String QUANTIFIERS_NON_SPLITTING = "QUANTIFIERS_NON_SPLITTING";
    public static final String QUANTIFIERS_NON_SPLITTING_WITH_PROGS =
        "QUANTIFIERS_NON_SPLITTING_WITH_PROGS";
    public static final String QUANTIFIERS_INSTANTIATE = "QUANTIFIERS_INSTANTIATE";

    public static final String AUTO_INDUCTION_OPTIONS_KEY = "AUTO_INDUCTION_OPTIONS_KEY";
    public static final String AUTO_INDUCTION_OFF = "AUTO_INDUCTION_OFF";
    public static final String AUTO_INDUCTION_RESTRICTED = "AUTO_INDUCTION_RESTRICTED";
    public static final String AUTO_INDUCTION_ON = "AUTO_INDUCTION_ON";
    public static final String AUTO_INDUCTION_LEMMA_ON = "AUTO_INDUCTION_LEMMA_ON";

    public static final int USER_TACLETS_NUM = 3;

    public static final String USER_TACLETS_OFF = "USER_TACLETS_OFF";
    public static final String USER_TACLETS_LOW = "USER_TACLETS_LOW";
    public static final String USER_TACLETS_HIGH = "USER_TACLETS_HIGH";

    private static final String CATEGORY = "StrategyProperty";
    /// Section key for storage file to identify strategy settings
    private static final String STRATEGY_PROPERTY = "[" + CATEGORY + "]";
    private static final String USER_TACLETS_OPTIONS_KEY_BASE = "USER_TACLETS_OPTIONS_KEY";

    // String identities.
    private static final String[] STRING_POOL = { STOPMODE_OPTIONS_KEY, STOPMODE_DEFAULT,
        STOPMODE_NONCLOSE,
        SPLITTING_OPTIONS_KEY, SPLITTING_NORMAL, SPLITTING_OFF, SPLITTING_DELAYED, LOOP_OPTIONS_KEY,
        LOOP_EXPAND, LOOP_INVARIANT, LOOP_NONE, FUNCTION_OPTIONS_KEY, FUNCTION_EXPAND,
        FUNCTION_CONTRACT, FUNCTION_NONE,
        NON_LIN_ARITH_OPTIONS_KEY, NON_LIN_ARITH_NONE, NON_LIN_ARITH_DEF_OPS,
        NON_LIN_ARITH_COMPLETION, QUANTIFIERS_OPTIONS_KEY,
        QUANTIFIERS_NONE, QUANTIFIERS_NON_SPLITTING, QUANTIFIERS_NON_SPLITTING_WITH_PROGS,
        QUANTIFIERS_INSTANTIATE, AUTO_INDUCTION_OPTIONS_KEY,
        AUTO_INDUCTION_OFF, AUTO_INDUCTION_RESTRICTED, AUTO_INDUCTION_ON, AUTO_INDUCTION_LEMMA_ON,
        USER_TACLETS_OPTIONS_KEY_BASE, USER_TACLETS_OFF, USER_TACLETS_LOW, USER_TACLETS_HIGH,
        userTacletsOptionsKey(1), userTacletsOptionsKey(2), userTacletsOptionsKey(3),
    };

    private static final Properties DEFAULT_MAP = new Properties();
    private static final Logger LOGGER = LoggerFactory.getLogger(StrategyProperties.class);

    static {
        DEFAULT_MAP.setProperty(SPLITTING_OPTIONS_KEY, SPLITTING_DELAYED);
        DEFAULT_MAP.setProperty(LOOP_OPTIONS_KEY, LOOP_INVARIANT);
        DEFAULT_MAP.setProperty(FUNCTION_OPTIONS_KEY, FUNCTION_CONTRACT);
        DEFAULT_MAP.setProperty(NON_LIN_ARITH_OPTIONS_KEY, NON_LIN_ARITH_NONE);
        DEFAULT_MAP.setProperty(QUANTIFIERS_OPTIONS_KEY, QUANTIFIERS_NON_SPLITTING_WITH_PROGS);
        for (int i = 1; i <= USER_TACLETS_NUM; ++i) {
            DEFAULT_MAP.setProperty(userTacletsOptionsKey(i), USER_TACLETS_OFF);
        }
        DEFAULT_MAP.setProperty(STOPMODE_OPTIONS_KEY, STOPMODE_DEFAULT);
        DEFAULT_MAP.setProperty(AUTO_INDUCTION_OPTIONS_KEY, AUTO_INDUCTION_OFF); // chrisg
    }

    public StrategyProperties() {
        put(SPLITTING_OPTIONS_KEY, DEFAULT_MAP.get(SPLITTING_OPTIONS_KEY));
        put(LOOP_OPTIONS_KEY, DEFAULT_MAP.get(LOOP_OPTIONS_KEY));
        put(FUNCTION_OPTIONS_KEY, DEFAULT_MAP.get(FUNCTION_OPTIONS_KEY));
        put(NON_LIN_ARITH_OPTIONS_KEY, DEFAULT_MAP.get(NON_LIN_ARITH_OPTIONS_KEY));
        put(QUANTIFIERS_OPTIONS_KEY, DEFAULT_MAP.get(QUANTIFIERS_OPTIONS_KEY));
        for (int i = 1; i <= USER_TACLETS_NUM; ++i) {
            put(userTacletsOptionsKey(i), DEFAULT_MAP.get(userTacletsOptionsKey(i)));
        }
        put(STOPMODE_OPTIONS_KEY, DEFAULT_MAP.get(STOPMODE_OPTIONS_KEY));
        put(AUTO_INDUCTION_OPTIONS_KEY, DEFAULT_MAP.getProperty(AUTO_INDUCTION_OPTIONS_KEY));
    }

    public static String userTacletsOptionsKey(int i) {
        return USER_TACLETS_OPTIONS_KEY_BASE + i;
    }

    public static StrategyProperties read(Configuration category) {
        category = category.getOrCreateSection("options");
        StrategyProperties sp = new StrategyProperties();
        for (Map.Entry<Object, Object> entry : DEFAULT_MAP.entrySet()) {
            final var def = entry.getValue();
            final var obj = category.get(entry.getKey().toString());
            if (obj != null && def.getClass() == obj.getClass()) {
                sp.put(entry.getKey(), obj);
            }
        }
        return sp;
    }

    public void write(Configuration category) {
        category = category.getOrCreateSection("options");
        for (Map.Entry<Object, Object> entry : entrySet()) {
            final var value = entry.getValue();
            if (value != null) {
                category.set(entry.getKey().toString(), value);
            }
        }
    }
}
