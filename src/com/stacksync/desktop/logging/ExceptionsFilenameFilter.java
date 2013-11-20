/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.logging;

import java.io.File;
import java.io.FilenameFilter;

public class ExceptionsFilenameFilter implements FilenameFilter {

    private String start;
    
    public ExceptionsFilenameFilter(String start) {
        this.start = start;
    }
    
    @Override
    public boolean accept(File dir, String name) {
        return name.startsWith(this.start);
    }
    
}
