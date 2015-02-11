/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.abe;

import com.ast.cloudABE.GUI.UIUtils;

/**
 *
 * @author javigd
 */
public class AbeGeneratePolicy {

    private static final String DEFAULT_ACCESS_STRUCT = "(MarketingA | (DesignA & DesignB))";
    private static final String RESOURCES_PATH = "./resources/conf/abe/";

    public static void main(String[] args) throws Exception {
        while (true) {
            /* Set the new user access policy */
            String attSet = UIUtils.getAccessStructure(RESOURCES_PATH, DEFAULT_ACCESS_STRUCT);
            System.out.println("[NewUser] Setting up access logical expression to: " + attSet);
        }
    }
}
