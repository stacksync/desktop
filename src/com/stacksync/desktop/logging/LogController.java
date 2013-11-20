package com.stacksync.desktop.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
        cal.set(Calendar.HOUR, cal.get(Calendar.HOUR) - 1); 
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
    
    public void addFailLog(Exception ex, String logsPath, String fileName)
            throws IOException, ClassNotFoundException {
        
        String path = logsPath + File.separator + "objs";
        HashMap<String, Exception> fileToException = this.readFailedLogs(path);
        
        fileToException.put(fileName, ex);
        
        this.saveFailedLogs(path, fileToException);
        
    }
    
    private HashMap<String, Exception> readFailedLogs(String path)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        
        File metadataFile = new File(path);
        HashMap<String, Exception> fileToException;
        if (metadataFile.exists()) {
           
            FileInputStream inputStream = new FileInputStream(metadataFile);
            ObjectInputStream in = new ObjectInputStream(inputStream);
            fileToException = (HashMap<String, Exception>) in.readObject();

            in.close();
            inputStream.close();
        } else {
            fileToException = new HashMap<String, Exception>();
        }
        
        return fileToException;
    }
    
    private void saveFailedLogs(String path, HashMap<String, Exception> object)
            throws IOException {
        
        FileOutputStream fileOut = new FileOutputStream(path);
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(object);
        out.close();
        fileOut.close();
        
    }
    
    public HashMap<String, Exception> getFailedLogs(String path) 
            throws FileNotFoundException, IOException, ClassNotFoundException {
        
        return this.readFailedLogs(path + File.separator + "objs");
    }
    
    public void setFailedLogs(String path, HashMap<String, Exception> object)
            throws IOException {
        
        this.saveFailedLogs(path + File.separator + "objs", object);
    }
    
    public void removeFailedFile(String path) {
        
        new File(path + File.separator + "objs").delete();
    }
    
}
