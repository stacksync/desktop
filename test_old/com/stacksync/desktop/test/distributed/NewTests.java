/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.distributed;

import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author Marc
 */
public class NewTests extends TestCase{
        
    private Logger logg = Logger.getLogger(NewTests.class.getName());
    private TestUtils testUtils = TestUtils.getUtils();
    private ConsoleAppender log = new ConsoleAppender(new PatternLayout("$$$$%d{yyyy-MM-dd HH:mm:ss}#%p#[%t]#%c->%M#%m#%n"));


    /**
     * we create a new file it's expected to be the same file for each machine
     */
    public  void testNewFile() {
       
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
		logg.warn("testing NEW FILE");
		String testFileName=testUtils.createNewFile("test.txt", "testing");
		if(testFileName == null){
			assertTrue(false);
		}
		logg.warn("Waiting until the file is sync");
		testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFileName,"localhost");
		logg.warn("File sync we can continue now");
		logg.warn("Give some time to stacksync to do his job");
		
		try {
			Thread.sleep(testUtils.getSleepTime());
		} catch (InterruptedException e) {
			logg.error("Sleep failed.");
			assertTrue(false);
		}
		boolean testOk = false;
		for (Computer comp : testUtils.getComputerList()) {
			testOk = testUtils.checkRemoteChecksum(testFileName, comp.getIp(), comp.getUsername(),comp.getPassword());
			logg.warn("CORRECT sync for FILE name modification:" + testOk+ " for computer:" + comp.getIp());
			if (!testOk){
				logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
				break;
			}
		}
		assertTrue(testOk);


	}

    /**
     * We create a new folder with 2 files in it's expected that it will have have the same checksum in all the machines
     */
    public void testNewFolder() {
        
        
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
		logg.warn("testing NEW FOLDER");
		String testFolderName = testUtils.createNewFolder("testNewFolder","testfile1","testfile2","file1.txt","file2.txt");
		if(testFolderName == null){
			assertTrue(false);
		}
		logg.warn("Waiting until the folder is sync");
		testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFolderName,"localhost");
		logg.warn("Folder sync we can continue now");
		logg.warn("Give some time to stacksync to do his job");	
		
		try {
			Thread.sleep(testUtils.getSleepTime());
		} catch (InterruptedException e) {
			logg.error("Sleep failed.");
			assertTrue(false);			
		}
		
		boolean testOk = false;
		
		for (Computer comp : testUtils.getComputerList()) {
		 testOk = testUtils.checkRemoteFolderChecksum(testFolderName, comp.getIp(), comp.getUsername(),comp.getPassword());
			logg.warn("CORRECT sync for new FOLDER:" + testOk	+ " for computer:" + comp.getIp());
			if (!testOk){
				logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
				break;
			}
		}
		assertTrue(testOk);
	}

}
