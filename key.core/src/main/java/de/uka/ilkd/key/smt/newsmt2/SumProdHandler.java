/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.smt.newsmt2;

import java.util.*;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.smt.SMTTranslationException;

import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;

import static de.uka.ilkd.key.logic.equality.RenamingTermProperty.RENAMING_TERM_PROPERTY;

// W I P
public class SumProdHandler implements SMTHandler {

    private Function bsumOp, bprodOp;

    // key is the term to identify the bsum, value is the name used for that function.
    private final Map<Term, SExpr> usedBsumTerms = new LinkedHashMap<>();

    // key is the term to identify the bprod, value is the name used for that function.
    private final Map<Term, SExpr> usedBprodTerms = new LinkedHashMap<>();

    @Override
    public void init(MasterHandler masterHandler, Services services, Properties handlerSnippets,
            String[] handlerOptions) {
        bsumOp = services.getTypeConverter().getIntegerLDT().getBsum();
        bprodOp = services.getTypeConverter().getIntegerLDT().getBprod();
    }

    @Override
    public boolean canHandle(Operator op) {
        boolean isSum = op == bsumOp;
        boolean isProd = op == bprodOp;
        return isSum || isProd;
    }

    @Override
    public SExpr handle(MasterHandler trans, Term term) throws SMTTranslationException {
        Operator op = term.op();
        if (op == bsumOp) {
            for (Term t : usedBsumTerms.keySet()) {
                if (RENAMING_TERM_PROPERTY.equalsModThisProperty(t, term)) {
                    return usedBsumTerms.get(t);
                }
            }
            List<SExpr> exprs = new LinkedList<>();
            exprs.add(trans.translate(term.sub(0)));
            exprs.add(SExprs.coerce(trans.translate(term.sub(1)), IntegerOpHandler.INT));
            String s = String.valueOf(usedBsumTerms.size());
            trans.addDeclaration(bsumOrProdDecl("bsum", s));
            SExpr ret = new SExpr("bsum" + s, IntegerOpHandler.INT, exprs);
            usedBsumTerms.put(term, ret);
            return ret;
        } else if (op == bprodOp) {
            for (Term t : usedBprodTerms.keySet()) {
                if (RENAMING_TERM_PROPERTY.equalsModThisProperty(t, term)) {
                    return usedBprodTerms.get(t);
                }
            }
            List<SExpr> exprs = new LinkedList<>();
            exprs.add(trans.translate(term.sub(0)));
            exprs.add(SExprs.coerce(trans.translate(term.sub(1)), IntegerOpHandler.INT));
            String s = String.valueOf(usedBprodTerms.size());
            trans.addDeclaration(bsumOrProdDecl("bprod", s));
            SExpr ret = new SExpr("bprod" + s, IntegerOpHandler.INT, exprs);
            usedBprodTerms.put(term, ret);
            return ret;
        } else { // unreachable
            return new SExpr("ERROR");
        }
    }

    private SExpr bsumOrProdDecl(String fun, String number) {
        return new SExpr("declare-fun", IntegerOpHandler.INT, new SExpr(fun + number),
            new SExpr(new SExpr("Int"), new SExpr("Int")), new SExpr("Int"));
    }
}
