/*******************************************************************************
 * Copyright (c) 2014 Karlsruhe Institute of Technology, Germany
 *                    Technical University Darmstadt, Germany
 *                    Chalmers University of Technology, Sweden
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Technical University Darmstadt - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.key_project.sed.key.ui.property;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IDisposable;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;
import org.key_project.key4eclipse.common.ui.decorator.EvaluationViewerDecorator;
import org.key_project.key4eclipse.common.ui.decorator.ProofSourceViewerDecorator;
import org.key_project.key4eclipse.common.ui.util.LogUtil;
import org.key_project.sed.key.core.model.IKeYSEDDebugNode;
import org.key_project.util.eclipse.job.AbstractDependingOnObjectsJob;
import org.key_project.util.java.ObjectUtil;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.label.PredicateTermLabel;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.op.Junctor;
import de.uka.ilkd.key.logic.op.UpdateApplication;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.AbstractOperationPO;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.symbolic_execution.PredicateEvaluationUtil;
import de.uka.ilkd.key.symbolic_execution.PredicateEvaluationUtil.BranchResult;
import de.uka.ilkd.key.symbolic_execution.PredicateEvaluationUtil.PredicateEvaluationResult;
import de.uka.ilkd.key.symbolic_execution.PredicateEvaluationUtil.PredicateResult;
import de.uka.ilkd.key.symbolic_execution.PredicateEvaluationUtil.PredicateValue;
import de.uka.ilkd.key.symbolic_execution.model.IExecutionNode;
import de.uka.ilkd.key.symbolic_execution.model.ITreeSettings;
import de.uka.ilkd.key.util.Pair;

/**
 * This composite provides the content shown in {@link AbstractPredicatePropertySection}
 * and {@link AbstractPredicateGraphitiPropertySection}.
 * @author Martin Hentschel
 */
public abstract class AbstractPredicateComposite implements IDisposable {
   /**
    * The {@link TabbedPropertySheetWidgetFactory} to use.
    */
   private final TabbedPropertySheetWidgetFactory factory;
   
   /**
    * The root {@link Composite}.
    */
   private final Composite root;
   
   /**
    * The children of {@link #root}.
    */
   private final List<Control> controls = new LinkedList<Control>();
   
   /**
    * The used {@link EvaluationViewerDecorator}s.
    */
   private final List<EvaluationViewerDecorator> decorators = new LinkedList<EvaluationViewerDecorator>();

   
   /**
    * The {@link Color} to highlight {@link PredicateValue#TRUE}.
    */
   private final Color trueColor;

   /**
    * The {@link Color} to highlight {@link PredicateValue#FALSE}.
    */
   private final Color falseColor;

   /**
    * The {@link Color} to highlight {@link PredicateValue#UNKNOWN} or {@code null}.
    */
   private final Color unknownColor;
   
   /**
    * The currently shown {@link IKeYSEDDebugNode}.
    */
   private IKeYSEDDebugNode<?> currentNode;
   
   /**
    * Constructor.
    * @param parent The parent {@link Composite}.
    * @param factory The {@link TabbedPropertySheetWidgetFactory} to use.
    */
   public AbstractPredicateComposite(Composite parent, TabbedPropertySheetWidgetFactory factory) {
      this.factory = factory;
      root = factory.createFlatFormComposite(parent);
      root.setLayout(new GridLayout(1, false));
      trueColor = new Color(parent.getDisplay(), EvaluationViewerDecorator.trueRGB);
      falseColor = new Color(parent.getDisplay(), EvaluationViewerDecorator.falseRGB);
      unknownColor = new Color(parent.getDisplay(), EvaluationViewerDecorator.unknownRGB);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void dispose() {
      if (trueColor != null) {
         trueColor.dispose();
      }
      if (falseColor != null) {
         falseColor.dispose();
      }
      if (unknownColor != null) {
         unknownColor.dispose();
      }
   }
   
   /**
    * Updates the shown content.
    * @param node The {@link IKeYSEDDebugNode} which provides the new content.
    */
   public void updateContent(final IKeYSEDDebugNode<?> node) {
      if (!ObjectUtil.equals(currentNode, node)) {
         currentNode = node;
         showEvaluatingInformation();
         AbstractDependingOnObjectsJob.cancelJobs(this);
         Job job = new AbstractDependingOnObjectsJob("Evaluating postconditions", this, PlatformUI.getWorkbench()) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
               try {
                  computeAndAddNewContent(node);
                  return Status.OK_STATUS;
               }
               catch (OperationCanceledException e) {
                  return Status.CANCEL_STATUS;
               }
               catch (Exception e) {
                  return LogUtil.getLogger().createErrorStatus(e);
               }
            }
         };
         job.setSystem(true);
         job.schedule();
      }
   }

   /**
    * Shows an information message to the user.
    */
   protected void showEvaluatingInformation() {
      removeOldContent();
      Label label = factory.createLabel(root, "Please wait until postcondition is evaluated.");
      controls.add(label);
      updateLayout();
   }

   /**
    * Removes all old content and disposes it.
    */
   protected void removeOldContent() {
      for (ProofSourceViewerDecorator viewerDecorator : decorators) {
         viewerDecorator.dispose();
      }
      decorators.clear();
      for (Control control : controls) {
         control.setVisible(false);
         control.dispose();
      }
      controls.clear();
   }
   
   /**
    * Shows new content.
    * @param node The {@link IKeYSEDDebugNode} which provides the new content.
    */
   protected void computeAndAddNewContent(final IKeYSEDDebugNode<?> node) {
      try {
         if (node != null) {
            // Get required information
            final IExecutionNode<?> executionNode = node.getExecutionNode();
            final Node keyNode = computeNodeToShow(node, executionNode);
            final Pair<Term, Term> pair = computeTermToShow(node, executionNode, keyNode);
            // Compute result
            ITreeSettings settings = node.getExecutionNode().getSettings();
            final PredicateEvaluationResult result = PredicateEvaluationUtil.evaluate(keyNode, 
                                                                                      PredicateTermLabel.NAME,
                                                                                      settings.isUseUnicode(),
                                                                                      settings.isUsePrettyPrinting());
System.out.println(result);
            if (!root.isDisposed()) {
               root.getDisplay().syncExec(new Runnable() {
                  @Override
                  public void run() {
                     if (!root.isDisposed()) {
                        addNewContent(result, pair.first, pair.second, executionNode);
                     }
                  }
               });
            }
         }
      }
      catch (final ProofInputException e) {
         if (!root.isDisposed()) {
            root.getDisplay().syncExec(new Runnable() {
               @Override
               public void run() {
                  if (!root.isDisposed()) {
                     Text text = factory.createText(root, e.getMessage());
                     text.setEditable(false);
                     controls.add(text);
                  }
               }
            });
         }
      }
   }
   
   /**
    * Computes the {@link Node} to show.
    * @param node The {@link IKeYSEDDebugNode}.
    * @param executionNode The {@link IExecutionNode}.
    * @return The {@link Node} to show.
    */
   protected Node computeNodeToShow(IKeYSEDDebugNode<?> node, IExecutionNode<?> executionNode) {
      return executionNode.getProofNode();
   }
   
   /**
    * Computes the {@link Term} to show.
    * @param node The {@link IKeYSEDDebugNode}.
    * @param executionNode The {@link IExecutionNode}.
    * @param keyNode The {@link Node}.
    * @return The {@link Term} to show and optionally the uninterpreted predicate.
    */
   protected abstract Pair<Term, Term> computeTermToShow(IKeYSEDDebugNode<?> node, 
                                                         IExecutionNode<?> executionNode, 
                                                         Node keyNode);

   /**
    * Shows the given content.
    * @param result The {@link PredicateEvaluationResult} to consider.
    * @param term The {@link Term} to show.
    * @param uninterpretedPredicate The optional {@link Term} with the uninterpreted predicate offering the {@link PredicateTermLabel}.
    * @param node The {@link IKeYSEDDebugNode} which provides the new content.
    */
   protected void addNewContent(PredicateEvaluationResult result,
                                Term term,
                                Term uninterpretedPredicate,
                                IExecutionNode<?> executionNode) {
      removeOldContent();
      BranchResult[] branchResults = result.getBranchResults();
      for (BranchResult branchResult : branchResults) {
         if (shouldShowBranchResult(branchResult, uninterpretedPredicate)) {
            // Create group
            Group viewerGroup = factory.createGroup(root, branchResult.getLeafNode().serialNr() + ": " + branchResult.getConditionString());
            viewerGroup.setLayout(new FillLayout());
            viewerGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            controls.add(viewerGroup);
            // Create viewer
            SourceViewer viewer = new SourceViewer(viewerGroup, null, SWT.MULTI | SWT.FULL_SELECTION);
            viewer.setEditable(false);
            EvaluationViewerDecorator viewerDecorator = new EvaluationViewerDecorator(viewer);
            decorators.add(viewerDecorator);
            // Show term and results
            PredicateValue value = viewerDecorator.showTerm(term, 
                                                            executionNode.getServices(), 
                                                            executionNode.getMediator(), 
                                                            branchResult);
            viewerGroup.setBackground(viewerDecorator.getColor(value));
         }
      }
      // Add legend
      addLegend();
      // Layout root
      updateLayout();
   }

   /**
    * Check is the given {@link BranchResult} should be shown.
    * @param branchResult The {@link BranchResult} to check.
    * @param uninterpretedPredicate The uninterpreted predicate which provides the {@link PredicateTermLabel}.
    * @return {@code true} show branch result, {@code false} do not show branch result.
    */
   protected boolean shouldShowBranchResult(BranchResult branchResult, Term uninterpretedPredicate) {
      if (branchResult != null) {
         if (uninterpretedPredicate != null) {
            TermLabel label = uninterpretedPredicate.getLabel(PredicateTermLabel.NAME);
            if (label instanceof PredicateTermLabel) {
               PredicateResult result = branchResult.evaluate((PredicateTermLabel) label);
               return result == null || !PredicateValue.FALSE.equals(result.getValue());
            }
            else {
               return true;
            }
         }
         else {
            return true;
         }
      }
      else {
         return false;
      }
   }
   
   /**
    * Searches the {@link Term} with the uninterpreted predicate.
    * @param term The {@link Term} to start search at.
    * @param uninterpretedPredicate The {@link Term} of the proof obligation which specifies the uninterpreted predicate.
    * @return The found {@link Term} or {@code null} if not available.
    */
   protected Term findUninterpretedPredicateTerm(Term term, Term uninterpretedPredicate) {
      if (term.op() == uninterpretedPredicate.op()) {
         return term;
      }
      else if (term.op() == Junctor.AND) {
         Term result = null;
         int i = 0;
         while (result == null && i < term.arity()) {
            result = findUninterpretedPredicateTerm(term.sub(i), uninterpretedPredicate);
            i++;
         }
         return result;
      }
      else {
         return null;
      }
   }

   /**
    * Ensures that the right content is shown.
    */
   protected void updateLayout() {
      root.layout();
      root.getParent().pack();
      root.getParent().layout();
   }

   /**
    * Removes the uninterpreted predicate if required.
    * @param node The {@link Node}.
    * @param term The {@link Term}.
    * @return The {@link Term} without the uninterpreted predicate.
    */
   protected Term removeUninterpretedPredicate(Node node, Term term) {
      Proof proof = node.proof();
      Term predicate = AbstractOperationPO.getUninterpretedPredicate(proof);
      if (predicate != null) {
         term = removeUninterpretedPredicate(proof.getServices().getTermBuilder(), 
                                             term, 
                                             predicate);
      }
      return term;
   }

   /**
    * Removes the uninterpreted predicate recursively in the {@code and} structure.
    * @param tb The {@link TermBuilder} to use.
    * @param term The current {@link Term}.
    * @param uninterpretedPredicate The uninterpreted predicate to remove.
    * @return The {@link Term} without the uninterpreted predicate. 
    */
   protected Term removeUninterpretedPredicate(TermBuilder tb, 
                                               Term term, 
                                               Term uninterpretedPredicate) {
      if (uninterpretedPredicate.op() == term.op()) {
         return tb.tt();
      }
      else if (term.op() == Junctor.AND) { // Only and is supported to ensure correct results
         boolean subsChanged = false;
         Term[] newSubs = new Term[term.arity()];
         for (int i = 0; i < newSubs.length; i++) {
            Term sub = term.sub(i);
            newSubs[i] = removeUninterpretedPredicate(tb, sub, uninterpretedPredicate);
            if (sub != newSubs[i]) {
               subsChanged = true;
            }
         }
         if (subsChanged) {
            Term newTerm = tb.and(newSubs);
            if (term.hasLabels() && newSubs[0] != newTerm && newSubs[1] != newTerm) { // Label new Term only if all children are still important.
               newTerm = tb.label(newTerm, term.getLabels());
            }
            return newTerm;
         }
         else {
            return term;
         }
      }
      else if (term.op() == UpdateApplication.UPDATE_APPLICATION) {
         Pair<ImmutableList<Term>, Term> pair = TermBuilder.goBelowUpdates2(term);
         Term newTarget = removeUninterpretedPredicate(tb, pair.second, uninterpretedPredicate);
         if (pair.second != newTarget) {
            Term newTerm = tb.applyParallel(pair.first, newTarget);
            if (term.hasLabels()) {
               newTerm = tb.label(newTerm, term.getLabels());
            }
            return newTerm;
         }
         else {
            return term;
         }
      }
      else {
         return term;
      }
   }

   /**
    * Adds the legend.
    */
   protected void addLegend() {
      Composite legendComposite = factory.createFlatFormComposite(root);
      legendComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      GridLayout legendLayout = new GridLayout(4, false);
      legendLayout.marginBottom = 0;
      legendLayout.marginHeight = 0;
      legendLayout.marginLeft = 0;
      legendLayout.marginRight = 0;
      legendLayout.marginWidth = 0;
      legendLayout.verticalSpacing = 0;
      legendComposite.setLayout(legendLayout);
      controls.add(legendComposite);
      factory.createLabel(legendComposite, "Legend: ");
      Label trueLabel = factory.createLabel(legendComposite, "true");
      trueLabel.setForeground(trueColor);
      Label falseLabel = factory.createLabel(legendComposite, "false");
      falseLabel.setForeground(falseColor);
      Label unknownLabel = factory.createLabel(legendComposite, "unknown");
      unknownLabel.setForeground(unknownColor);
   }
}