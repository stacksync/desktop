/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myLogger;

/**
 *
 * @author sergi
 */
public class MyFunctionProcessLogger extends MyLogger{

    public static enum QUEUE_STATUS{
      NEW, CHECK, DELETED  
    };
    
    private final static String LOG_NAME = PATH +"func_process.log";
    private static MyFunctionProcessLogger instance;
    
    /**
     *
     * @return instance
     */
    public static synchronized MyFunctionProcessLogger getInstance(){
        if(instance == null){
            instance = new MyFunctionProcessLogger(LOG_NAME);
        }
        return instance;
    }
    
    /**
     *
     * @param name
     */
    public MyFunctionProcessLogger(String name){
        super(name);
    }
}
