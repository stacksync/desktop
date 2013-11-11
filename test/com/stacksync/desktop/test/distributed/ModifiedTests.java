/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.distributed;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
/**
 *
 * @author Marc
 */
public class ModifiedTests extends TestCase{
    private Logger logg = Logger.getLogger(ModifiedTests.class.getName());
    private TestUtils testUtils = TestUtils.getUtils();
   

    /**
     * we modify a file it's expected to be the same file for each machine
     */
    public  void testModifiedFile() {	
        
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
       	logg.warn("testing MODIFIED FILE");
        String testFileName=testUtils.createNewFile("test.txt", "testing");
		if(testFileName == null){
			assertTrue(false);
		}
		logg.warn("Waiting until the file is sync");
		testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFileName,"localhost");
        
        logg.warn("testing MODIFY FILE");
		testFileName=testUtils.createNewFile("test.txt", "testingmodified");
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
			logg.warn("CORRECT sync for FILE modification:" + testOk+ " for computer:" + comp.getIp());
			if (!testOk){
				logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
				break;
			}
		}
		assertTrue(testOk);
	}
  
	/**
     * We modify a folder previously sync and we expect to have the same changes in all the machines
     */
    public  void testModifiedFolder() {	
       
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
        String testFolderName = testUtils.createNewFolder("testNewFolder","testing","testing","file1.txt","file2.txt");	
        if(testFolderName == null){
			assertTrue(false);
		}
        testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFolderName,"localhost");
		logg.warn("testing MODIFIED FOLDER");
		testFolderName = testUtils.createNewFolder("testNewFolder","testingmodified","testingmodified","file1.txt","file2.txt");		
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
			logg.warn("Sleep failed.");
			assertTrue(false);

		}
		boolean testOk = false;
		for (Computer comp : testUtils.getComputerList()) {
		 testOk = testUtils.checkRemoteFolderChecksum(testFolderName, comp.getIp(), comp.getUsername(),comp.getPassword());
			logg.warn("CORRECT sync for modified FOLDER:" + testOk	+ " for computer:" + comp.getIp());
			if (!testOk){
				logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
				break;
			}
		}
		assertTrue(testOk);
	}

}
