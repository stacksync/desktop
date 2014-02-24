/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sergi
 */
public abstract class MyLogger {

    public enum ACTION {

        START, STOP
    };
    protected File logFile;
    protected BufferedWriter buff;

    protected final static String PATH = "/home/lab144/Desktop/";
    
    public MyLogger(String name){
        try {
            logFile = new File(name); 
            buff = new BufferedWriter(new FileWriter(logFile));
        } catch (IOException ex) {
            Logger.getLogger(MyLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void info(long time, String clazz, String method, String path, String fileName, boolean isFolder, ACTION action, String info) {
        try {
            String message = time + "\t" + clazz + "\t" + method + "\t" + path  + "\t" + fileName + "\t" + isFolder + "\t" + action + "\t" + info + "\n";
            System.out.println(message);
            buff.write(message);
            buff.flush();
        } catch (IOException ex) {
            Logger.getLogger(MyLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /*
    public synchronized void info(long time, String clazz, String method, String fileName, ACTION action, String info) {
        try {
            String message = time + "\t" + clazz + "\t" + method + "\t" + fileName + "\t" + action + "\t" + info + "\n";
            System.out.println(message);
            buff.write(message);
            buff.flush();
        } catch (IOException ex) {
            Logger.getLogger(MyLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */
    
    public void closeWriter(){
        try {
            buff.close();
        } catch (IOException ex) {
            Logger.getLogger(MyLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
