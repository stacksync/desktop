/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.distributed;

import java.io.File;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import static org.junit.Assert.*;
/**
 *
 * @author Marc
 */
public class DeletedTests extends TestCase{
    
    private Logger logg = Logger.getLogger(DeletedTests.class.getName());
    private TestUtils testUtils = TestUtils.getUtils();    
    
	/**
     * we delete a file it's expected to not find it for each machine
     */
    public  void testDeletedFile() {
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
		
        logg.warn("testing DELETED FILE");
		String testFileName = "test.txt";
		File testFile = new File(testUtils.getStacksyncFolderPath() + testFileName);
		testFile.delete();
		logg.warn("Give some time to stacksync to do his job");
		
		try {
			Thread.sleep(testUtils.getSleepTime());
		} catch (InterruptedException e) {
		}		
		boolean itsok = false;
		for (Computer comp : testUtils.getComputerList()) {
			itsok = testUtils.checkRemoteFileDelete(testFileName, comp.getIp(), comp.getUsername(),comp.getPassword());
			logg.warn("CORRECT sync for deleted File:" + itsok	+ " for computer:" + comp.getIp());

            if (!itsok){
                logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
                break;
            }
		}
		assertTrue(itsok);
	}
	
	/**
     * We delete the folder and we expect to not find it for each machine
     */
    public void testDeletedFolder() {
        
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
		logg.warn("testing DELETED FOLDER");
		String testFolderName = "testNewFolder";
		File testFile = new File(testUtils.getStacksyncFolderPath() + testFolderName);
		testUtils.deleteFolder(testFile);
		logg.warn("Give some time to stacksync to do his job");
		try {
			Thread.sleep(testUtils.getSleepTime());
		} catch (InterruptedException e) {
		}		
		boolean itsok = false;
		for (Computer comp : testUtils.getComputerList()) {
			itsok = testUtils.checkRemoteFileDelete(testFolderName+"/file1.txt", comp.getIp(), comp.getUsername(),comp.getPassword());
			logg.warn("CORRECT sync for deleted Folder:" + itsok	+ " for computer:" + comp.getIp());
            if (!itsok){
                logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
                break;
            }
		}
		assertTrue(itsok);
	}

    /**
     *we generate a folder with 5 levels and we delete it and we check if the files and the folder are deleted for each machine
     */
    public  void testDelete5LevelsFolder(){
        
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
    	String testFolderName= "test5LevelsFolder";
    	boolean correctCreation = testUtils.createFolderLevels(5, testFolderName);
    	if(!correctCreation){
    		assertTrue(false);
    	}
		File originalFolder = new File(testUtils.getStacksyncFolderPath() + testFolderName+"level0");
		testUtils.deleteFolder(originalFolder);
		logg.warn("All Folders Deleted");
		try {
			Thread.sleep(testUtils.getSleepTime());
		} catch (InterruptedException e) {
		}
		boolean ok = false;
		for (Computer comp : testUtils.getComputerList()) {
			ok = testUtils.checkRemoteFileDelete(testFolderName+"level0/file1.txt", comp.getIp(), comp.getUsername(),comp.getPassword());
			logg.warn("CORRECT sync for 5 levels deleted Folder:" + ok	+ " for computer:" + comp.getIp());
            if (!ok){
                logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
                break;
            }
		}
		assertTrue(true);

		


    }
}
