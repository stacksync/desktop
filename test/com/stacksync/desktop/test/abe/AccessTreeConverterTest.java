package com.stacksync.desktop.test.abe;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.stacksync.desktop.util.AccessTreeConverter;
import com.ast.cloudABE.kpabe.KPABE;

import com.stacksync.commons.models.abe.AccessTree;
import static junit.framework.TestCase.assertTrue;
import org.junit.Test;

/**
 *
 * @author marcruiz
 */
public class AccessTreeConverterTest {
    
    @Test
    public void checkAccessTrees() throws Exception  {
        
        System.out.println("Converting from ABEClient to Commons:");
        com.ast.cloudABE.accessTree.AccessTree accessTreeABEClient = KPABE.setAccessTree("(ATTRIBUTE_1 | ATTRIBUTE_2) &" +
				" (ATTRIBUTE_1 & ATTRIBUTE_3)");

        AccessTree accessTreeCommons = new AccessTree(AccessTreeConverter.transformTreeFromABEClientToCommons(accessTreeABEClient));
        
        System.out.println("ABE client: " + accessTreeABEClient);
        System.out.println("Commons: " + accessTreeCommons);
        assertTrue(accessTreeCommons.toString().equals(accessTreeABEClient.toString()));
        
        System.out.println("\nConverting from Commons to ABEClient:");
        
        com.ast.cloudABE.accessTree.AccessTree newAccessTreeABEClient = new com.ast.cloudABE.accessTree.AccessTree(AccessTreeConverter.transformTreeFromCommonsToABEClient(accessTreeCommons));
        System.out.println("Commons: " + accessTreeCommons);
        System.out.println("New ABE client: " +newAccessTreeABEClient);
        assertTrue(accessTreeCommons.toString().equals(newAccessTreeABEClient.toString()));
    }
}
