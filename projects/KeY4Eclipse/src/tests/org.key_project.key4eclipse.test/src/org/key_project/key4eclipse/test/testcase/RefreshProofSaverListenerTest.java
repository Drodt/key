package org.key_project.key4eclipse.test.testcase;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.key_project.key4eclipse.util.KeYExampleUtil;
import org.key_project.util.eclipse.ResourceUtil;
import org.key_project.util.test.util.TestUtilsUtil;

import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.io.ProofSaver;
import de.uka.ilkd.key.symbolic_execution.util.KeYEnvironment;
import de.uka.ilkd.key.ui.CustomConsoleUserInterface;

/**
 * Tests for {@code org.key_project.key4eclipse.event.RefreshProofSaverListener}.
 * @author Martin Hentschel
 */
public class RefreshProofSaverListenerTest extends TestCase {
   /**
    * Tests if an {@link IFile} is refreshed when its local file changes
    * via {@link ProofSaver#save()}.
    */
   @Test
   public void testRefresh() throws IOException, CoreException {
      // Create file
      IProject project = TestUtilsUtil.createProject("RefreshProofSaverListenerTest_testRefresh");
      IFile file = TestUtilsUtil.createFile(project, "Test.proof", "Replace me!");
      File location = ResourceUtil.getLocation(file);
      // Do proof
      KeYEnvironment<CustomConsoleUserInterface> env = KeYEnvironment.load(KeYExampleUtil.getExampleProof(), null, null);
      try {
         Proof proof = env.getLoadedProof();
         assertNotNull(proof);
         // Save proof
         new ProofSaver(proof, location.getAbsolutePath(), "1.0").save();
         // Make sure that IFile was updated
         assertTrue(file.isSynchronized(IResource.DEPTH_INFINITE));
         assertFalse("Replace me!".equals(ResourceUtil.readFrom(file)));
      }
      finally {
         env.dispose();
      }
   }
}