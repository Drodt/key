/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.speclang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.declaration.modifier.VisibilityModifier;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.op.IObserverFunction;
import de.uka.ilkd.key.logic.op.LocationVariable;
import de.uka.ilkd.key.pp.LogicPrinter;
import de.uka.ilkd.key.proof.OpReplacer;
import de.uka.ilkd.key.proof.init.ContractPO;
import de.uka.ilkd.key.proof.init.DependencyContractPO;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.ProofOblInput;

import org.key_project.logic.SyntaxElement;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.java.MapUtil;

/**
 * Standard implementation of the DependencyContract interface.
 */
public final class DependencyContractImpl implements DependencyContract {
    final String baseName;
    final String name;
    final KeYJavaType kjt;
    final IObserverFunction target;
    final KeYJavaType specifiedIn;
    final Map<LocationVariable, JTerm> originalPres;
    final JTerm originalMby;
    final Map<LocationVariable, JTerm> originalDeps;
    final LocationVariable originalSelfVar;
    final ImmutableList<LocationVariable> originalParamVars;
    final Map<LocationVariable, LocationVariable> originalAtPreVars;
    final JTerm globalDefs;
    final int id;


    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    DependencyContractImpl(String baseName, String name, KeYJavaType kjt, IObserverFunction target,
            KeYJavaType specifiedIn, Map<LocationVariable, JTerm> pres, JTerm mby,
            Map<LocationVariable, JTerm> deps, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars,
            Map<LocationVariable, LocationVariable> atPreVars, JTerm globalDefs, int id) {
        assert baseName != null;
        assert kjt != null;
        assert target != null;
        assert pres != null;
        assert deps != null : "cannot create contract " + baseName + " for " + target
            + " when no specification is given";
        assert (selfVar == null) == target.isStatic();
        assert paramVars != null;
        // This cannot be done properly for multiple heaps without access to services:
        // assert paramVars.size() == target.arity() - (target.isStatic() ? 1 : 2);
        assert target.getStateCount() > 0;
        this.baseName = baseName;
        this.name = name != null ? name
                : ContractFactory.generateContractName(baseName, kjt, target, specifiedIn, id);
        this.kjt = kjt;
        this.target = target;
        this.specifiedIn = specifiedIn;
        this.originalPres = pres;
        this.originalMby = mby;
        this.originalDeps = deps;
        this.originalSelfVar = selfVar;
        this.originalParamVars = paramVars;
        this.originalAtPreVars = atPreVars;
        this.globalDefs = globalDefs;
        this.id = id;
    }


    @Deprecated
    DependencyContractImpl(String baseName, KeYJavaType kjt, IObserverFunction target,
            KeYJavaType specifiedIn, Map<LocationVariable, JTerm> pres, JTerm mby,
            Map<LocationVariable, JTerm> deps, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars,
            Map<LocationVariable, LocationVariable> atPreVars) {
        this(baseName, null, kjt, target, specifiedIn, pres, mby, deps, selfVar, paramVars,
            atPreVars, null, INVALID_ID);
    }

    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------

    @Override
    public DependencyContract map(UnaryOperator<JTerm> op, Services services) {
        Map<LocationVariable, JTerm> newPres = originalPres.entrySet().stream()
                .collect(MapUtil.collector(Map.Entry::getKey, entry -> op.apply(entry.getValue())));
        JTerm newMby = op.apply(originalMby);
        Map<LocationVariable, JTerm> newDeps = originalDeps.entrySet().stream()
                .collect(MapUtil.collector(Map.Entry::getKey, entry -> op.apply(entry.getValue())));

        return new DependencyContractImpl(baseName, name, kjt, target, specifiedIn, newPres, newMby,
            newDeps, originalSelfVar, originalParamVars, originalAtPreVars, globalDefs, id);
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public int id() {
        return id;
    }


    @Override
    public KeYJavaType getKJT() {
        return kjt;
    }


    @Override
    public IObserverFunction getTarget() {
        return target;
    }


    @Override
    public boolean hasMby() {
        return originalMby != null;
    }


    @Override
    public JTerm getPre(LocationVariable heap, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars,
            Map<LocationVariable, LocationVariable> atPreVars, Services services) {
        assert (selfVar == null) == (originalSelfVar == null);
        assert paramVars != null;
        assert paramVars.size() == originalParamVars.size();
        assert services != null;
        Map<SyntaxElement, SyntaxElement> map = new LinkedHashMap<>();
        if (originalSelfVar != null) {
            map.put(originalSelfVar, selfVar);
        }
        for (LocationVariable originalParamVar : originalParamVars) {
            map.put(originalParamVar, paramVars.head());
            paramVars = paramVars.tail();
        }
        if (atPreVars != null && originalAtPreVars != null) {
            for (LocationVariable h : services.getTypeConverter().getHeapLDT().getAllHeaps()) {
                LocationVariable originalAtPreVar = originalAtPreVars.get(h);
                if (atPreVars.get(h) != null && originalAtPreVar != null) {
                    map.put(services.getTermBuilder().var(originalAtPreVar),
                        services.getTermBuilder().var(atPreVars.get(h)));
                }
            }
        }

        OpReplacer or = new OpReplacer(map, services.getTermFactory(), services.getProof());
        return or.replace(originalPres.get(heap));
    }

    @Override
    public JTerm getPre(List<LocationVariable> heapContext, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars,
            Map<LocationVariable, LocationVariable> atPreVars, Services services) {
        JTerm result = null;
        for (LocationVariable heap : heapContext) {
            final JTerm p = getPre(heap, selfVar, paramVars, atPreVars, services);
            if (result == null) {
                result = p;
            } else {
                result = services.getTermBuilder().and(result, p);
            }
        }
        return result;
    }


    @Override
    public JTerm getPre(LocationVariable heap, JTerm heapTerm, JTerm selfTerm,
            ImmutableList<JTerm> paramTerms, Map<LocationVariable, JTerm> atPres,
            Services services) {
        assert heapTerm != null;
        assert (selfTerm == null) == (originalSelfVar == null);
        assert paramTerms != null;
        assert paramTerms.size() == originalParamVars.size();
        assert services != null;
        Map<SyntaxElement, SyntaxElement> map = new LinkedHashMap<>();
        map.put(services.getTermBuilder().var(heap), heapTerm);
        if (originalSelfVar != null) {
            map.put(services.getTermBuilder().var(originalSelfVar), selfTerm);
        }
        for (LocationVariable originalParamVar : originalParamVars) {
            map.put(services.getTermBuilder().var(originalParamVar), paramTerms.head());
            paramTerms = paramTerms.tail();
        }
        if (atPres != null && originalAtPreVars != null) {
            for (LocationVariable h : services.getTypeConverter().getHeapLDT().getAllHeaps()) {
                LocationVariable originalAtPreVar = originalAtPreVars.get(h);
                if (atPres.get(h) != null && originalAtPreVar != null) {
                    map.put(services.getTermBuilder().var(originalAtPreVar), atPres.get(h));
                }
            }
        }
        OpReplacer or = new OpReplacer(map, services.getTermFactory(), services.getProof());
        return or.replace(originalPres.get(heap));
    }


    @Override
    public JTerm getPre(List<LocationVariable> heapContext, Map<LocationVariable, JTerm> heapTerms,
            JTerm selfTerm, ImmutableList<JTerm> paramTerms, Map<LocationVariable, JTerm> atPres,
            Services services) {
        JTerm result = null;
        for (LocationVariable heap : heapContext) {
            final JTerm p =
                getPre(heap, heapTerms.get(heap), selfTerm, paramTerms, atPres, services);
            if (result == null) {
                result = p;
            } else {
                result = services.getTermBuilder().and(result, p);
            }
        }
        return result;
    }

    @Override
    public JTerm getRequires(LocationVariable heap) {
        return originalPres.get(heap);
    }

    @Override
    public JTerm getModifiable(LocationVariable heap) {
        throw new UnsupportedOperationException("Not applicable for dependency contracts.");
    }

    @Override
    public JTerm getAccessible(LocationVariable heap) {
        return originalDeps.get(heap);
    }

    @Override
    public JTerm getMby() {
        return this.originalMby;
    }

    @Override
    public JTerm getMby(LocationVariable selfVar, ImmutableList<LocationVariable> paramVars,
            Services services) {
        assert hasMby();
        assert (selfVar == null) == (originalSelfVar == null);
        assert paramVars != null;
        assert paramVars.size() == originalParamVars.size();
        assert services != null;
        Map<SyntaxElement, SyntaxElement> map = new LinkedHashMap<>();
        if (originalSelfVar != null) {
            map.put(originalSelfVar, selfVar);
        }
        for (LocationVariable originalParamVar : originalParamVars) {
            map.put(originalParamVar, paramVars.head());
            paramVars = paramVars.tail();
        }
        OpReplacer or = new OpReplacer(map, services.getTermFactory(), services.getProof());
        return or.replace(originalMby);
    }


    @Override
    public JTerm getMby(Map<LocationVariable, JTerm> heapTerms, JTerm selfTerm,
            ImmutableList<JTerm> paramTerms, Map<LocationVariable, JTerm> atPres,
            Services services) {
        assert hasMby();
        assert heapTerms != null;
        assert (selfTerm == null) == (originalSelfVar == null);
        assert paramTerms != null;
        assert paramTerms.size() == originalParamVars.size();
        assert services != null;
        Map<SyntaxElement, SyntaxElement> map = new LinkedHashMap<>();
        for (LocationVariable heap : heapTerms.keySet()) {
            map.put(services.getTermBuilder().var(heap), heapTerms.get(heap));
        }
        if (originalSelfVar != null) {
            map.put(services.getTermBuilder().var(originalSelfVar), selfTerm);
        }
        for (LocationVariable originalParamVar : originalParamVars) {
            map.put(services.getTermBuilder().var(originalParamVar), paramTerms.head());
            paramTerms = paramTerms.tail();
        }
        if (atPres != null && originalAtPreVars != null) {
            for (LocationVariable h : services.getTypeConverter().getHeapLDT().getAllHeaps()) {
                LocationVariable originalAtPreVar = originalAtPreVars.get(h);
                if (atPres.get(h) != null && originalAtPreVar != null) {
                    map.put(services.getTermBuilder().var(originalAtPreVar), atPres.get(h));
                }
            }
        }
        OpReplacer or = new OpReplacer(map, services.getTermFactory(), services.getProof());
        return or.replace(originalMby);
    }


    @Override
    public String getPlainText(Services services) {
        return getText(false, services);
    }

    @Override
    public String getHTMLText(Services services) {
        return getText(true, services);
    }

    private String getText(boolean includeHtmlMarkup, Services services) {
        StringBuilder pres = new StringBuilder();
        for (LocationVariable h : originalPres.keySet()) {
            JTerm originalPre = originalPres.get(h);
            if (originalPre != null) {
                pres.append("<b>pre[").append(h).append("]</b> ").append(LogicPrinter.escapeHTML(
                    LogicPrinter.quickPrintTerm(originalPre, services), false)).append("<br>");
            }
        }
        StringBuilder deps = new StringBuilder();
        for (LocationVariable h : originalDeps.keySet()) {
            if (h.name().toString().endsWith("AtPre") && target.getStateCount() == 1) {
                continue;
            }
            JTerm originalDep = originalDeps.get(h);
            if (originalDep != null) {
                deps.append("<b>dep[").append(h).append("]</b> ").append(LogicPrinter.escapeHTML(
                    LogicPrinter.quickPrintTerm(originalDep, services), false)).append("<br>");
            }
        }
        final String mby = hasMby() ? LogicPrinter.quickPrintTerm(originalMby, services) : null;

        if (includeHtmlMarkup) {
            return "<html>" + pres + deps
                + (mby != null ? "<br><b>measured-by</b> " + LogicPrinter.escapeHTML(mby, false)
                        : "")
                + "</html>";
        } else {
            return "pre: " + pres + "\ndep: " + deps + (hasMby() ? "\nmeasured-by: " + mby : "");
        }
    }


    @Override
    public boolean toBeSaved() {
        return false; // because dependency contracts currently cannot be
        // specified directly in DL
    }


    @Override
    public String proofToString(Services services) {
        assert false;
        return null;
    }


    @Override
    public JTerm getDep(LocationVariable heap, boolean atPre, LocationVariable selfVar,
            ImmutableList<LocationVariable> paramVars,
            Map<LocationVariable, LocationVariable> atPreVars, Services services) {
        assert (selfVar == null) == (originalSelfVar == null);
        assert paramVars != null;
        assert paramVars.size() == originalParamVars.size();
        assert services != null;
        Map<SyntaxElement, SyntaxElement> map = new LinkedHashMap<>();
        if (originalSelfVar != null) {
            map.put(originalSelfVar, selfVar);
        }
        for (LocationVariable originalParamVar : originalParamVars) {
            map.put(originalParamVar, paramVars.head());
            paramVars = paramVars.tail();
        }
        if (atPreVars != null && originalAtPreVars != null) {
            for (LocationVariable h : services.getTypeConverter().getHeapLDT().getAllHeaps()) {
                LocationVariable originalAtPreVar = originalAtPreVars.get(h);
                if (atPreVars.get(h) != null && originalAtPreVar != null) {
                    map.put(services.getTermBuilder().var(atPre ? h : originalAtPreVar),
                        services.getTermBuilder().var(atPreVars.get(h)));
                }
            }
        }
        OpReplacer or = new OpReplacer(map, services.getTermFactory(), services.getProof());
        return or.replace(originalDeps.get(atPre ? originalAtPreVars.get(heap) : heap));
    }


    @Override
    public JTerm getDep(LocationVariable heap, boolean atPre, JTerm heapTerm, JTerm selfTerm,
            ImmutableList<JTerm> paramTerms, Map<LocationVariable, JTerm> atPres,
            Services services) {
        assert heapTerm != null;
        assert (selfTerm == null) == (originalSelfVar == null);
        assert paramTerms != null;
        assert paramTerms.size() == originalParamVars.size();
        assert services != null;
        Map<SyntaxElement, SyntaxElement> map = new LinkedHashMap<>();
        map.put(services.getTermBuilder().var(heap), heapTerm);
        if (originalSelfVar != null) {
            map.put(services.getTermBuilder().var(originalSelfVar), selfTerm);
        }
        for (LocationVariable originalParamVar : originalParamVars) {
            map.put(services.getTermBuilder().var(originalParamVar), paramTerms.head());
            paramTerms = paramTerms.tail();
        }
        if (atPres != null && originalAtPreVars != null) {
            for (LocationVariable h : services.getTypeConverter().getHeapLDT().getAllHeaps()) {
                LocationVariable originalAtPreVar = originalAtPreVars.get(h);
                if (originalAtPreVar != null && atPres.get(h) != null) {
                    map.put(services.getTermBuilder().var(originalAtPreVar), atPres.get(h));
                }
            }
        }

        OpReplacer or = new OpReplacer(map, services.getTermFactory(), services.getProof());
        return or.replace(originalDeps.get(atPre ? originalAtPreVars.get(heap) : heap));
    }

    @Override
    public JTerm getGlobalDefs() {
        return this.globalDefs;
    }

    @Override
    public JTerm getGlobalDefs(LocationVariable heap, JTerm heapTerm, JTerm selfTerm,
            ImmutableList<JTerm> paramTerms, Services services) {
        assert false : "old clauses are not yet supported for dependency contracts";
        return null;
    }

    @Override
    public String toString() {
        return originalDeps.toString();
    }


    @Override
    public String getDisplayName() {
        return ContractFactory.generateDisplayName(baseName, kjt, target, specifiedIn, id);
    }


    @Override
    public VisibilityModifier getVisibility() {
        return null;
    }


    @Override
    public boolean transactionApplicableContract() {
        return false;
    }

    @Override
    public ProofOblInput createProofObl(InitConfig initConfig, Contract contract,
            boolean addSymbolicExecutionLabel) {
        if (addSymbolicExecutionLabel) {
            throw new IllegalStateException("Symbolic Execution API is not supported.");
        } else {
            return createProofObl(initConfig, contract);
        }
    }


    @Override
    public ProofOblInput createProofObl(InitConfig initConfig, Contract contract) {
        return new DependencyContractPO(initConfig, (DependencyContract) contract);
    }


    @Override
    public ContractPO createProofObl(InitConfig initConfig) {
        return (ContractPO) createProofObl(initConfig, this);
    }


    @Override
    public ProofOblInput getProofObl(Services services) {
        return services.getSpecificationRepository().getPO(this);
    }


    @Override
    public DependencyContract setID(int newId) {
        return new DependencyContractImpl(baseName, null, kjt, target, specifiedIn, originalPres,
            originalMby, originalDeps, originalSelfVar, originalParamVars, originalAtPreVars,
            globalDefs, newId);
    }


    @Override
    public Contract setTarget(KeYJavaType newKJT, IObserverFunction newPM) {
        return new DependencyContractImpl(baseName, null, newKJT, newPM, specifiedIn, originalPres,
            originalMby, originalDeps, originalSelfVar, originalParamVars, originalAtPreVars,
            globalDefs, id);
    }


    @Override
    public String getTypeName() {
        return ContractFactory.generateContractTypeName(baseName, kjt, target, specifiedIn);
    }

    @Override
    public boolean hasSelfVar() {
        return originalSelfVar != null;
    }

    @Override
    public OriginalVariables getOrigVars() {
        Map<LocationVariable, LocationVariable> atPreVars =
            new LinkedHashMap<>();
        if (originalAtPreVars != null) {
            for (LocationVariable h : originalAtPreVars.keySet()) {
                atPreVars.put(h, originalAtPreVars.get(h));
            }
        }
        return new OriginalVariables(originalSelfVar, null, null, atPreVars, originalParamVars);
    }
}
