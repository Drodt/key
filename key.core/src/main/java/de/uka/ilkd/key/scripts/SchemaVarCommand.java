/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.scripts;

import java.util.Map;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.op.JOperatorSV;
import de.uka.ilkd.key.logic.op.SchemaVariableFactory;
import de.uka.ilkd.key.pp.AbbrevMap;
import de.uka.ilkd.key.scripts.meta.Option;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;

/**
 *
 */
public class SchemaVarCommand extends AbstractCommand<SchemaVarCommand.Parameters> {

    public SchemaVarCommand() {
        super(Parameters.class);
    }

    @Override
    public Parameters evaluateArguments(EngineState state, Map<String, Object> arguments)
            throws Exception {
        return state.getValueInjector().inject(this, new Parameters(), arguments);
    }

    @Override
    public void execute(Parameters args) throws ScriptException, InterruptedException {

        if (args.type == null || args.var == null) {
            throw new ScriptException("Missing argument: type var");
        }

        AbbrevMap abbrMap = state.getAbbreviations();

        if (!args.var.matches("@[a-zA-Z0-9_]")) {
            throw new ScriptException("Illegal variable name: " + args.var);
        }

        Name schemaVar = new Name("_SCHEMA_" + args.var.substring(1));

        try {
            JOperatorSV sv;
            if ("Formula".equals(args.type)) {
                sv = SchemaVariableFactory.createFormulaSV(schemaVar);
            } else {
                Sort sort = state.toSort(args.type);
                sv = SchemaVariableFactory.createTermSV(schemaVar, sort);
            }

            JTerm term = state.getProof().getServices().getTermFactory().createTerm(sv);

            abbrMap.put(term, args.var, true);
        } catch (Exception e) {
            throw new ScriptException(e);
        }

    }

    @Override
    public String getName() {
        return "schemaVar";
    }

    public static class Parameters {
        @Option("#2")
        public String type;
        @Option("#3")
        public String var;
    }

}
