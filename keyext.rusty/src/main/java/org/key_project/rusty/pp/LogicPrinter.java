/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.pp;

import java.util.Iterator;

import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ldt.BoolLDT;
import org.key_project.rusty.logic.RustyBlock;
import org.key_project.rusty.logic.Semisequent;
import org.key_project.rusty.logic.Sequent;
import org.key_project.rusty.logic.SequentFormula;
import org.key_project.rusty.logic.op.*;
import org.key_project.rusty.logic.op.sv.ModalOperatorSV;
import org.key_project.rusty.logic.op.sv.SchemaVariable;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSet;

import static org.key_project.rusty.pp.PosTableLayouter.DEFAULT_LINE_WIDTH;

public class LogicPrinter {
    /**
     * Contains information on the concrete syntax of operators.
     */
    protected final NotationInfo notationInfo;

    /**
     * the services object
     */
    protected final Services services;

    /**
     * This chooses the layout.
     */
    protected PosTableLayouter layouter;

    private SVInstantiations instantiations = SVInstantiations.EMPTY_SVINSTANTIATIONS;

    private QuantifiableVariablePrintMode quantifiableVariablePrintMode =
        QuantifiableVariablePrintMode.NORMAL;

    private enum QuantifiableVariablePrintMode {
        NORMAL, WITH_OUT_DECLARATION
    }

    /**
     * Creates a LogicPrinter. Sets the sequent to be printed, as well as a ProgramPrinter to print
     * Java programs and a NotationInfo which determines the concrete syntax.
     *
     * @param notationInfo the NotationInfo for the concrete syntax
     * @param services services.
     * @param layouter the layouter to use
     */
    public LogicPrinter(NotationInfo notationInfo, Services services, PosTableLayouter layouter) {
        this.notationInfo = notationInfo;
        this.services = services;
        if (services != null) {
            notationInfo.refresh(services);
        }
        this.layouter = layouter;
    }

    /**
     * Creates a LogicPrinter that does not create a position table.
     *
     * @param notationInfo the NotationInfo for the concrete syntax
     * @param services The Services object
     */
    public static LogicPrinter purePrinter(NotationInfo notationInfo, Services services) {
        return new LogicPrinter(notationInfo, services, PosTableLayouter.pure());
    }

    /**
     * @return the Layouter
     */
    public PosTableLayouter layouter() {
        return layouter;
    }

    /**
     * @return the notationInfo associated with this LogicPrinter
     */
    public NotationInfo getNotationInfo() {
        return notationInfo;
    }

    /**
     * Resets the printer by creating a new layouter.
     */
    public void reset() {
        layouter = layouter.cloneArgs();
    }

    /**
     * sets the line width to the new value but does <em>not</em> reprint the sequent. The actual
     * set line width is the maximum of {@link PosTableLayouter#DEFAULT_LINE_WIDTH} and the given
     * value
     *
     * @param lineWidth the max. number of character to put on one line
     */
    public void setLineWidth(int lineWidth) {
        this.layouter.setLineWidth(Math.max(lineWidth, DEFAULT_LINE_WIDTH));
    }

    /**
     * Reprints the sequent. This can be useful if settings like PresentationFeatures or
     * abbreviations have changed.
     *
     * @param filter The SequentPrintFilter for seq
     * @param lineWidth the max. number of character to put on one line (the actual taken linewidth
     *        is the max of {@link PosTableLayouter#DEFAULT_LINE_WIDTH} and the given value
     */
    public void update(SequentPrintFilter filter, int lineWidth) {
        setLineWidth(lineWidth);
        reset();
        if (filter != null) {
            printFilteredSequent(filter);
        }
    }

    /**
     * sets instantiations of schema variables
     */
    public void setInstantiation(SVInstantiations instantiations) {
        this.instantiations = instantiations;
    }

    /**
     * Pretty-print a sequent. The sequent arrow is rendered as <code>=&gt;</code>. If the sequent
     * doesn't fit in one line, a line break is inserted after each formula, the sequent arrow is on
     * a line of its own, and formulae are indented w.r.t. the arrow. A line-break is printed after
     * the Sequent. No filtering is done.
     *
     * @param seq The Sequent to be pretty-printed
     */
    public void printSequent(Sequent seq) {
        layouter.beginC(0);
        printSequentInExistingBlock(seq);
        layouter.end();
    }

    private void printSequentInExistingBlock(Sequent seq) {
        try {
            Semisequent antec = seq.antecedent();
            Semisequent succ = seq.succedent();
            layouter.markStartSub();
            layouter.startTerm(antec.size() + succ.size());

            if (!antec.isEmpty()) {
                layouter.beginRelativeC(1);
                layouter.ind();
                printSemisequent(antec);
                layouter.end();
                layouter.brk();
            }

            printSequentArrow();
            if (!succ.isEmpty()) {
                layouter.beginRelativeC(1).brk();
                printSemisequent(succ);
                layouter.end();
            }

            layouter.markEndSub();
        } catch (UnbalancedBlocksException e) {
            throw new RuntimeException("Unbalanced blocks in pretty printer", e);
        }
    }

    /**
     * Pretty-prints a Semisequent. Formulae are separated by commas.
     *
     * @param semiseq the semisequent to be printed
     */
    public void printSemisequent(Semisequent semiseq) {
        for (int i = 0; i < semiseq.size(); i++) {
            layouter.markStartSub();
            printConstrainedFormula(semiseq.get(i));
            layouter.markEndSub();
            if (i != semiseq.size() - 1) {
                layouter.print(",").brk();
            }
        }
    }

    public void printSemisequent(ImmutableList<SequentPrintFilterEntry> formulas) {
        Iterator<SequentPrintFilterEntry> it = formulas.iterator();
        SequentPrintFilterEntry entry;

        for (int size = formulas.size() - 1; size >= 0; --size) {
            entry = it.next();
            layouter.markStartSub();
            printConstrainedFormula(entry.getFilteredFormula());
            layouter.markEndSub();
            if (size != 0) {
                layouter.print(",").brk();
            }
        }
    }

    /**
     * Pretty-prints a constrained formula. The constraint "Constraint.BOTTOM" is suppressed
     *
     * @param cfma the constrained formula to be printed
     */
    public void printConstrainedFormula(SequentFormula cfma) {
        printTerm(cfma.formula());
    }

    /**
     * Pretty-print a sequent. The sequent arrow is rendered as <code>=&gt;</code>. If the sequent
     * doesn't fit in one line, a line break is inserted after each formula, the sequent arrow is on
     * a line of its own, and formulae are indented w.r.t. the arrow. A line-break is printed after
     * the Sequent.
     *
     * @param filter The SequentPrintFilter for seq
     */
    public void printFilteredSequent(SequentPrintFilter filter) {
        try {
            ImmutableList<SequentPrintFilterEntry> antec = filter.getFilteredAntec();
            ImmutableList<SequentPrintFilterEntry> succ = filter.getFilteredSucc();

            layouter.markStartSub();
            layouter.startTerm(antec.size() + succ.size());

            layouter.beginC(1).ind();
            printSemisequent(antec);

            layouter.brk(1, -1);
            printSequentArrow();
            layouter.brk();

            printSemisequent(succ);

            layouter.markEndSub();
            layouter.end();
        } catch (UnbalancedBlocksException e) {
            throw new RuntimeException("Unbalanced blocks in pretty printer", e);
        }
    }

    /**
     * Pretty-prints a term or formula. How it is rendered depends on the NotationInfo given to the
     * constructor.
     *
     * @param t the Term to be printed
     */
    public void printTerm(Term t) {
        if (notationInfo.getAbbrevMap().isEnabled(t)) {
            layouter.startTerm(0);
            layouter.print(notationInfo.getAbbrevMap().getAbbrev(t));
        } else {
            /*
             * if (t.hasLabels() && !getVisibleTermLabels(t).isEmpty() && notationInfo
             * .getNotation(t.op()).getPriority() < NotationInfo.PRIORITY_ATOM) {
             * layouter.print("(");
             * }
             */
            notationInfo.getNotation(t.op()).print(t, this);
            /*
             * if (t.hasLabels() && !getVisibleTermLabels(t).isEmpty() && notationInfo
             * .getNotation(t.op()).getPriority() < NotationInfo.PRIORITY_ATOM) {
             * layouter.print(")");
             * }
             */
        }
        /*
         * if (t.hasLabels()) {
         * printLabels(t);
         * }
         */
    }

    /**
     * Pretty-prints a set of terms.
     *
     * @param terms the terms to be printed
     */
    public void printTerm(ImmutableSet<Term> terms) {
        layouter.print("{");
        Iterator<Term> it = terms.iterator();
        while (it.hasNext()) {
            printTerm(it.next());
            if (it.hasNext()) {
                layouter.print(", ");
            }
        }
        layouter.print("}");
    }

    /**
     * Pretty-prints a term or formula in the same block. How it is rendered depends on the
     * NotationInfo given to the constructor. `In the same block' means that no extra indentation
     * will be added if line breaks are necessary. A formula <code>a &amp; (b
     * &amp; c)</code> would print <code>a &amp; b &amp; c</code>, omitting the redundant
     * parentheses. The subformula <code>b &amp; c</code> is printed using this method to get a
     * layout of
     *
     * <pre>
     * a &amp; b &amp; c
     * </pre>
     * <p>
     * instead of
     *
     * <pre>
     * a &amp; b &amp; c
     * </pre>
     *
     * @param t the Term to be printed
     */
    public void printTermContinuingBlock(Term t) {
        /*
         * if (t.hasLabels() && !getVisibleTermLabels(t).isEmpty()
         * && notationInfo.getNotation(t.op()).getPriority() < NotationInfo.PRIORITY_ATOM) {
         * layouter.print("(");
         * }
         */
        notationInfo.getNotation(t.op()).printContinuingBlock(t, this);
        /*
         * if (t.hasLabels() && !getVisibleTermLabels(t).isEmpty()
         * && notationInfo.getNotation(t.op()).getPriority() < NotationInfo.PRIORITY_ATOM) {
         * layouter.print(")");
         * }
         * if (t.hasLabels()) {
         * printLabels(t);
         * }
         */
    }

    /**
     * Print a term in <code>f(t1,...tn)</code> style. If the operator has arity 0, no parentheses
     * are printed, i.e. <code>f</code> instead of <code>f()</code>. If the term doesn't fit on one
     * line, <code>t2...tn</code> are aligned below <code>t1</code>.
     *
     * @param t the term to be printed.
     */
    public void printFunctionTerm(Term t) {
        boolean isKeyword = false;
        if (services != null) {
            // Function measuredByEmpty = services.getTermBuilder().getMeasuredByEmpty();
            BoolLDT bool = services.getLDTs().getBoolLDT();

            isKeyword = (t.op() == bool.getFalseConst() || t.op() == bool.getTrueConst());
        }
        /*
         * if (notationInfo.isPrettySyntax() && services != null
         * && FieldPrinter.isJavaFieldConstant(t, getHeapLDT(), services)
         * && getNotationInfo().isHidePackagePrefix()) {
         * // Hide package prefix when printing field constants.
         * layouter.startTerm(0);
         * String name = t.op().name().toString();
         * int index = name.lastIndexOf('.');
         * String prettyFieldName = name.substring(index + 1);
         * if (isKeyword) {
         * layouter.markStartKeyword();
         * }
         * layouter.print(prettyFieldName);
         * if (isKeyword) {
         * layouter.markEndKeyword();
         * }
         * } else
         */ {
            String name = t.op().name().toString();
            layouter.startTerm(t.arity());
            boolean alreadyPrinted = false;
            /*
             * if (t.op() instanceof SortDependingFunction op) {
             * if (op.getKind().compareTo(JavaDLTheory.EXACT_INSTANCE_NAME) == 0) {
             * layouter.print(op.getSortDependingOn().declarationString());
             * layouter.print("::");
             * layouter.keyWord(op.getKind().toString());
             * alreadyPrinted = true;
             * }
             * }
             */
            if (isKeyword) {
                layouter.markStartKeyword();
            }
            if (!alreadyPrinted) {
                layouter.print(name);
            }
            if (isKeyword) {
                layouter.markEndKeyword();
            }
            if (!t.boundVars().isEmpty()) {
                layouter.print("{").beginC(0);
                printVariables((ImmutableArray<QuantifiableVariable>) t.boundVars(),
                    quantifiableVariablePrintMode);
                layouter.print("}").end();
            }
            if (t.arity() > 0) {
                layouter.print("(").beginC(0);
                for (int i = 0, n = t.arity(); i < n; i++) {
                    layouter.markStartSub();
                    printTerm(t.sub(i));
                    layouter.markEndSub();

                    if (i < n - 1) {
                        layouter.print(",").brk(1, 0);
                    }
                }
                layouter.print(")").end();
            }
        }
    }

    /**
     * Print a unary term in prefix style. For instance <code>!a</code>. No line breaks are
     * possible.
     *
     * @param name the prefix operator
     * @param t whole term
     * @param sub the subterm to be printed
     * @param ass the associativity for the subterm
     */
    public void printPrefixTerm(String name, Term t, Term sub, int ass) {
        layouter.startTerm(1);
        if (t.op() == Junctor.NOT) {
            layouter.markStartKeyword();
        }
        layouter.print(name);
        if (t.op() == Junctor.NOT) {
            layouter.markEndKeyword();
        }
        maybeParens(sub, ass);
    }

    /**
     * Print a binary term in infix style. For instance <code>p
     * &amp; q</code>, where <code>&amp;</code> is the infix operator. If line breaks are necessary,
     * the format is like
     *
     * <pre>
     * {@code p & q}
     * </pre>
     * <p>
     * The subterms are printed using {@link #printTermContinuingBlock(Term)}.
     *
     * @param l the left subterm
     * @param assLeft associativity for left subterm
     * @param name the infix operator
     * @param t whole term
     * @param r the right subterm
     * @param assRight associativity for right subterm
     */
    public void printInfixTerm(Term l, int assLeft, String name, Term t, Term r, int assRight) {
        int indent = name.length() + 1;
        layouter.beginC(indent);
        printInfixTermContinuingBlock(l, assLeft, name, t, r, assRight);
        layouter.end();
    }

    /**
     * Print a binary term in infix style, continuing a containing block. See
     * {@link #printTermContinuingBlock(Term)} for the idea. Otherwise, like
     * {@link #printInfixTerm(Term, int, String, Term, Term, int)}.
     *
     * @param l the left subterm
     * @param assLeft associativity for left subterm
     * @param name the infix operator
     * @param t whole term
     * @param r the right subterm
     * @param assRight associativity for right subterm
     */
    public void printInfixTermContinuingBlock(Term l, int assLeft, String name, Term t, Term r,
            int assRight) {
        boolean isKeyword = false;
        if (services != null) {
            // LocSetLDT loc = services.getTypeConverter().getLocSetLDT();
            isKeyword = (t.op() == Junctor.AND || t.op() == Junctor.OR || t.op() == Junctor.IMP
            /* || t.op() == Equality.EQV || t.op() == loc.getUnion() */);
        }
        int indent = name.length() + 1;
        layouter.startTerm(2);
        layouter.ind();
        maybeParens(l, assLeft);
        layouter.brk(1, -indent);
        if (isKeyword) {
            layouter.markStartKeyword();
        }
        layouter.print(name);
        if (isKeyword) {
            layouter.markEndKeyword();
        }
        layouter.ind(1, 0);
        maybeParens(r, assRight);
    }

    /**
     * Print a term with an update. This looks like <code>{u} t</code>. If line breaks are
     * necessary, the format is
     *
     * <pre>
     * {u}
     *   t
     * </pre>
     *
     * @param l the left brace
     * @param r the right brace
     * @param t the update term
     * @param ass3 associativity for phi
     */
    public void printUpdateApplicationTerm(String l, String r, Term t, int ass3) {
        assert t.op() instanceof UpdateApplication && t.arity() == 2;

        layouter.markStartUpdate();
        layouter.beginC().print(l);
        layouter.startTerm(t.arity());

        layouter.markStartSub();
        printTerm(t.sub(0));
        layouter.markEndSub();

        layouter.print(r);
        layouter.markEndUpdate();
        layouter.brk(0);

        maybeParens(t.sub(1), ass3);

        layouter.end();
    }

    /**
     * Print an elementary update. This looks like <code>loc := val</code>
     *
     * @param asgn the assignment operator (including spaces)
     * @param ass2 associativity for the new values
     */
    public void printElementaryUpdate(String asgn, Term t, int ass2) {
        ElementaryUpdate op = (ElementaryUpdate) t.op();

        assert t.arity() == 1;
        layouter.startTerm(1);

        layouter.print(op.lhs().name().toString());

        layouter.print(asgn);

        maybeParens(t.sub(0), ass2);
    }

    private void printParallelUpdateHelper(String separator, Term t, int ass) {
        assert t.arity() == 2;
        layouter.startTerm(2);

        if (t.sub(0).op() == UpdateJunctor.PARALLEL_UPDATE) {
            layouter.markStartSub();
            printParallelUpdateHelper(separator, t.sub(0), ass);
            layouter.markEndSub();
        } else {
            maybeParens(t.sub(0), ass);
        }

        layouter.brk().print(separator + " ");

        if (t.sub(1).op() == UpdateJunctor.PARALLEL_UPDATE) {
            layouter.markStartSub();
            layouter.print("(");
            printParallelUpdateHelper(separator, t.sub(1), ass);
            layouter.print(")");
            layouter.markEndSub();
        } else {
            maybeParens(t.sub(1), ass);
        }
    }

    public void printParallelUpdate(String separator, Term t, int ass) {
        layouter.beginC(0);
        printParallelUpdateHelper(separator, t, ass);
        layouter.end();
    }

    private void printVariables(ImmutableArray<QuantifiableVariable> vars,
            QuantifiableVariablePrintMode mode) {
        int size = vars.size();
        for (int j = 0; j != size; j++) {
            final QuantifiableVariable v = vars.get(j);
            if (v instanceof LogicVariable) {
                if (mode != QuantifiableVariablePrintMode.WITH_OUT_DECLARATION) {
                    // do not print declarations in taclets...
                    layouter.print(v.sort().name().toString());
                    layouter.print(" ");
                }
                if (services != null && notationInfo.getAbbrevMap()
                        .containsTerm(services.getTermFactory().createTerm(v))) {
                    layouter.print(notationInfo.getAbbrevMap()
                            .getAbbrev(services.getTermFactory().createTerm(v)));
                } else {
                    layouter.print(v.name().toString());
                }
            } else {
                layouter.print(v.name().toString());
            }
            if (j < size - 1) {
                layouter.print(", ");
            }
        }
        layouter.print(";");
    }

    public void printIfThenElseTerm(Term t, String keyword) {
        layouter.startTerm(t.arity());

        layouter.beginC(0);
        layouter.keyWord(keyword);
        if (!t.varsBoundHere(0).isEmpty()) {
            layouter.print(" ");
            printVariables((ImmutableArray<QuantifiableVariable>) t.varsBoundHere(0),
                quantifiableVariablePrintMode);
        }

        layouter.print(" (");
        layouter.markStartSub();
        printTerm(t.sub(0));
        layouter.markEndSub();
        layouter.print(")");

        for (int i = 1; i < t.arity(); ++i) {
            layouter.brk(1, 3);
            layouter.print(" ");
            layouter.markStartKeyword();
            if (i == 1) {
                layouter.print("\\then");
            } else {
                layouter.print("\\else");
            }
            layouter.markEndKeyword();
            layouter.print(" (");
            layouter.markStartSub();
            printTerm(t.sub(i));
            layouter.markEndSub();
            layouter.print(")");
        }

        layouter.end();
    }

    /**
     * Print a substitution term. This looks like <code>{var/t}s</code>. If line breaks are
     * necessary, the format is
     *
     * <pre>
     * {var/t}
     *   s
     * </pre>
     *
     * @param l the String used as left brace symbol
     * @param v the {@link QuantifiableVariable} to be substituted
     * @param t the Term to be used as new value
     * @param ass2 the int defining the associativity for the new value
     * @param r the String used as right brace symbol
     * @param phi the substituted term/formula
     * @param ass3 the int defining the associativity for phi
     */
    public void printSubstTerm(String l, QuantifiableVariable v, Term t, int ass2, String r,
            Term phi, int ass3) {
        layouter.beginC().print(l);
        printVariables(new ImmutableArray<>(v), quantifiableVariablePrintMode);
        layouter.startTerm(2);
        maybeParens(t, ass2);
        layouter.print(r).brk(0);
        maybeParens(phi, ass3);
        layouter.end();
    }

    /**
     * Print a quantified term. Normally, this looks like <code>all x:s.phi</code>. If line breaks
     * are necessary, the format is
     *
     * <pre>
     * all x:s.
     *   phi
     * </pre>
     * <p>
     * Note that the parameter <code>var</code> has to contain the variable name with colon and
     * sort.
     *
     * @param name the name of the quantifier
     * @param vars the quantified variables (+colon and sort)
     * @param phi the quantified formula
     * @param ass associativity for phi
     */
    public void printQuantifierTerm(String name, ImmutableArray<QuantifiableVariable> vars,
            Term phi, int ass) {
        layouter.beginC();
        layouter.keyWord(name);
        layouter.print(" ");
        printVariables(vars, quantifiableVariablePrintMode);
        layouter.brk();
        layouter.startTerm(1);
        maybeParens(phi, ass);
        layouter.end();
    }

    /**
     * Print a constant. This just prints the string <code>s</code> and marks it as a nullary term.
     *
     * @param s the constant
     */
    public void printConstant(String s) {
        layouter.startTerm(0);
        layouter.print(s);
    }

    /**
     * Print a constant. This just prints the string <code>s</code> and marks it as a nullary term.
     *
     * @param t constant as term to be printed
     * @param s name of the constant
     */
    public void printConstant(Term t, String s) {
        layouter.startTerm(0);
        boolean isKeyword = t.op() == Junctor.FALSE || t.op() == Junctor.TRUE;
        if (isKeyword) {
            layouter.markStartKeyword();
        }
        layouter.print(s);
        if (isKeyword) {
            layouter.markEndKeyword();
        }
    }

    /**
     * Print a Rusty block. This is formatted using the ProgramPrinter given to the constructor. The
     * result is indented according to the surrounding material. The first `executable' statement is
     * marked for highlighting.
     *
     * @param rb the Java block to be printed
     */
    public void printRustyBlock(RustyBlock rb) {
        printSourceElement(rb.program());
    }

    /**
     * Print a DL modality formula. <code>phi</code> is the whole modality formula, not just the
     * subformula inside the modality. Normally, this looks like <code>&lt;Program&gt;psi</code>,
     * where <code>psi = phi.sub(0)</code>. No line breaks are inserted, as the program itself is
     * always broken. In case of a program modality with arity greater than 1, the subformulae are
     * listed between parens, like <code>&lt;Program&gt;(psi1,psi2)</code>
     */

    public void printModalityTerm(String left, RustyBlock jb, String right, Term phi, int ass) {
        assert jb != null;
        assert jb.program() != null;
        if (phi.op() instanceof Modality mod && mod.kind() instanceof ModalOperatorSV) {
            Object o = getInstantiations().getInstantiation(mod.kind());
            if (o instanceof Modality.RustyModalityKind kind) {
                if (notationInfo.getAbbrevMap().isEnabled(phi)) {
                    layouter.startTerm(0);
                    layouter.print(notationInfo.getAbbrevMap().getAbbrev(phi));
                } else {
                    Term[] ta = new Term[phi.arity()];
                    for (int i = 0; i < phi.arity(); i++) {
                        ta[i] = phi.sub(i);
                    }
                    final Modality m =
                        Modality.getModality((Modality.RustyModalityKind) o, mod.program());
                    final Term term = services.getTermFactory().createTerm(m, ta,
                        (ImmutableArray<QuantifiableVariable>) phi.boundVars());
                    notationInfo.getNotation(m).print(term, this);
                    return;
                }
            }
        }

        layouter.markModPosTbl();
        layouter.startTerm(phi.arity());
        layouter.print(left);
        layouter.markStartRustyBlock();
        printRustyBlock(jb);
        layouter.markEndRustyBlock();
        layouter.print(right + " ");
        if (phi.arity() == 1) {
            maybeParens(phi.sub(0), ass);
        } else if (phi.arity() > 1) {
            layouter.print("(");
            for (int i = 0; i < phi.arity(); i++) {
                layouter.markStartSub();
                printTerm(phi.sub(i));
                layouter.markEndSub();
                if (i < phi.arity() - 1) {
                    layouter.print(",").brk(1, 0);
                }
            }
            layouter.print(")");
        }
    }

    /**
     * Returns the pretty-printed sequent in a StringBuffer. This should only be called after a
     * <tt>printSequent</tt> invocation returns.
     *
     * @return the pretty-printed sequent.
     */
    public String result() {
        return layouter.result();
    }

    /**
     * Prints a subterm, if needed with parentheses. Each subterm has a Priority. If the priority is
     * less than the associativity for that subterm fixed by the Notation/NotationInfo, parentheses
     * are needed.
     *
     * <p>
     * If prio and associativity are equal, the subterm is printed using
     * {@link #printTermContinuingBlock(Term)}. This currently only makes a difference for infix
     * operators.
     *
     * @param t the subterm to print
     * @param ass the associativity for this subterm
     */
    protected void maybeParens(Term t, int ass) {
        if (t.op() instanceof SchemaVariable sv && instantiations != null
                && instantiations.getInstantiation(sv) instanceof Term t1) {
            t = t1;
        }

        if (notationInfo.getNotation(t.op()).getPriority() < ass) {
            layouter.markStartSub();
            layouter.print("(");
            printTerm(t);
            layouter.print(")");
            layouter.markEndSub();
        } else {
            layouter.markStartSub();
            if (notationInfo.getNotation(t.op()).getPriority() == ass) {
                printTermContinuingBlock(t);
            } else {
                printTerm(t);
            }
            layouter.markEndSub();
        }
    }

    protected void printSequentArrow() {
        if (getNotationInfo().isUnicodeEnabled()) {
            // layouter.print(Character.toString(UnicodeHelper.SEQUENT_ARROW));
        } else {
            layouter.print("==>");
        }
    }

    /**
     * @return The SVInstantiations given with the last printTaclet call.
     */
    public SVInstantiations getInstantiations() {
        return instantiations;
    }

    private void printSourceElement(RustyProgramElement element) {
        new PrettyPrinter(layouter, instantiations, services,
            notationInfo.isPrettySyntax(),
            notationInfo.isUnicodeEnabled()).printFragment(element);
    }

    /**
     * Pretty-prints a ProgramElement.
     *
     * @param pe You've guessed it, the ProgramElement to be pretty-printedprint(Term t,
     *        LogicPrinter sp)
     */
    public void printProgramElement(RustyProgramElement pe) {
        if (pe instanceof ProgramVariable) {
            printProgramVariable((ProgramVariable) pe);
        } else {
            printSourceElement(pe);
        }
    }

    /**
     * Pretty-Prints a ProgramVariable in the logic, not in Java blocks. Prints out the full (logic)
     * name, so if A.b is private, it becomes a.A::b .
     *
     * @param pv The ProgramVariable in the logicprint(Term t, LogicPrinter sp)
     */
    public void printProgramVariable(ProgramVariable pv) {
        // LOGGER.debug("PP PV {}", pv.name());
        layouter.beginC().print(pv.name().toString()).end();
    }
}