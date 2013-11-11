package com.stacksync.desktop.test.distributed;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

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
