/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.proof.io;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.util.KeYResourceManager;

public class RuleSourceFactory {

    /**
     * Use this key to set a system property where rule resources have to be looked up.
     */
    public static final String STD_TACLET_DIR_PROP_KEY = "org.key_project.stdTacletDirectory";

    private static final String PATH_TO_RULES = "rules/";

    public static RuleSource fromDefaultLocation(final String ruleFileName) {
        String stdTacletDir = System.getProperty(STD_TACLET_DIR_PROP_KEY);
        if (stdTacletDir == null) {
            return fromBuiltInRule(ruleFileName);
        } else {
            return initRuleFile(Paths.get(stdTacletDir, ruleFileName));
        }
    }

    public static RuleSource fromBuiltInRule(final String ruleFileName) {
        final URL u = KeYResourceManager.getManager().getResourceFile(Proof.class,
            PATH_TO_RULES + ruleFileName);
        if (u == null) {
            // a more specific exception type would probably be better
            throw new RuntimeException("Could not find rule file " + PATH_TO_RULES + ruleFileName);
        }
        return new UrlRuleSource(u);
    }

    public static RuleSource initRuleFile(final URL url) {
        return new UrlRuleSource(url);
    }

    public static RuleSource initRuleFile(final Path file) {
        return initRuleFile(file, false);
    }

    /**
     * Initialise this object from a file
     *
     * @param file the non-<code>null</code> file reference
     * @param compressed <code>true</code> iff the file is gzip-compressed
     * @return the rule source read from the file.
     */
    public static RuleSource initRuleFile(final Path file, boolean compressed) {
        if (compressed) {
            return new GZipFileRuleSource(file);
        } else {
            return new FileRuleSource(file);
        }
    }
}
