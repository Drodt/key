/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java.statement;

import de.uka.ilkd.key.java.*;
import de.uka.ilkd.key.java.visitor.Visitor;
import de.uka.ilkd.key.logic.PosInProgram;
import de.uka.ilkd.key.logic.ProgramElementName;
import de.uka.ilkd.key.logic.ProgramPrefix;

import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

/**
 * Labeled statement.
 */
public class LabeledStatement extends JavaStatement
        implements StatementContainer, NamedProgramElement, ProgramPrefix {

    /**
     * Name.
     */
    protected final Label name;

    /**
     * Body.
     */
    protected final Statement body;


    private final PosInProgram firstActiveChildPos;

    private final int prefixLength;
    private final MethodFrame innerMostMethodFrame;

    /**
     * Constructor for the transformation of COMPOST ASTs to KeY.
     *
     * @param children the children of this AST element as KeY classes. May contain: a Label (as
     *        name of the label) a Statement (as body of the labeled statement) Comments
     */
    public LabeledStatement(ExtList children, Label label, PositionInfo pos) {
        super(children, pos);
        name = label;

        body = children.get(Statement.class);
        firstActiveChildPos = body instanceof StatementBlock
                ? ((StatementBlock) body).isEmpty() ? PosInProgram.TOP : PosInProgram.ONE_ZERO
                : PosInProgram.ONE;

        // otherwise it will crash later
        assert body != null;
        assert name != null;
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.getLength();
        innerMostMethodFrame = info.getInnerMostMethodFrame();

    }

    /**
     * Labeled statement.
     *
     * @param name an identifier.
     */
    public LabeledStatement(Label name) {
        this.name = name;
        body = new EmptyStatement();
        firstActiveChildPos = body instanceof StatementBlock
                ? (((StatementBlock) body).isEmpty() ? PosInProgram.TOP : PosInProgram.ONE_ZERO)
                : PosInProgram.ONE;
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.getLength();
        innerMostMethodFrame = info.getInnerMostMethodFrame();
    }

    /**
     * Labeled statement.
     *
     * @param id a Label.
     * @param statement a statement.
     */
    public LabeledStatement(Label id, Statement statement, PositionInfo pos) {
        super(pos);
        this.name = id;
        body = statement;
        firstActiveChildPos = body instanceof StatementBlock
                ? (((StatementBlock) body).isEmpty() ? PosInProgram.TOP : PosInProgram.ONE_ZERO)
                : PosInProgram.ONE;
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.getLength();
        innerMostMethodFrame = info.getInnerMostMethodFrame();
    }

    @Override
    public boolean hasNextPrefixElement() {
        if (body instanceof ProgramPrefix) {
            if (body instanceof StatementBlock) {
                return !((StatementBlock) body).isEmpty()
                        && ((StatementBlock) body).getStatementAt(0) instanceof ProgramPrefix;
            }
            return true;
        }
        return false;
    }

    @Override
    public ProgramPrefix getNextPrefixElement() {
        if (hasNextPrefixElement()) {
            return (ProgramPrefix) (body instanceof StatementBlock
                    ? ((StatementBlock) body).getStatementAt(0)
                    : body);
        } else {
            throw new IndexOutOfBoundsException("No next prefix element " + this);
        }
    }

    @Override
    public ProgramPrefix getLastPrefixElement() {
        return hasNextPrefixElement() ? getNextPrefixElement().getLastPrefixElement() : this;
    }

    @Override
    public ImmutableArray<ProgramPrefix> getPrefixElements() {
        if (body instanceof StatementBlock) {
            return StatementBlock.computePrefixElements(((StatementBlock) body).getBody(), this);
        } else if (body instanceof ProgramPrefix) {
            return StatementBlock.computePrefixElements(new ImmutableArray<>(body), this);
        }
        return new ImmutableArray<>(this);
    }


    public SourceElement getFirstElement() {
        if (body instanceof StatementBlock) {
            return body.getFirstElement();
        } else {
            return body;
        }
    }

    @Override
    public SourceElement getFirstElementIncludingBlocks() {
        if (body instanceof StatementBlock) {
            return body.getFirstElementIncludingBlocks();
        } else {
            return body;
        }
    }

    public SourceElement getLastElement() {
        if (body instanceof StatementBlock) {
            return body.getLastElement();
        } else {
            return body;
        }
    }


    /**
     * Get name.
     *
     * @return the string.
     */
    public final String getName() {
        return (name == null) ? null : name.toString();
    }

    /**
     * Get identifier.
     *
     * @return the identifier.
     */
    public Label getLabel() {
        return name;
    }


    /**
     * Get identifier.
     *
     * @return the identifier.
     */
    public ProgramElementName getProgramElementName() {
        if ((name instanceof ProgramElementName) || (name == null)) {
            return (ProgramElementName) name;
        }
        LOGGER.debug("labeledstatement: SCHEMAVARIABLE IN LABELEDSTATEMENT");
        return null;
    }

    /**
     * Get body.
     *
     * @return the statement.
     */
    public Statement getBody() {
        return body;
    }

    /**
     * Returns the number of children of this node.
     *
     * @return an int giving the number of children of this node
     */
    public int getChildCount() {
        int result = 0;
        if (name != null) {
            result++;
        }
        if (body != null) {
            result++;
        }
        return result;
    }

    /**
     * Returns the child at the specified index in this node's "virtual" child array
     *
     * @param index an index into this node's "virtual" child array
     * @return the program element at the given position
     * @exception ArrayIndexOutOfBoundsException if <tt>index</tt> is out of bounds
     */
    public ProgramElement getChildAt(int index) {
        if (name != null) {
            if (index == 0) {
                return name;
            }
            index--;
        }
        if (body != null) {
            if (index == 0) {
                return body;
            }
            index--;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * Get the number of statements in this container.
     *
     * @return the number of statements.
     */
    public int getStatementCount() {
        return (body != null) ? 1 : 0;
    }

    /**
     * Return the statement at the specified index in this node's "virtual" statement array.
     *
     * @param index an index for a statement.
     * @return the statement with the given index.
     * @exception ArrayIndexOutOfBoundsException if <tt>index</tt> is out of bounds.
     */
    public Statement getStatementAt(int index) {
        if (body != null && index == 0) {
            return body;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * calls the corresponding method of a visitor in order to perform some action/transformation on
     * this element
     *
     * @param v the Visitor
     */
    public void visit(Visitor v) {
        v.performActionOnLabeledStatement(this);
    }

    public PosInProgram getFirstActiveChildPos() {
        return firstActiveChildPos;
    }

    @Override
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public MethodFrame getInnerMostMethodFrame() {
        return innerMostMethodFrame;
    }

}
