package org.key_project.key4eclipse.resources.test.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.key_project.key4eclipse.resources.test.testcase.junit.AutoDeleteTests;
import org.key_project.key4eclipse.resources.test.testcase.junit.BuilderTests;
import org.key_project.key4eclipse.resources.test.testcase.junit.HideMetaFilesTests;
import org.key_project.key4eclipse.resources.test.testcase.junit.KeYResourcesUtilTest;
import org.key_project.key4eclipse.resources.test.testcase.junit.MarkerTests;
import org.key_project.key4eclipse.resources.test.testcase.junit.ProofMetaFileContentExceptionTests;

/**
 * Run all contained JUnit 4 test cases.
 * @author Martin Hentschel
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
   AutoDeleteTests.class,
   BuilderTests.class,
   HideMetaFilesTests.class,
   KeYResourcesUtilTest.class,
   MarkerTests.class,
   ProofMetaFileContentExceptionTests.class
})
public class AllResourcesTests {
}