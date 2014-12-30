/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.db;

import com.stacksync.desktop.config.Database;
import java.io.File;

/**
 *
 * @author cotes
 */
public class TestItem {
    
    private static Database database;
    
    public TestItem() {
        database = new Database();
    }
    
    //@BeforeClass
    public static void setUpClass() {
        
        // Create and copy config file
        File configFolder = new File("database_test");
        configFolder.mkdir();
        
        // Read config file and load database
        //loadDatabase();
    }
    
    public static void loadDatabase(){
        
    }
    
    //@AfterClass
    public static void tearDownClass() {
        File configFolder = new File("database_test");
        configFolder.delete();
    }
    
    //@Before
    public void setUp() {
    }
    
    //@After
    public void tearDown() {
    }

    //@Test
    public void test() {
        System.out.println("Hello");
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    
    public static void main(String[] args) {
        TestItem test = new TestItem();
        TestItem.setUpClass();
        TestItem.tearDownClass();
        test.test();
    }
}
