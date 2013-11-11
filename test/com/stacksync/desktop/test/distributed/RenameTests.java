/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.distributed;

import java.io.File;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author Marc
 */
public class RenameTests extends TestCase{
    
    
    private Logger logg = Logger.getLogger(RenameTests.class.getName());
    private TestUtils testUtils = TestUtils.getUtils();
    private ConsoleAppender log = new ConsoleAppender(new PatternLayout("$$$$%d{yyyy-MM-dd HH:mm:ss}#%p#[%t]#%c->%M#%m#%n"));
    
    /**
     * Testing if renaming a file generates problems it's expected to just change it
     */
    public  void testRenameFile(){

        if(!logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
    	logg.warn("Testing File renamed");
		String testFileName=testUtils.createNewFile("test.txt", "testing");
		if(testFileName == null){
			assertTrue(false);
		}
    	logg.warn("Filecreated");
		testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFileName,"localhost");
		
		File filesrc = new File(testUtils.getStacksyncFolderPath() + testFileName);
		File filedest = new File(testUtils.getStacksyncFolderPath()+"testRenamedFile.txt");
		filesrc.renameTo(filedest);
		testUtils.waitSync(testUtils.getStacksyncFolderPath() +"testRenamedFile.txt","localhost");
		logg.warn("Give some time to stacksync to do his job");
		
		try {
			Thread.sleep(testUtils.getSleepTime());
		} catch (InterruptedException e) {
			logg.error("Sleep failed.");
			assertTrue(false);
		}
		boolean testOk = false;
		for (Computer comp : testUtils.getComputerList()) {
			 testOk = testUtils.checkRemoteChecksum("testRenamedFile.txt", comp.getIp(), comp.getUsername(),comp.getPassword());
			logg.warn("CORRECT sync for FILE name modification:" + testOk+ " for computer:" + comp.getIp());
			if (!testOk){
				logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
				break;
			}
		}
		assertTrue(testOk);

		
		
    }

    /**
     * Testing if renaming a folder generates problems it's expected to just change the folder name and don't affect it's content
     */
    public  void testRenameFolder(){

         
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
		logg.warn("testing MODIFIED FOLDER NAME");
		String testFolderName = testUtils.createNewFolder("testRenameFolderOld","testfile1","testfile2","file1.txt","file2.txt");		
		if(testFolderName == null){
			assertTrue(false);
		}
		logg.warn("Waiting until the folder is sync");
		testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFolderName,"localhost");
		logg.warn("Folder sync we can continue now");
		logg.warn("Give some time to stacksync to do his job");	
		File filesrc = new File(testUtils.getStacksyncFolderPath() + testFolderName);
		File filedest = new File(testUtils.getStacksyncFolderPath()+"testRenamedFolder");
		filesrc.renameTo(filedest);
		try {
			Thread.sleep(testUtils.getSleepTime());
		} catch (InterruptedException e) {
			logg.error("Sleep failed.");
			assertTrue(false);			
		}
		boolean testOk = false;
		for (Computer comp : testUtils.getComputerList()) {
		 testOk = testUtils.checkRemoteFolderChecksum("testRenamedFolder", comp.getIp(), comp.getUsername(),comp.getPassword());
			logg.warn("CORRECT sync for new FOLDER:" + testOk	+ " for computer:" + comp.getIp());
			if (!testOk){
				logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
				break;
			}
		}
		assertTrue(testOk);
    }
   
}
