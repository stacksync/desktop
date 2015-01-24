/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.workspace;

import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.test.utils.DatabaseUtils;
import java.io.File;
import javax.persistence.EntityManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cotes
 */
public class WorkspaceControllerTest {
    
    private static DatabaseHelper databaseHelper;
    private static EntityManager entityManager;
    
    public WorkspaceControllerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws ConfigException {
        databaseHelper = DatabaseHelper.getInstance();
        
        File configFolder = new File("database_test");
        // Create and copy config file
        File configFile = DatabaseUtils.prepareConfigFile();
        databaseHelper.initializeDatabase(configFolder.getAbsolutePath(), DatabaseUtils.getDBConfigNode(configFile));
        entityManager = databaseHelper.getEntityManager();
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    
    public void persist(Object o){
        entityManager.getTransaction().begin();
        entityManager.persist(o);
        entityManager.getTransaction().commit();
    }
}
