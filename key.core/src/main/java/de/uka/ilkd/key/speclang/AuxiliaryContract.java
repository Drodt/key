/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.speclang;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import de.uka.ilkd.key.java.Label;
import de.uka.ilkd.key.java.LoopInitializer;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.SourceElement;
import de.uka.ilkd.key.java.StatementBlock;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.statement.Break;
import de.uka.ilkd.key.java.statement.Continue;
import de.uka.ilkd.key.java.statement.For;
import de.uka.ilkd.key.java.statement.JavaStatement;
import de.uka.ilkd.key.java.statement.LabelJumpStatement;
import de.uka.ilkd.key.java.statement.LabeledStatement;
import de.uka.ilkd.key.java.statement.LoopStatement;
import de.uka.ilkd.key.java.visitor.OuterBreakContinueAndReturnCollector;
import de.uka.ilkd.key.java.visitor.ProgramVariableCollector;
import de.uka.ilkd.key.java.visitor.Visitor;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.IObserverFunction;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.JModality;
import de.uka.ilkd.key.logic.op.LocationVariable;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.rule.AuxiliaryContractBuilders;
import de.uka.ilkd.key.speclang.Contract.OriginalVariables;
import de.uka.ilkd.key.util.InfFlowSpec;
import de.uka.ilkd.key.util.MiscTools;

import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.ImmutableSet;

/**
 * Super-interface for {@link BlockContract} and {@link LoopContract}.
 *
 * @author wacker, lanzinger
 */
public interface AuxiliaryContract extends SpecificationElement {

    /**
     *
     * @return all {@link FunctionalAuxiliaryContract}s with a valid id that correspond to this
     *         {@code BlockSpecificationElement}. Unless this contract is a combination of other
     *         contracts, the resulting set will only contain one element.
     */
    ImmutableSet<FunctionalAuxiliaryContract<?>> getFunctionalContracts();

    /**
     *
     * @param contract the new functional contract.
     * @see #getFunctionalContracts()
     */
    void setFunctionalContract(FunctionalAuxiliaryContract<?> contract);

    /**
     *
     * @return the block this contract belongs to.
     */
    StatementBlock getBlock();

    /**
     *
     * @return all labels belonging to {@link #getBlock()}
     */
    List<Label> getLabels();

    /**
     *
     * @return the method containing {@link #getBlock()}
     */
    IProgramMethod getMethod();

    /**
     * @return this contract's modality.
     */
    JModality.JavaModalityKind getModalityKind();

    /**
     * Returns the set of placeholder variables created during this contract's instantiation. These
     * are replaced by the real variables with the same names when the contract is applied.
     *
     * @return the placeholder variables used created during this contracts instantiation.
     * @see AuxiliaryContractBuilders.VariablesCreatorAndRegistrar
     */
    Variables getPlaceholderVariables();

    /**
     *
     * @return {@code true} if and only if this contract is applicable for transactions.
     */
    boolean isTransactionApplicable();

    /**
     *
     * @param services services.
     * @return {@code true} if and only if this contract is read-only.
     */
    boolean isReadOnly(Services services);

    /**
     *
     * @return this contract's base name.
     */
    String getBaseName();

    /**
     *
     * @return this contract's unique name.
     */
    String getUniqueName();

    /**
     * Returns <code>true</code> iff the method (according to the contract) does not modify the heap
     * at all, i.e., iff it is "strictly pure."
     *
     * @param heap the heap to use.
     * @return whether this contract is strictly pure.
     */
    boolean hasModifiableClause(LocationVariable heap);

    /**
     * Returns <code>true</code> iff the method (according to the free part of the contract)
     * does not modify the heap at all, i.e., iff it is "strictly pure."
     *
     * @param heap
     *        the heap to use.
     * @return whether this contract is strictly pure.
     */
    boolean hasFreeModifiableClause(LocationVariable heap);

    /**
     *
     * @return the {@code self} variable as a term.
     */
    JTerm getInstantiationSelfTerm();

    /**
     *
     * @param heap the heap to use.
     * @param services services.
     * @return this contract's precondition on the specified heap.
     */
    JTerm getPrecondition(LocationVariable heap, Services services);

    /**
     *
     * @param heap the heap to use.
     * @param variables the variables to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's precondition on the specified heap with all free program variables
     *         replaced by those in {@code variables}.
     */
    JTerm getPrecondition(LocationVariable heap, Variables variables, Services services);

    /**
     *
     * @param heap the heap to use.
     * @param self the {@code self} variable to use instead of {@link #getPlaceholderVariables()}.
     * @param atPres a map from every variable {@code var} to {@code \old(var)} to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's precondition on the specified heap.
     */
    JTerm getPrecondition(LocationVariable heap, LocationVariable self,
            Map<LocationVariable, LocationVariable> atPres, Services services);

    /**
     *
     * @param heapVariable the heap to use.
     * @param heap the heap to use.
     * @param self the {@code self} variable to use to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param atPres a map from every variable {@code var} to {@code \old(var)} to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's precondition on the specified heap.
     */
    JTerm getPrecondition(LocationVariable heapVariable, JTerm heap, JTerm self,
            Map<LocationVariable, JTerm> atPres, Services services);

    /**
     *
     * @param heapVariable the heap to use.
     * @param heap the heap to use.
     * @param terms the terms to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's precondition on the specified heap.
     */
    JTerm getPrecondition(LocationVariable heapVariable, JTerm heap, Terms terms,
            Services services);

    /**
     *
     * @param heap the heap to use.
     * @param variables the variables to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's postcondition on the specified heap.
     */
    JTerm getPostcondition(LocationVariable heap, Variables variables, Services services);

    /**
     *
     * @param heapVariable the heap to use.
     * @param heap the heap to use.
     * @param terms the terms to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's precondition on the specified heap.
     */
    JTerm getPostcondition(LocationVariable heapVariable, JTerm heap, Terms terms,
            Services services);

    /**
     *
     * @param heap the heap to use.
     * @param services services.
     * @return this contract's precondition on the specified heap.
     */
    JTerm getPostcondition(LocationVariable heap, Services services);



    /**
     *
     * @param heap the heap to use.
     * @param services services.
     * @return this contract's free precondition on the specified heap.
     */
    JTerm getFreePrecondition(LocationVariable heap, Services services);

    /**
     *
     * @param heap the heap to use.
     * @param variables the variables to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's free precondition on the specified heap with all free program
     *         variables replaced by those in {@code variables}.
     */
    JTerm getFreePrecondition(LocationVariable heap, Variables variables, Services services);

    /**
     *
     * @param heap the heap to use.
     * @param self the {@code self} variable to use instead of {@link #getPlaceholderVariables()}.
     * @param atPres a map from every variable {@code var} to {@code \old(var)} to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's free precondition on the specified heap.
     */
    JTerm getFreePrecondition(LocationVariable heap, LocationVariable self,
            Map<LocationVariable, LocationVariable> atPres, Services services);

    /**
     *
     * @param heapVariable the heap to use.
     * @param heap the heap to use.
     * @param self the {@code self} variable to use to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param atPres a map from every variable {@code var} to {@code \old(var)} to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's free precondition on the specified heap.
     */
    JTerm getFreePrecondition(LocationVariable heapVariable, JTerm heap, JTerm self,
            Map<LocationVariable, JTerm> atPres, Services services);

    /**
     *
     * @param heapVariable the heap to use.
     * @param heap the heap to use.
     * @param terms the terms to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's free precondition on the specified heap.
     */
    JTerm getFreePrecondition(LocationVariable heapVariable, JTerm heap, Terms terms,
            Services services);

    /**
     *
     * @param heap the heap to use.
     * @param variables the variables to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's free postcondition on the specified heap.
     */
    JTerm getFreePostcondition(LocationVariable heap, Variables variables, Services services);

    /**
     *
     * @param heapVariable the heap to use.
     * @param heap the heap to use.
     * @param terms the terms to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's free precondition on the specified heap.
     */
    JTerm getFreePostcondition(LocationVariable heapVariable, JTerm heap, Terms terms,
            Services services);

    /**
     *
     * @param heap the heap to use.
     * @param services services.
     * @return this contract's free precondition on the specified heap.
     */
    JTerm getFreePostcondition(LocationVariable heap, Services services);

    /**
     *
     * @param heap the heap to use.
     * @param self the {@code self} variable to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's modifiable clause on the specified heap.
     */
    JTerm getModifiableClause(LocationVariable heap, LocationVariable self, Services services);

    /**
     *
     * @param heapVariable the heap to use.
     * @param heap the heap to use.
     * @param self the {@code self} variable to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's modifiable clause on the specified heap.
     */
    JTerm getModifiableClause(LocationVariable heapVariable, JTerm heap, JTerm self,
            Services services);

    /**
     *
     * @param heap the heap to use.
     * @param variables the variables to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's modifiable clause on the specified heap.
     */
    JTerm getModifiableClause(LocationVariable heap, Variables variables, Services services);

    /**
     *
     * @param heap the heap to use.
     * @param services services.
     * @return this contract's modifiable clause on the specified heap.
     */
    JTerm getModifiableClause(LocationVariable heap, Services services);

    /**
     *
     * @param heap
     *        the heap to use.
     * @param self
     *        the {@code self} variable to use instead of {@link #getPlaceholderVariables()}.
     * @param services
     *        services.
     * @return this contract's free modifiable clause on the specified heap.
     */
    JTerm getFreeModifiableClause(LocationVariable heap, LocationVariable self, Services services);

    /**
     *
     * @param heapVariable
     *        the heap to use.
     * @param heap
     *        the heap to use.
     * @param self
     *        the {@code self} variable to use instead of {@link #getPlaceholderVariables()}.
     * @param services
     *        services.
     * @return this contract's free modifiable clause on the specified heap.
     */
    JTerm getFreeModifiableClause(LocationVariable heapVariable, JTerm heap, JTerm self,
            Services services);

    /**
     *
     * @param heap
     *        the heap to use.
     * @param variables
     *        the variables to use instead of {@link #getPlaceholderVariables()}.
     * @param services
     *        services.
     * @return this contract's free modifiable clause on the specified heap.
     */
    JTerm getFreeModifiableClause(LocationVariable heap, Variables variables, Services services);

    /**
     *
     * @param heap
     *        the heap to use.
     * @param services
     *        services.
     * @return this contract's free modifiable clause on the specified heap.
     */
    JTerm getFreeModifiableClause(LocationVariable heap, Services services);


    /**
     *
     * @param heap the heap to use.
     * @return this contract's precondition on the specified heap.
     */
    JTerm getRequires(LocationVariable heap);

    /**
     *
     * @param heap the heap to use.
     * @return this contract's free precondition on the specified heap.
     */
    JTerm getRequiresFree(LocationVariable heap);

    /**
     *
     * @param heap the heap to use.
     * @return this contract's postcondition on the specified heap.
     */
    JTerm getEnsures(LocationVariable heap);

    /**
     *
     * @param heap the heap to use.
     * @return this contract's free postcondition on the specified heap.
     */
    JTerm getEnsuresFree(LocationVariable heap);

    /**
     *
     * @param heap the heap to use.
     * @return this contract's modifiable term on the specified heap.
     */
    JTerm getModifiable(LocationVariable heap);

    /**
     * Accepts a visitor.
     *
     * @param visitor the visitor to accept.
     */
    void visit(Visitor visitor);

    /**
     *
     * @param services services.
     * @return a HTML representation of this contract.
     */
    String getHtmlText(Services services);

    /**
     *
     * @param services services.
     * @return a plain text representation of this contract.
     */
    String getPlainText(Services services);

    /**
     *
     * @param services services.
     * @param terms the terms to use instead of {@link #getPlaceholderVariables()}.
     * @return a plain text representation of this contract.
     */
    String getPlainText(Services services, Terms terms);

    /**
     * @return the method in which the block is located.
     */
    IProgramMethod getTarget();

    /**
     *
     * @return {@code true} if and only if this contract has a measured-by clause.
     * @see #getMby()
     */
    boolean hasMby();

    /**
     *
     * @return this contract's measured-by clause if it has one, {@code null} otherwise.
     */
    JTerm getMby();

    /**
     *
     * @param variables variables to use instead of {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's measured-by clause if it has one, {@code null} otherwise.
     */
    JTerm getMby(Variables variables, Services services);

    /**
     *
     * @param selfVar the {@code self} variable to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's measured-by clause if it has one, {@code null} otherwise.
     */
    JTerm getMby(LocationVariable selfVar, Services services);

    /**
     *
     * @param heapTerms the heaps to use.
     * @param selfTerm the {@code self} variable to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param atPres a map from every variable {@code var} to {@code \old(var)} to use instead of
     *        {@link #getPlaceholderVariables()}.
     * @param services services.
     * @return this contract's measured-by clause if it has one, {@code null} otherwise.
     */
    JTerm getMby(Map<LocationVariable, JTerm> heapTerms, JTerm selfTerm,
            Map<LocationVariable, JTerm> atPres, Services services);

    /**
     *
     * @return {@code true} if and only if this contract has information flow specs.
     * @see #getInfFlowSpecs()
     */
    boolean hasInfFlowSpecs();

    /**
     *
     * @param selfInstantiation the new instantiation self term.
     * @see #getInstantiationSelfTerm()
     */
    void setInstantiationSelf(JTerm selfInstantiation);

    /**
     * @param services services.
     * @return the term internally used for self or a newly instantiated one. Use with care - it is
     *         likely that this is *not* the right "self" for you.
     */
    JTerm getInstantiationSelfTerm(TermServices services);

    /**
     * @param services services.
     * @return the original precondition of the contract.
     */
    JTerm getPre(Services services);

    /**
     * @param services services.
     * @return the original postcondition of the contract.
     */
    JTerm getPost(Services services);

    /**
     * @param services services.
     * @return the original free precondition of the contract.
     */
    JTerm getFreePre(Services services);

    /**
     * @param services services.
     * @return the original free postcondition of the contract.
     */
    JTerm getFreePost(Services services);

    /**
     * @param services services.
     * @return the original modifiable clause of the contract.
     */
    JTerm getModifiable(Services services);

    /**
     * @return the original information flow specification clause of the contract.
     */
    ImmutableList<InfFlowSpec> getInfFlowSpecs();

    /**
     * @return the original used variables like self, result etc..
     */
    Variables getVariables();

    /**
     *
     * @param newKJT the type containing the new target method.
     * @param newPM the new target method.
     * @return a contract equal to this one except that it belongs to a different target.
     */

    AuxiliaryContract setTarget(KeYJavaType newKJT, IObserverFunction newPM);

    /**
     *
     * @param newBlock the new block.
     * @return a contract equal to this one except that it belongs to a different block.
     */
    AuxiliaryContract setBlock(StatementBlock newBlock);

    /**
     * @param services services.
     * @return the original used variables like self, result etc. as terms.
     */
    Terms getVariablesAsTerms(Services services);

    /**
     *
     * @return the contract's variabels converted to {@code OriginalVariables}.
     * @see #getVariables()
     */
    OriginalVariables getOrigVars();

    @Override
    AuxiliaryContract map(UnaryOperator<JTerm> op, Services services);

    /**
     * This class contains all new variables that are introduced during a
     * {@link AuxiliaryContract}'s instantiation.
     */
    class Variables {

        /**
         * {@code self}
         */
        public final LocationVariable self;

        /**
         * Boolean flags that are set to {@code true} when the block terminates by a
         * {@code break label;} statement with the specified label.
         */
        public final Map<Label, LocationVariable> breakFlags;

        /**
         * Boolean flags that are set to {@code true} when the block terminates by a
         * {@code continue label;} statement with the specified label.
         */
        public final Map<Label, LocationVariable> continueFlags;

        /**
         * Boolean flag that is set to {@code true} when the block terminates by a {@code return}
         * statement.
         */
        public final LocationVariable returnFlag;

        /**
         * Result variable to set when the block terminates by a {@code return} statement.
         */
        public final LocationVariable result;

        /**
         * Exception variable to set when the block terminates by an uncaught {@code throw}
         * statement.
         */
        public final LocationVariable exception;

        /**
         * A map from every heap {@code heap} to {@code heap_Before_BLOCK}.
         */
        public final Map<LocationVariable, LocationVariable> remembranceHeaps;

        /**
         * A map from every variable {@code var} that is modifiable inside the block to
         * {@code var_Before_BLOCK}.
         */
        public final Map<LocationVariable, LocationVariable> remembranceLocalVariables;

        /**
         * A map from every heap {@code heap} that is accessible inside the block to
         * {@code heap_Before_METHOD}.
         */
        public final Map<LocationVariable, LocationVariable> outerRemembranceHeaps;

        /**
         * A map from every variable {@code var} that is accessible inside the block to
         * {@code var_Before_METHOD}.
         */
        public final Map<LocationVariable, LocationVariable> outerRemembranceVariables;

        /**
         * Services.
         */
        private final TermServices services;

        /**
         * creates an auxiliary contract
         * You should use the suitable `create` method instead of this constructor.
         *
         * @param self {@code self}
         * @param breakFlags boolean flags that are set to {@code true} when the block terminates by
         *        a {@code break label;} statement with the specified label.
         * @param continueFlags boolean flags that are set to {@code true} when the block terminates
         *        by a {@code continue label;} statement with the specified label.
         * @param returnFlag boolean flag that is set to {@code true} when the block terminates by a
         *        {@code return} statement.
         * @param result result variable to set when the block terminates by a {@code return}
         *        statement.
         * @param exception exception variable to set when the block terminates by an uncaught
         *        {@code throw} statement.
         * @param remembranceHeaps a map from every heap {@code heap} to {@code heap_Before_BLOCK}.
         * @param remembranceLocalVariables a map from every variable {@code var} that is modifiable
         *        inside the block to {@code var_Before_BLOCK}.
         * @param outerRemembranceHeaps a map from every heap {@code heap} that is accessible inside
         *        the block to {@code heap_Before_METHOD}.
         * @param outerRemembranceVariables a map from every variable {@code var} that is accessible
         *        inside the block to {@code var_Before_METHOD}.
         * @param services services.
         */
        public Variables(final LocationVariable self, final Map<Label, LocationVariable> breakFlags,
                final Map<Label, LocationVariable> continueFlags, final LocationVariable returnFlag,
                final LocationVariable result, final LocationVariable exception,
                final Map<LocationVariable, LocationVariable> remembranceHeaps,
                final Map<LocationVariable, LocationVariable> remembranceLocalVariables,
                final Map<LocationVariable, LocationVariable> outerRemembranceHeaps,
                final Map<LocationVariable, LocationVariable> outerRemembranceVariables,
                final TermServices services) {
            this.services = services;
            this.self = self;
            this.breakFlags = breakFlags;
            this.continueFlags = continueFlags;
            this.returnFlag = returnFlag;
            this.result = result;
            this.exception = exception;
            this.remembranceHeaps = remembranceHeaps;
            this.remembranceLocalVariables = remembranceLocalVariables;
            this.outerRemembranceHeaps = outerRemembranceHeaps;
            this.outerRemembranceVariables = outerRemembranceVariables;
        }

        /**
         * Creates a new instance.
         *
         * @param block the block for which this instance is created.
         * @param labels all labels that belong to the block.
         * @param method the method containing the block.
         * @param services services.
         * @return a new instance.
         */
        public static Variables create(final StatementBlock block, final List<Label> labels,
                final IProgramMethod method, final Services services) {
            return new VariablesCreator(block, labels, method, services).create();
        }

        /**
         * Creates a new instance.
         *
         * @param loop the loop for which this instance is created.
         * @param labels all labels that belong to the block.
         * @param method the method containing the block.
         * @param services services.
         * @return a new instance.
         */
        public static Variables create(final LoopStatement loop, final List<Label> labels,
                final IProgramMethod method, final Services services) {
            return new VariablesCreator(loop, labels, method, services).create();
        }

        /**
         *
         * @return the union of {@link #remembranceHeaps} and {@link #remembranceLocalVariables}.
         */
        public Map<LocationVariable, LocationVariable> combineRemembranceVariables() {
            final Map<LocationVariable, LocationVariable> result =
                new LinkedHashMap<>();
            result.putAll(remembranceHeaps);
            result.putAll(remembranceLocalVariables);
            return result;
        }

        /**
         *
         * @return the union of {@link #outerRemembranceHeaps} and
         *         {@link #outerRemembranceVariables}.
         */
        public Map<LocationVariable, LocationVariable> combineOuterRemembranceVariables() {
            final Map<LocationVariable, LocationVariable> result =
                new LinkedHashMap<>();
            result.putAll(outerRemembranceHeaps);
            result.putAll(outerRemembranceVariables);
            return result;
        }

        /**
         *
         * @param self the {@code self} term to use.
         * @return a {@code Terms} object containing these variables in term form.
         */
        public Terms termify(JTerm self) {
            return new Terms(self, termifyFlags(breakFlags), termifyFlags(continueFlags),
                termifyVariable(returnFlag), termifyVariable(result), termifyVariable(exception),
                termifyRemembranceVariables(remembranceHeaps),
                termifyRemembranceVariables(remembranceLocalVariables),
                termifyRemembranceVariables(outerRemembranceHeaps),
                termifyRemembranceVariables(outerRemembranceVariables));
        }

        /**
         *
         * @param flags a map containing the variables to termify.
         * @return a map with all the same keys with termified values.
         */
        private Map<Label, JTerm> termifyFlags(final Map<Label, LocationVariable> flags) {
            final Map<Label, JTerm> result = new LinkedHashMap<>();
            for (Map.Entry<Label, LocationVariable> flag : flags.entrySet()) {
                result.put(flag.getKey(), termifyVariable(flag.getValue()));
            }
            return result;
        }

        /**
         *
         * @param remembranceVariables a map containing the variables to termify.
         * @return a map with all the same keys with termified values.
         */
        private Map<LocationVariable, JTerm> termifyRemembranceVariables(
                final Map<LocationVariable, LocationVariable> remembranceVariables) {
            final Map<LocationVariable, JTerm> result = new LinkedHashMap<>();
            for (Map.Entry<LocationVariable, LocationVariable> remembranceVariable : remembranceVariables
                    .entrySet()) {
                result.put(remembranceVariable.getKey(),
                    termifyVariable(remembranceVariable.getValue()));
            }
            return result;
        }

        /**
         *
         * @param variable a variable.
         * @return a term containing the specified variable.
         */
        private JTerm termifyVariable(final LocationVariable variable) {
            if (variable != null) {
                return services.getTermBuilder().var(variable);
            } else {
                return null;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((breakFlags == null) ? 0 : breakFlags.hashCode());
            result = prime * result + ((continueFlags == null) ? 0 : continueFlags.hashCode());
            result = prime * result + ((exception == null) ? 0 : exception.hashCode());
            result =
                prime * result + ((remembranceHeaps == null) ? 0 : remembranceHeaps.hashCode());
            result = prime * result + ((remembranceLocalVariables == null) ? 0
                    : remembranceLocalVariables.hashCode());
            result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
            result = prime * result + ((returnFlag == null) ? 0 : returnFlag.hashCode());
            result = prime * result + ((self == null) ? 0 : self.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Variables other = (Variables) obj;
            if (breakFlags == null && other.breakFlags != null) {
                return false;
            } else if (breakFlags != null && !breakFlags.equals(other.breakFlags)) {
                return false;
            } else if (continueFlags == null && other.continueFlags != null) {
                return false;
            } else if (continueFlags != null && !continueFlags.equals(other.continueFlags)) {
                return false;
            } else if (exception == null && other.exception != null) {
                return false;
            } else if (exception != null && !exception.equals(other.exception)) {
                return false;
            } else if (remembranceHeaps == null && other.remembranceHeaps != null) {
                return false;
            } else if (remembranceHeaps != null
                    && !remembranceHeaps.equals(other.remembranceHeaps)) {
                return false;
            } else if (remembranceLocalVariables == null
                    && other.remembranceLocalVariables != null) {
                return false;
            } else if (remembranceLocalVariables != null
                    && !remembranceLocalVariables.equals(other.remembranceLocalVariables)) {
                return false;
            } else if (outerRemembranceVariables == null
                    && other.outerRemembranceVariables != null) {
                return false;
            } else if (outerRemembranceVariables != null
                    && !outerRemembranceVariables.equals(other.outerRemembranceVariables)) {
                return false;
            } else if (result == null && other.result != null) {
                return false;
            } else if (result != null && !result.equals(other.result)) {
                return false;
            } else if (returnFlag == null && other.returnFlag != null) {
                return false;
            } else if (returnFlag != null && !returnFlag.equals(other.returnFlag)) {
                return false;
            } else if (self == null && other.self != null) {
                return false;
            } else {
                return self == null || self.equals(other.self);
            }
        }

        /**
         *
         * @return a conversion of this object to {@code OriginalVariables}.
         */
        public OriginalVariables toOrigVars() {
            Map<LocationVariable, LocationVariable> atPreVars =
                new LinkedHashMap<>();
            for (LocationVariable h : remembranceLocalVariables.keySet()) {
                atPreVars.put(h, remembranceLocalVariables.get(h));
            }
            return new OriginalVariables(self, result, exception, atPreVars,
                ImmutableSLList.nil());
        }

    }

    /**
     * @see Variables#create(StatementBlock, List, IProgramMethod, Services)
     */
    class VariablesCreator extends TermBuilder {

        /**
         * Base name for all break flags.
         *
         * @see Variables#breakFlags
         */
        public static final String BREAK_FLAG_BASE_NAME = "broke";

        /**
         * Base name for all continue flags.
         *
         * @see Variables#continueFlags
         */
        public static final String CONTINUE_FLAG_BASE_NAME = "continued";

        /**
         * Name for the return flag.
         *
         * @see Variables#returnFlag
         */
        public static final String RETURN_FLAG_NAME = "returned";

        /**
         * Infix used between a flag's base name and the label name.
         *
         * @see Variables#breakFlags
         * @see Variables#continueFlags
         */
        public static final String FLAG_INFIX = "To";

        /**
         * Suffix for all remembrance variables.
         *
         * @see Variables#remembranceHeaps
         * @see Variables#remembranceLocalVariables
         */
        public static final String REMEMBRANCE_SUFFIX = "_Before_BLOCK";

        /**
         * Suffix for all outer remembrance variables.
         *
         * @see Variables#outerRemembranceHeaps
         * @see Variables#outerRemembranceVariables
         */
        public static final String OUTER_REMEMBRANCE_SUFFIX = "_Before_METHOD";

        /**
         * @see AuxiliaryContract#getBlock()
         * @see LoopContract#getLoop()
         */
        private final JavaStatement statement;

        /**
         * @see AuxiliaryContract#getLabels()
         */
        private final List<Label> labels;

        /**
         * @see AuxiliaryContract#getMethod()
         */
        private final IProgramMethod method;

        /**
         * @see Variables#breakFlags
         */
        private Map<Label, LocationVariable> breakFlags;

        /**
         * @see Variables#continueFlags
         */
        private Map<Label, LocationVariable> continueFlags;

        /**
         * @see Variables#returnFlag
         */
        private LocationVariable returnFlag;

        /**
         * Constructor.
         *
         * @param statement the block or loop the contract belongs to.
         * @param labels all labels belonging to the block.
         * @param method the method containing the block.
         * @param services services.
         */
        public VariablesCreator(final JavaStatement statement, final List<Label> labels,
                final IProgramMethod method, final Services services) {
            super(services.getTermFactory(), services);

            this.statement = statement;
            this.labels = labels;
            this.method = method;
        }

        /**
         *
         * @return a new {@code Variables} object.
         */
        public Variables create() {
            createAndStoreFlags();

            return new Variables(selfVar(method, method.getContainerType(), false), breakFlags,
                continueFlags, returnFlag,
                // We don't need to (and shouldn't) make the variable names unique here.
                // That is done in AuxiliaryContractBuilders.VariablesCreatorAndRegistrar
                // when the contract is applied.
                resultVar(method, false), excVar(method, false), createRemembranceHeaps(),
                createRemembranceLocalVariables(), createOuterRemembranceHeaps(),
                createOuterRemembranceLocalVariables(), services);
        }

        /**
         * Creates all flags.
         *
         * @see #breakFlags
         * @see #continueFlags
         * @see #returnFlag
         */
        private void createAndStoreFlags() {
            final OuterBreakContinueAndReturnCollector collector =
                new OuterBreakContinueAndReturnCollector(statement, labels, services);
            collector.collect();

            final List<Break> breaks = collector.getBreaks();
            final List<Continue> continues = collector.getContinues();
            final boolean returnOccurred = collector.hasReturns();

            final Set<Label> breakLabels = collectLabels(breaks);
            final Set<Label> continueLabels = collectLabels(continues);

            breakFlags = createFlags(breakLabels, BREAK_FLAG_BASE_NAME);
            continueFlags = createFlags(continueLabels, CONTINUE_FLAG_BASE_NAME);
            returnFlag = returnOccurred ? createFlag(RETURN_FLAG_NAME) : null;
        }

        /**
         *
         * @param jumps a list of jump statements.
         * @return the label of every labeled jump statement contained in the specified list.
         */
        private Set<Label> collectLabels(final List<? extends LabelJumpStatement> jumps) {
            final Set<Label> result = new LinkedHashSet<>();
            for (LabelJumpStatement jump : jumps) {
                result.add(jump.getLabel());
            }
            return result;
        }

        /**
         * Creates flags for the specified labels
         *
         * @param labels the labels.
         * @param baseName the base name for the flags.
         * @return
         */
        private Map<Label, LocationVariable> createFlags(final Set<Label> labels,
                final String baseName) {
            final Map<Label, LocationVariable> result = new LinkedHashMap<>();
            for (Label label : labels) {
                final String suffix = label == null ? "" : FLAG_INFIX + label;
                result.put(label, createFlag(baseName + suffix));
            }
            return result;
        }

        /**
         *
         * @param name a name.
         * @return a boolean variable with the specified name.
         */
        private LocationVariable createFlag(final String name) {
            return createVariable(name, services.getJavaInfo().getKeYJavaType("boolean"));
        }

        /**
         *
         * @return a map from every heap to its remembrance heap.
         * @see Variables#remembranceHeaps
         */
        private Map<LocationVariable, LocationVariable> createRemembranceHeaps() {
            return createRemembranceHeaps(REMEMBRANCE_SUFFIX);
        }

        /**
         *
         * @param suffix the suffix to use for the remembrance heaps.
         * @return a map from every heap to a remembrance heap.
         * @see Variables#remembranceHeaps
         * @see Variables#outerRemembranceHeaps
         */
        private Map<LocationVariable, LocationVariable> createRemembranceHeaps(String suffix) {
            final Map<LocationVariable, LocationVariable> result =
                new LinkedHashMap<>();
            for (LocationVariable heap : services.getTypeConverter().getHeapLDT().getAllHeaps()) {
                result.put(heap, locationVariable(heap + suffix, heap.sort(), true));
            }
            return result;
        }

        /**
         *
         * @return a map from every variable to its remembrance variable.
         * @see Variables#remembranceLocalVariables
         */
        private Map<LocationVariable, LocationVariable> createRemembranceLocalVariables() {
            ImmutableSet<LocationVariable> localOutVariables =
                MiscTools.getLocalOuts(statement, services);

            SourceElement first;
            if (statement instanceof LabeledStatement) {
                // statement is a labeled loop.
                first = statement;
            } else {
                // statement is a block starting with a (maybe labeled) loop.
                first = statement.getFirstElement();
            }

            while (first instanceof LabeledStatement s) {
                first = s.getBody();
            }

            if (first instanceof For) {
                ImmutableArray<LoopInitializer> inits = ((For) first).getInitializers();
                ProgramVariableCollector collector =
                    new ProgramVariableCollector(new StatementBlock(inits), services);
                collector.start();

                for (LocationVariable var : collector.result()) {
                    if (!var.getKeYJavaType().equals(
                        services.getTypeConverter().getHeapLDT().getHeap().getKeYJavaType())) {
                        localOutVariables = localOutVariables.add(var);
                    }
                }
            }

            Map<LocationVariable, LocationVariable> result =
                new LinkedHashMap<>();

            for (ProgramVariable var : localOutVariables) {
                result.put((LocationVariable) var,
                    createVariable(var.name() + REMEMBRANCE_SUFFIX, var.getKeYJavaType()));
            }
            return result;
        }

        /**
         *
         * @return a map from every heap to its outer remembrance heap.
         * @see Variables#outerRemembranceHeaps
         */
        private Map<LocationVariable, LocationVariable> createOuterRemembranceHeaps() {
            return createRemembranceHeaps(OUTER_REMEMBRANCE_SUFFIX);
        }

        /**
         *
         * @return a map from every variable to its outer remembrance variable.
         * @see Variables#outerRemembranceVariables
         */
        private Map<LocationVariable, LocationVariable> createOuterRemembranceLocalVariables() {
            ImmutableSet<LocationVariable> localInVariables =
                MiscTools.getLocalIns(statement, services);

            SourceElement first;

            if (statement instanceof LoopStatement) {
            } else {
                first = statement.getFirstElement();
                while (first instanceof LabeledStatement s) {
                    first = s.getBody();
                }

                if (first instanceof For) {
                    ImmutableArray<LoopInitializer> inits = ((For) first).getInitializers();
                    ProgramVariableCollector collector =
                        new ProgramVariableCollector(new StatementBlock(inits), services);
                    collector.start();

                    for (LocationVariable var : collector.result()) {
                        if (!var.getKeYJavaType().equals(
                            services.getTypeConverter().getHeapLDT().getHeap().getKeYJavaType())) {
                            localInVariables = localInVariables.add(var);
                        }
                    }
                }
            }



            Map<LocationVariable, LocationVariable> result =
                new LinkedHashMap<>();

            for (LocationVariable var : localInVariables) {
                result.put(var,
                    createVariable(var.name() + OUTER_REMEMBRANCE_SUFFIX, var.getKeYJavaType()));
            }
            return result;
        }

        /**
         *
         * @param name a base name.
         * @param type a type.
         * @return a variable with a name based on the specified base name of the specified type.
         */
        private LocationVariable createVariable(final String name, final KeYJavaType type) {
            return new LocationVariable(services.getVariableNamer().getTemporaryNameProposal(name),
                type);
        }
    }

    /**
     * @see Variables#termify(JTerm)
     */
    class Terms {

        /**
         * @see Variables#self
         */
        public final JTerm self;

        /**
         * @see Variables#breakFlags
         */
        public final Map<Label, JTerm> breakFlags;

        /**
         * @see Variables#continueFlags
         */
        public final Map<Label, JTerm> continueFlags;

        /**
         * @see Variables#returnFlag
         */
        public final JTerm returnFlag;

        /**
         * @see Variables#result
         */
        public final JTerm result;

        /**
         * @see Variables#exception
         */
        public final JTerm exception;

        /**
         * @see Variables#remembranceHeaps
         */
        public final Map<LocationVariable, JTerm> remembranceHeaps;

        /**
         * @see Variables#remembranceLocalVariables
         */
        public final Map<LocationVariable, JTerm> remembranceLocalVariables;

        /**
         * @see Variables#outerRemembranceHeaps
         */
        public final Map<LocationVariable, JTerm> outerRemembranceHeaps;

        /**
         * @see Variables#outerRemembranceVariables
         */
        public final Map<LocationVariable, JTerm> outerRemembranceVariables;

        /**
         * Creates a new instance. In most cases, {@link Variables#termify(JTerm)} or
         * {@link Terms#Terms(Variables, TermBuilder)} should be used instead of this.
         *
         * @param self {@code self}
         * @param breakFlags boolean flags that are set to {@code true} when the block terminates by
         *        a {@code break label;} statement with the specified label.
         * @param continueFlags boolean flags that are set to {@code true} when the block terminates
         *        by a {@code continue label;} statement with the specified label.
         * @param returnFlag boolean flag that is set to {@code true} when the block terminates by a
         *        {@code return} statement.
         * @param result result variable to set when the block terminates by a {@code return}
         *        statement.
         * @param exception exception variable to set when the block terminates by an uncaught
         *        {@code throw} statement.
         * @param remembranceHeaps a map from every heap {@code heap} to {@code heap_Before_BLOCK}.
         * @param remembranceLocalVariables a map from every variable {@code var} that is modifiable
         *        inside the block to {@code var_Before_BLOCK}.
         * @param outerRemembranceHeaps a map from every heap {@code heap} that is accessible inside
         *        the block to {@code heap_Before_METHOD}.
         * @param outerRemembranceVariables a map from every variable {@code var} that is accessible
         *        inside the block to {@code var_Before_METHOD}.
         */
        public Terms(final JTerm self, final Map<Label, JTerm> breakFlags,
                final Map<Label, JTerm> continueFlags, final JTerm returnFlag, final JTerm result,
                final JTerm exception, final Map<LocationVariable, JTerm> remembranceHeaps,
                final Map<LocationVariable, JTerm> remembranceLocalVariables,
                final Map<LocationVariable, JTerm> outerRemembranceHeaps,
                final Map<LocationVariable, JTerm> outerRemembranceVariables) {
            this.self = self;
            this.breakFlags = breakFlags;
            this.continueFlags = continueFlags;
            this.returnFlag = returnFlag;
            this.result = result;
            this.exception = exception;
            this.remembranceHeaps = remembranceHeaps;
            this.remembranceLocalVariables = remembranceLocalVariables;
            this.outerRemembranceHeaps = outerRemembranceHeaps;
            this.outerRemembranceVariables = outerRemembranceVariables;
        }

        /**
         *
         * @param variables the variables to termify.
         * @param tb the term builder to use.
         */
        public Terms(Variables variables, TermBuilder tb) {
            this(variables.self != null ? tb.var(variables.self) : null,
                convertFlagMap(variables.breakFlags, tb),
                convertFlagMap(variables.continueFlags, tb),
                variables.returnFlag != null ? tb.var(variables.returnFlag) : null,
                variables.result != null ? tb.var(variables.result) : null,
                variables.exception != null ? tb.var(variables.exception) : null,
                convertHeapMap(variables.remembranceHeaps, tb),
                convertHeapMap(variables.remembranceLocalVariables, tb),
                convertHeapMap(variables.outerRemembranceHeaps, tb),
                convertHeapMap(variables.outerRemembranceVariables, tb));
        }

        /**
         *
         * @param map a map containing heaps as values.
         * @param tb a term builder.
         * @return a map with all values termified.
         */
        private static Map<LocationVariable, JTerm> convertHeapMap(
                Map<LocationVariable, LocationVariable> map, TermBuilder tb) {
            return map.entrySet().stream().collect(
                Collectors.<Map.Entry<LocationVariable, LocationVariable>, LocationVariable, JTerm>toMap(
                    Map.Entry::getKey, entry -> tb.var(entry.getValue())));
        }

        /**
         *
         * @param map a map containing boolean variables as values.
         * @param tb a term builder.
         * @return a map with all values termified.
         */
        private static Map<Label, JTerm> convertFlagMap(Map<Label, LocationVariable> map,
                TermBuilder tb) {
            return map.entrySet().stream().collect(
                Collectors.<Map.Entry<Label, LocationVariable>, Label, JTerm>toMap(
                    Map.Entry::getKey,
                    entry -> tb.var(entry.getValue())));
        }
    }
}
