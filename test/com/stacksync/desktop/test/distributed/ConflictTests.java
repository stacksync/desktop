/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.distributed;

import java.util.LinkedList;
import java.util.List;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 *
 * @author Marc
 */
public class ConflictTests extends TestCase{

    private Logger logg = Logger.getLogger(ConflictTests.class.getName());
    private TestUtils testUtils = TestUtils.getUtils();

   /**
     * we modify files at the same time this is suposed to generate a conflict and we look for all the posible files as conflcited copies
     */   
    public void testConflictSync() {
       if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
         }
        logg.warn("testing sync FILE conflict");
        String testFileName = testUtils.createNewFile("testConflicted.txt", "testing");
        if (testFileName == null) {
            assertTrue(false);
        }
        logg.warn("Waiting until the file is sync");
        testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFileName, "localhost");
        logg.warn("File sync we can continue now");
        logg.warn("Give some time to stacksync to do his job");
        List<String> md5List = new LinkedList<String>();

        try {
            Thread.sleep(testUtils.getSleepTime());
        } catch (InterruptedException e) {
            logg.error("Sleep failed.");
            assertTrue(false);
        }
        int compNumber = 0;
        logg.warn("Generating conflict");

        for (Computer comp : testUtils.getComputerList()) {
            compNumber++;
            String md5 = testUtils.generateConflict(testFileName, comp.getIp(), comp.getUsername(), comp.getPassword(), compNumber);
            if (md5 != null) {
                md5List.add(md5);
            }
        }
        logg.warn("Conflict generated");

        try {
            Thread.sleep(testUtils.getSleepTime());
        } catch (InterruptedException e) {
        }
        boolean testOk = false;

        for (Computer comp : testUtils.getComputerList()) {
            testOk = testUtils.checkMd5List("testConflicted", md5List, comp.getIp(), comp.getUsername(), comp.getPassword());
            if (testOk == false) {
                break;
            }
        }
        assertTrue(testOk);


    }


    /**
     * We delete a file in all the clients at the same time and we check if that generates a random empty conflicted copy wich will be wrong
     */
    public void testFileConflictDoubleDeleted() {
        
         if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
         }
        logg.warn("Testing File Deleted double");
        String testFileName = testUtils.createNewFile("test.txt", "testing");
        if (testFileName == null) {
            assertTrue(false);
        }
        logg.warn("Filecreated");
        testUtils.waitSync(testUtils.getStacksyncFolderPath() + testFileName, "localhost");
        for (Computer comp : testUtils.getComputerList()) {
            testUtils.remoteFileDelete(testFileName, comp.getIp(), comp.getUsername(), comp.getPassword());
        }
        try {
            Thread.sleep(testUtils.getSleepTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean itsok = false;
        for (Computer comp : testUtils.getComputerList()) {
            itsok = testUtils.checkRemoteFileDelete(testFileName, comp.getIp(), comp.getUsername(), comp.getPassword());
            logg.warn("CORRECT sync for double deleted File:" + itsok + " for computer:" + comp.getIp());
            if (!itsok) {
                logg.warn("Test failed for computer: " + comp.getIp() + " aborting test");
                break;
            }
        }
        assertTrue(itsok);
    }

    
    /**
     *We add multiple files with diferent checksums at the same time and we expect
     * that one copy will win and all the others will show up as conflicted copies,
     * so we check all the checksums to find all the possible versions
     */
    public void testConflictedNewFile() {
        
        if( !logg.isAttached(testUtils.getConsoleAppender())){
                logg.addAppender(testUtils.getConsoleAppender());
        }
        String simpleFileName = "test2.txt";
        List<String> md5List = new LinkedList<String>();
        int compNumber = 0;
        for (Computer comp : testUtils.getComputerList()) {
            compNumber++;
            String md5 = testUtils.generateConflict(simpleFileName, comp.getIp(), comp.getUsername(), comp.getPassword(), compNumber);
            if (md5 != null) {
                md5List.add(md5);
            }
        }

        try {
            Thread.sleep(testUtils.getSleepTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean testOk = false;
        for (Computer comp : testUtils.getComputerList()) {
            testOk = testUtils.checkMd5List("test2", md5List, comp.getIp(), comp.getUsername(), comp.getPassword());
            if (testOk == false) {
                break;
            }
        }
        assertTrue(testOk);
    }
}
