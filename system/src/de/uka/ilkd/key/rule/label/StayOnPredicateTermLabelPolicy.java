package de.uka.ilkd.key.rule.label;

import java.util.LinkedList;
import java.util.List;

import de.uka.ilkd.key.collection.ImmutableArray;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.JavaBlock;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.label.PredicateTermLabel;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.label.TermLabelState;
import de.uka.ilkd.key.logic.op.Operator;
import de.uka.ilkd.key.logic.op.QuantifiableVariable;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.Rule;
import de.uka.ilkd.key.rule.Taclet.TacletLabelHint;
import de.uka.ilkd.key.rule.Taclet.TacletLabelHint.TacletOperation;
import de.uka.ilkd.key.symbolic_execution.PredicateEvaluationUtil;
import de.uka.ilkd.key.symbolic_execution.util.IFilter;
import de.uka.ilkd.key.symbolic_execution.util.JavaUtil;

/**
 * This {@link TermLabelPolicy} maintains a {@link PredicateTermLabel} on predicates.
 * @author Martin Hentschel
 */
public class StayOnPredicateTermLabelPolicy implements TermLabelPolicy {
   /**
    * {@inheritDoc}
    */
   @Override
   public TermLabel keepLabel(TermLabelState state,
                              Services services,
                              PosInOccurrence applicationPosInOccurrence, 
                              Term applicationTerm, 
                              Rule rule, 
                              Goal goal, 
                              Object hint, 
                              Term tacletTerm, 
                              Operator newTermOp, 
                              ImmutableArray<Term> newTermSubs, 
                              ImmutableArray<QuantifiableVariable> newTermBoundVars, 
                              JavaBlock newTermJavaBlock, 
                              ImmutableArray<TermLabel> newTermOriginalLabels,
                              TermLabel label) {
      // Maintain label if new Term is a predicate
      if (PredicateEvaluationUtil.isPredicate(newTermOp) || 
          PredicateEvaluationUtil.isLogicOperator(newTermOp, newTermSubs)) {
         assert label instanceof PredicateTermLabel;
         PredicateTermLabel pLabel = (PredicateTermLabel) label;
         PredicateTermLabel originalLabel = ensureThatOriginalLabelIsMaintained(newTermOriginalLabels);
         PredicateTermLabel mostImportantLabel = originalLabel != null ? originalLabel : pLabel;
         // May change sub ID if logical operators like junctors are used
         boolean newLabelIdRequired = false;
         List<String> originalLabelIds = new LinkedList<String>();
         if (hint instanceof TacletLabelHint) {
            TacletLabelHint tacletHint = (TacletLabelHint) hint;
            if (TacletOperation.ADD_ANTECEDENT.equals(tacletHint.getTacletOperation()) ||
                TacletOperation.ADD_SUCCEDENT.equals(tacletHint.getTacletOperation()) ||
                TacletOperation.REPLACE_TO_ANTECEDENT.equals(tacletHint.getTacletOperation()) ||
                TacletOperation.REPLACE_TO_SUCCEDENT.equals(tacletHint.getTacletOperation())) {
//               newLabelIdRequired = true;
            }
            boolean topLevel = isTopLevel(tacletHint, tacletTerm);
            if (tacletHint.getSequentFormula() != null) {
               if (!PredicateEvaluationUtil.isPredicate(tacletHint.getSequentFormula())) {
                  newLabelIdRequired = true;
               }
            }
            else if (tacletHint.getTerm() != null) {
               if (!topLevel && !PredicateEvaluationUtil.isPredicate(tacletHint.getTerm())) {
                  newLabelIdRequired = true;
               }
            }
            if (mostImportantLabel != pLabel) { // Without support of quantors '&& topLevel' can be added.
               originalLabelIds.add(pLabel.getId());
            }
         }
         // Replace label with a new one with increased sub ID.
         if (newLabelIdRequired) {
            if (originalLabel != null) {
               originalLabelIds.add(originalLabel.getId());
            }
            int labelSubID = services.getCounter(PredicateTermLabel.PROOF_COUNTER_SUB_PREFIX + mostImportantLabel.getMajorId()).getCountPlusPlus();
            if (!originalLabelIds.isEmpty()) {
               return new PredicateTermLabel(mostImportantLabel.getMajorId(), labelSubID, originalLabelIds);
            }
            else {
               return new PredicateTermLabel(mostImportantLabel.getMajorId(), labelSubID);
            }
         }
         else {
            if (!originalLabelIds.isEmpty()) {
               return new PredicateTermLabel(mostImportantLabel.getMajorId(), mostImportantLabel.getMinorId(), originalLabelIds);
            }
            else {
               return label;
            }
         }
      }
      else {
         return null;
      }
   }

   protected PredicateTermLabel ensureThatOriginalLabelIsMaintained(ImmutableArray<TermLabel> originalLabels) {
      TermLabel originalLabel = JavaUtil.search(originalLabels, new IFilter<TermLabel>() {
         @Override
         public boolean select(TermLabel element) {
            return element instanceof PredicateTermLabel;
         }
      });
      return (PredicateTermLabel)originalLabel;
   }

   protected boolean isTopLevel(TacletLabelHint tacletHint, Term tacletTerm) {
      if (TacletOperation.REPLACE_TERM.equals(tacletHint.getTacletOperation())) {
         return tacletHint.getTerm() == tacletTerm;
      }
      else {
         return tacletHint.getSequentFormula().formula() == tacletTerm;
      }
   }
}