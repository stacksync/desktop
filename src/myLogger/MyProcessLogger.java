/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myLogger;

/**
 *
 * @author sergi
 */
public class MyProcessLogger extends MyLogger{

    private final static String LOG_NAME = "/home/milax/Escriptori/process.log";
    private static MyProcessLogger instance;
    
    /**
     *
     * @return instance
     */
    public static synchronized MyProcessLogger getInstance(){
        if(instance == null){
            instance = new MyProcessLogger(LOG_NAME);
        }
        return instance;
    }
    
    public MyProcessLogger(String name){
        super(name);
    }
}
