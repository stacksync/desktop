/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.abe;

import com.ast.cloudABE.GUI.UIUtils;
import com.ast.cloudABE.cloudABEClient.CloudABEClient;
import com.ast.cloudABE.cloudABEClient.CloudABEClientAdapter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *
 * @author javigd
 */
public class AbeGeneratePolicy {

    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    private static final String RESOURCES_PATH = "./resources/conf/abe/";
    private static CloudABEClient cabe;
    private static BufferedReader bufferRead;
    
    public static void main(String[] args) throws Exception {
        while (true) {
            bufferRead = new BufferedReader(new InputStreamReader(System.in));
            /* Set the new user access policy */
            String attSet = UIUtils.getAccessStructure(RESOURCES_PATH, DEFAULT_ACCESS_STRUCT);
            System.out.println("[ABEGeneratePolicy] Setting up access logical expression to: " + attSet + "... confirm? (Y/n)");
            
            if(bufferRead.readLine().equalsIgnoreCase("Y")) {
                cabe = new CloudABEClientAdapter(RESOURCES_PATH);
                cabe.setupABESystem(0, attSet);
                System.exit(0);
            }
        }
    }
}
