/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;


import org.key_project.logic.IntIterator;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ast.SourceData;
import org.key_project.rusty.ast.stmt.Statement;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.logic.PosInProgram;
import org.key_project.rusty.logic.PossibleProgramPrefix;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableList;


public class ContextBlockExpression extends BlockExpression {
    /// length of this progran prefix
    private final int patternPrefixLength;

    public ContextBlockExpression(ImmutableList<Statement> statements, Expr expr) {
        super(statements, expr);
        patternPrefixLength = this.getPrefixLength();
    }

    public ContextBlockExpression(ExtList children) {
        super(ImmutableList.of(children.collect(Statement.class)), children.get(Expr.class));
        patternPrefixLength = this.getPrefixLength();
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnContextBlockExpression(this);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("{c#\n");
        for (int i = 0; i < statements.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append('\t').append(statements.get(i));
        }
        if (getValue() != null) {
            sb.append('\n').append('\t').append(getValue());
        }
        return sb.append("\n#c}").toString();
    }

    /// overrides the check of the superclass as unmatched elements will disappear in the suffix of
    /// this ContextStatementBlock
    @Override
    public boolean compatibleBlockSize(int pos, int max) {
        return true;
    }

    @Override
    public MatchConditions match(SourceData source, MatchConditions mc) {
        // TODO: 2nd use of blockBreakValue has wrong ctx!
        SourceData newSource = source;

        final RustyProgramElement src = newSource.getSource();
        final Services services = source.getServices();

        final PossibleProgramPrefix prefix;
        int pos = -1;
        PosInProgram relPos = PosInProgram.TOP;

        if (src instanceof PossibleProgramPrefix pre) {
            prefix = pre;
            final int srcPrefixLength = prefix.getPrefixLength();

            if (patternPrefixLength > srcPrefixLength) {
                return null;
            }

            pos = srcPrefixLength - patternPrefixLength;
            PossibleProgramPrefix firstActiveStatement = getPrefixElementAt(prefix, pos);

            relPos = firstActiveStatement.getFirstActiveChildPos();

            // firstActiveStatement contains the ProgramPrefix in front of the first active
            // statement
            // start denotes the child where to start to match
            // in some cases firstActiveStatement already denotes the element to match
            // (empty block, empty try block etc.) this is encoded by setting start to -1
            int start = -1;

            if (relPos != PosInProgram.TOP) {
                start = relPos.get(relPos.depth() - 1);
                if (relPos.depth() > 1) {
                    firstActiveStatement =
                        (PossibleProgramPrefix) PosInProgram.getProgramAt(relPos.up(),
                            firstActiveStatement);
                }
            }
            newSource = new SourceData(firstActiveStatement, start, services);
        } else {
            prefix = null;
        }

        // matching children
        mc = matchChildren(newSource, mc, 0);

        if (mc == null) {
            return null;
        }

        mc =
            makeContextInfoComplete(mc, newSource, prefix, pos, relPos, src, services);

        return mc;
    }

    /// completes match of context block by adding the prefix end position and the suffix start
    /// position
    private MatchConditions makeContextInfoComplete(MatchConditions matchCond, SourceData newSource,
            PossibleProgramPrefix prefix, int pos, PosInProgram relPos, RustyProgramElement src,
            Services services) {

        final SVInstantiations instantiations = matchCond.getInstantiations();

        final PosInProgram prefixEnd = matchPrefixEnd(prefix, pos, relPos);

        // compute position of the first element not matched
        final int lastMatchedPos = newSource.getChildPos();
        final PosInProgram suffixStart = prefixEnd.up().down(lastMatchedPos);

        /* add context block instantiation */
        matchCond = matchCond.setInstantiations(
            instantiations.replace(prefixEnd, suffixStart, src, services));
        return matchCond;
    }

    /// computes the PosInProgram of the first element, which is not part of the prefix
    ///
    /// @param prefix the ProgramPrefix the outermost prefix element of the source
    /// @param pos the number of elements to disappear in the context
    /// @param relPos the position of the first active statement of element
    ///        prefix.getPrefixElementAt(pos);
    /// @return the PosInProgram of the first element, which is not part of the prefix
    private PosInProgram matchPrefixEnd(final PossibleProgramPrefix prefix, int pos,
            PosInProgram relPos) {
        PosInProgram prefixEnd = PosInProgram.TOP;
        if (prefix != null) {
            PossibleProgramPrefix currentPrefix = prefix;
            int i = 0;
            while (i <= pos) {
                final IntIterator it = currentPrefix.getFirstActiveChildPos().iterator();
                while (it.hasNext()) {
                    prefixEnd = prefixEnd.down(it.next());
                }
                i++;
                if (i <= pos) {
                    // as fail-fast measure I do not test here using
                    // {@link ProgramPrefix#hasNextPrefixElement()}
                    // It must be guaranteed that there are at least pos + 1
                    // prefix elements (incl. prefix) otherwise there
                    // is a bug already at an earlier point
                    currentPrefix = currentPrefix.getNextPrefixElement();
                }
            }
        } else {
            prefixEnd = relPos;
        }
        return prefixEnd;
    }

    private static PossibleProgramPrefix getPrefixElementAt(PossibleProgramPrefix prefix, int i) {
        PossibleProgramPrefix current = prefix;
        for (int pos = 0; pos < i; pos++) {
            current = current.getNextPrefixElement();
        }
        return current;
    }
}
