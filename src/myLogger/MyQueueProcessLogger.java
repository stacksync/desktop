/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myLogger;

import static myLogger.MyLogger.PATH;
/**
 *
 * @author sergi
 */
public class MyQueueProcessLogger extends MyLogger{

    private final static String LOG_NAME = PATH + "queue_process.log";
    private static MyQueueProcessLogger instance;
    
    /**
     *
     * @return instance
     */
    public static synchronized MyQueueProcessLogger getInstance(){
        if(instance == null){
            instance = new MyQueueProcessLogger(LOG_NAME);
        }
        return instance;
    }
    
    public MyQueueProcessLogger(String name){
        super(name);
    }

    /**
     *
     * @param time
     * @param clazz
     * @param method
     * @param action
     * @param info
     */
    /*
    public void info(long time, String clazz, String method, ACTION action, String name, String info) {
        try {
            String message = time + "\t" + clazz + "\t" + method + "\t" + action + "\t" + name + "\t" + info + "\n";
            System.out.println(message);
            buff.write(message);
            buff.flush();
        } catch (IOException ex) {
            Logger.getLogger(MyLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */
}
