/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myLogger;

/**
 *
 * @author sergi
 */
public class MyStorageLogger extends MyLogger{
    
    private final static String LOG_NAME = "/home/lab144/Desktop/storage.log";
    private static MyStorageLogger instance;
    
    /**
     *
     * @return instance
     */
    public static synchronized MyStorageLogger getInstance(){
        if(instance == null){
            instance = new MyStorageLogger(LOG_NAME);
        }
        return instance;
    }
    
    public MyStorageLogger(String name){
        super(name);
    }
}
