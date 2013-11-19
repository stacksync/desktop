package com.stacksync.desktop.logging;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class LogController {
    
    private static final int TIMES = 3;
    
    private static LogController instance;
    private HashMap<Exception, LinkedList<Long>> exceptions;
    
    private LogController() {
        
        this.exceptions = new HashMap<Exception, LinkedList<Long>>();
    }
    
    public static LogController getInstance() {
        if (instance == null) {
            instance = new LogController();
        }
        return instance;
    }
    
    public boolean canBeSent(Exception ex) {
        
        LinkedList<Long> timestamps = exceptions.get(ex);
        
        if (timestamps == null) {
            return true;
        }
        
        this.cleanTimestampList(timestamps);
        
        if (timestamps.size() < TIMES) {
            return true;
        }
        
        return false;
    }
    
    private void cleanTimestampList(LinkedList<Long> timestamps) {
        
        Calendar cal = Calendar.getInstance();  
        cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) - 10); 
        long anHourAgo = cal.getTimeInMillis();
        
        // Remove old timestamps
        Iterator<Long> iter = timestamps.iterator();
        while (iter.hasNext()){
            Long time = iter.next();
            if (anHourAgo > time) {
                iter.remove();
            }
        }
    }
    
    public void addLogSent(Exception ex) {
        
        LinkedList<Long> timestamps = exceptions.get(ex);
        
        if (timestamps == null) {
            timestamps = new LinkedList<Long>();
        }
        
        Calendar cal = Calendar.getInstance();
        Long currentTime = cal.getTimeInMillis();
        timestamps.add(currentTime);
        
        exceptions.put(ex, timestamps);
    }
    
}
