package org.key_project.keyide.ui.util;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.key_project.key4eclipse.common.ui.provider.ImmutableCollectionContentProvider;
import org.key_project.key4eclipse.starter.core.property.KeYResourceProperties;
import org.key_project.key4eclipse.starter.core.util.KeYUtil;
import org.key_project.keyide.ui.editor.input.StringInput;
import org.key_project.keyide.ui.editor.input.StringStorage;
import org.key_project.util.eclipse.ResourceUtil;
import org.key_project.util.eclipse.WorkbenchUtil;
import org.key_project.util.eclipse.swt.SWTUtil;
import org.key_project.util.java.CollectionUtil;
import org.key_project.util.java.IFilter;
import org.key_project.util.java.ObjectUtil;
import org.key_project.util.jdt.JDTUtil;

import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.nodeviews.NonGoalInfoView;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.speclang.FunctionalOperationContract;
import de.uka.ilkd.key.symbolic_execution.util.KeYEnvironment;

/**
 * This class provides static methods for the communication between KeY and the Eclipse UI.
 * 
 * @author Christoph Schneider, Niklas Bunzel, Stefan K�sdorf, Marco Drebing
 */

public class KeYToUIUtil {
   
   private KeYToUIUtil(){
      
   }

   /**
    * Opens a dialog to select a contract for the specified method, furthermore creates the proof for that method
    * @param method The method to create the proof for.
    */
   public static void createProof(final IMethod method) {
         try {
           if (method != null && method.exists()) {
               // Load location
               final IProject project = method.getResource().getProject();
               final File location = ResourceUtil.getLocation(project);
               final File bootClassPath = KeYResourceProperties.getKeYBootClassPathLocation(project);
               final List<File> classPaths = KeYResourceProperties.getKeYClassPathEntries(project);
                 
               new Job("Loading Proof") { // A Job to load the proof in KeY
                  @Override
                    protected IStatus run(IProgressMonitor monitor) {
                       try {
                          monitor.beginTask("Loading Proof", 1);
                          final KeYEnvironment<?> environment = KeYEnvironment.load(location, classPaths, bootClassPath);

                          if (environment.getInitConfig() != null) {
                             // Get method to proof in KeY
                             final IProgramMethod pm = KeYUtil.getProgramMethod(method, environment.getJavaInfo());
                             if (pm != null) {
                                 KeYJavaType type = pm.getContainerType();
                                 final ImmutableSet<FunctionalOperationContract> operationContracts = environment.getSpecificationRepository().getOperationContracts(type, pm);
                                 final Display display = Display.getDefault();
                                 Runnable run = new Runnable() {
                                    @Override
                                    public void run() {
                                       try {
                                          // Open selection dialog
                                          Proof proof = openDialog(operationContracts, environment);
                                          //Open proof in Editor if correctly selected
                                          if(proof != null){
                                             KeYToUIUtil.openEditor(proof, environment);
                                          }
                                       }
                                       catch (Exception e) {
                                          LogUtil.getLogger().logError(e);
                                          LogUtil.getLogger().openErrorDialog(null, e);
                                       }  
                                    }
                                 };
                                 display.asyncExec(run);  
                             }
                             else {
                                return LogUtil.getLogger().createErrorStatus("Can't find method \"" + JDTUtil.getQualifiedMethodLabel(method) + "\" in KeY.");
                             }
                          }
                          SWTUtil.checkCanceled(monitor);
                          monitor.worked(1);
                          monitor.done();
                          done(Status.OK_STATUS);
                          return Status.OK_STATUS;                          
                       }
                       catch (OperationCanceledException e) {
                          return Status.CANCEL_STATUS;
                       }
                     catch (Exception e) {
                        LogUtil.getLogger().logError(e);
                        return Status.CANCEL_STATUS;
                     }
                  }
              }.schedule();
          }
       }
       catch (Exception e) {
          LogUtil.getLogger().logError(e);
       }
   }

   /**
    * Searches a {@link FunctionalOperationContract} with the given name.
    * @param operationContracts The available {@link FunctionalOperationContract} to search in.
    * @param contractName The name of the {@link FunctionalOperationContract} to search.
    * @return The found {@link FunctionalOperationContract} or {@code null} if no one was found.
    */
   public static FunctionalOperationContract findContract(ImmutableSet<FunctionalOperationContract> operationContracts, 
                                                          final String contractName) {
       return CollectionUtil.search(operationContracts, new IFilter<FunctionalOperationContract>() {
           @Override
           public boolean select(FunctionalOperationContract element) {
               return element != null && ObjectUtil.equals(element.getName(), contractName);
           }
       });
   }
  
   
   
   
   /**
    * Opens the currently selected {@link Proof} into the KeY-Editor.
    * @param name The  name to display at the editor-tab
    * @param ui The UserInterface that holds the KeYMediator
    */
   public static void openEditor(Proof proof, KeYEnvironment<?> environment){
      String inputText = NonGoalInfoView.computeText(environment.getMediator(), proof.root());
      String inputSequent=(String) inputText.subSequence(0, inputText.length()-12);
      IStorage storage = new StringStorage(inputSequent, inputText);
      IStorageEditorInput input = new StringInput(storage, proof, environment);
      try {
         PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, "org.key_project.keyide.ui.editor");
      }
      catch (PartInitException e) {
         e.printStackTrace();
      }
   }
   
   
   
   /**
    * Asks the user if he wants to switch into the KeY-perspective and opens it if necessary
    */
   public static void switchPerspective() {
      if(KeYIDEUtil.shouldSwitchToKeyPerspective(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage())){
         WorkbenchUtil.openPerspective("org.key_project.keyide.ui.perspectives");
      }
      
   }
   
   
//   private static ContractSelectionDialog initializeDialog(ImmutableSet<FunctionalOperationContract> operationContracts, Shell shell, Services services) {
//      Shell dialogShell = new Shell(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
//      ContractSelectionDialog dialog = new ContractSelectionDialog(dialogShell, ImmutableCollectionContentProvider.getInstance(), services);
//      dialog.setTitle("Contract selection");
//      dialog.setMessage("Select a contract to debug.");
//      dialog.setInput(operationContracts);
////      existingContractText=new Text(dialogShell, SWT.TOP);
//      return dialog;
//   } 
   
   private static Proof openDialog(ImmutableSet<FunctionalOperationContract> operationContracts, KeYEnvironment<?> environment) throws ProofInputException 
   {
      Shell parent = WorkbenchUtil.getActiveShell();
      ImmutableCollectionContentProvider contentProvider = ImmutableCollectionContentProvider.getInstance();
      org.key_project.key4eclipse.common.ui.dialog.ContractSelectionDialog dialog = new org.key_project.key4eclipse.common.ui.dialog.ContractSelectionDialog(parent, contentProvider, environment.getServices());
      dialog.setInput(operationContracts);
//      ContractSelectionDialog dialog = new ContractSelectionDialog(WorkbenchUtil, contentProvider, services)
//      FunctionalOperationContract selectedContract = KeYToUIUtil.findContract(operationContracts, getContractId());
      if (dialog.open() == org.key_project.key4eclipse.common.ui.dialog.ContractSelectionDialog.OK) {
          Object result = dialog.getFirstResult();
          if (result instanceof FunctionalOperationContract) {
              FunctionalOperationContract foc = (FunctionalOperationContract)result;
//              existingContractText.setText(foc.getName());
              return environment.createProof(foc.createProofObl(environment.getInitConfig(), foc));
         }
          else {
             throw new ProofInputException("The selected contract is no FunctionalOperationContract.");
          }
      }
      else {
         return null;
      }
   }     
}