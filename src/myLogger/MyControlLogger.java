/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package myLogger;

import com.stacksync.syncservice.models.ObjectMetadata;
import java.util.List;

/**
 *
 * @author sergi
 */
public class MyControlLogger extends MyLogger {

    private final static String LOG_NAME = "/home/milax/Escriptori/control.log";
    private static MyControlLogger instance;

    /**
     *
     * @return instance
     */
    public static synchronized MyControlLogger getInstance() {
        if (instance == null) {
            instance = new MyControlLogger(LOG_NAME);
        }
        return instance;
    }

    public MyControlLogger(String name) {
        super(name);
    }

    public synchronized void info(long time, String clazz, String method, List<ObjectMetadata> commitObjects, ACTION action, String info) {
        for (ObjectMetadata obj : commitObjects) {
            super.info(time, clazz, method, obj.getFilePath(),obj.getFileName(), obj.isFolder(), action, obj.getStatus());
        }
    }
}
