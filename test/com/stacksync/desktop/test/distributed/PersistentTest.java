package com.stacksync.desktop.test.distributed;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PersistentTest extends TestCase {
	public static TestSuite suite() {
		TestSuite suite = new TestSuite();
		// Transient tests
		suite.addTestSuite(NewTests.class);
        suite.addTestSuite(ModifiedTests.class);
        suite.addTestSuite(DeletedTests.class);
        suite.addTestSuite(ConflictTests.class);
        suite.addTestSuite(RenameTests.class);
        suite.addTestSuite(MovedTests.class);
		return suite;
	}

}
