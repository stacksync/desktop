/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test;

import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author AST
 */
public class TestCreateWindowsLink {
    
    public static void main(String[] args) throws Exception {
        FileUtil.createWindowsLink("c:\\hola.lnk", "C:\\Documents and Settings\\AST\\AppData\\Roaming\\stacksync");
    }
}
