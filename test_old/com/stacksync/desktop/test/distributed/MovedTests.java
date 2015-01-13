/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.distributed;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author Marc
 */
public class MovedTests extends TestCase{
    
    private Logger logg = Logger.getLogger(MovedTests.class.getName());
    private TestUtils testUtils = TestUtils.getUtils();
    private ConsoleAppender log = new ConsoleAppender(new PatternLayout("$$$$%d{yyyy-MM-dd HH:mm:ss}#%p#[%t]#%c->%M#%m#%n"));
    
    
    /**
     * We copy a file from a folder to another and we check if the changes are correct
     */
    public  void testMovedFile(){
        
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
        
		logg.warn("testing MOVED FILE");
		try{
			String testFolderName = testUtils.createNewFolder("testMovedFileFolder1","testfile1","testfile2","file1.txt","file2.txt");
			if(testFolderName == null){
				assertTrue(false);
			}
			testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFolderName,"localhost");
			logg.warn("Created folder1");
			
			String testAuxFolderName = testUtils.createNewFolder("testMovedFileFolder2","testfile4","testfile5","file3.txt","file4.txt");		
			if(testAuxFolderName == null){
				assertTrue(false);
			}
			
			testUtils.waitSync(testUtils.getStacksyncFolderPath() + testAuxFolderName,"localhost");
			logg.warn("Created folder2");
			
			File testFile = new File(testUtils.getStacksyncFolderPath() + testFolderName+"/file1.txt");
			FileOutputStream fos= new FileOutputStream(testUtils.getStacksyncFolderPath() + testAuxFolderName+"/file1.txt");
	        byte[] fileByteArray = org.apache.commons.io.FileUtils.readFileToByteArray(testFile);
			fos.write(fileByteArray);
			fos.close();
			testFile.delete();
			
			testUtils.waitSync(testUtils.getStacksyncFolderPath() + testAuxFolderName,"localhost");
			boolean testOk = false;
			try {
				Thread.sleep(testUtils.getSleepTime());
			} catch (InterruptedException e) {
			}		
			for (Computer comp : testUtils.getComputerList()) {
                testOk = testUtils.checkRemoteChecksum(testAuxFolderName+"/file1.txt", comp.getIp(), comp.getUsername(),comp.getPassword());
				if (!testOk){
					break;
				}
			}
			assertTrue(testOk);
		}catch(IOException e){
			assertTrue(false);
		}

    }
    /**
     * We copy a folder from a folder to another folder and we check if the files are correct
     */
    public  void testMovedFolder(){
        
        
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
        
		logg.warn("testing MOVED FOLDER");
		try{
			String testFolderName = testUtils.createNewFolder("testfolder1","testfile1","testfile2","file1.txt","file2.txt");
			if(testFolderName == null){
				assertTrue(false);
			}			
			testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFolderName,"localhost");
			logg.warn("Folder1 created");
			
			String testFolderName2 = testUtils.createNewFolder("testfolder2","testfile4","testfile5","file3.txt","file4.txt");
			if(testFolderName2 == null){
				assertTrue(false);
			}
			testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFolderName2,"localhost");
			logg.warn("Folder2 created");
			
			String testAuxFolderName = testUtils.createNewFolder(testFolderName+"/movedFolder","testfile4","testfile5","file5.txt","file6.txt");
			if(testAuxFolderName == null){
				assertTrue(false);
			}
			testUtils.waitSync(testUtils.getStacksyncFolderPath() + testAuxFolderName,"localhost");
			logg.warn("Folder3 created");
			
			File originalFolder = new File (testUtils.getStacksyncFolderPath() + testAuxFolderName);
			File destinationFolder = new File (testUtils.getStacksyncFolderPath() + testFolderName2+"/movedFolder");
			testUtils.copyFolder(originalFolder, destinationFolder);
			logg.warn("Folder3 copied to the new destination");
			testUtils.waitSync(testUtils.getStacksyncFolderPath() + testAuxFolderName,"localhost");
			testUtils.deleteFolder(originalFolder);
			logg.warn("Folder3 deleted from the old source");
			boolean testOk = false;
			
			for (Computer comp : testUtils.getComputerList()) {
				testOk = testUtils.checkRemoteFolderChecksum(testFolderName, comp.getIp(), comp.getUsername(),comp.getPassword());
				if (!testOk){
					logg.warn("Test failed for computer: "+comp.getIp()+" aborting test" );
					break;
				}
			}
			assertTrue(testOk);

		}catch(IOException e){
		}
    }
}
