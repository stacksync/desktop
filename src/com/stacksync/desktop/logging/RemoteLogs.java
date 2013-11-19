package com.stacksync.desktop.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.stacksync.desktop.Environment;
import java.io.FileOutputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.FileUtils;

public class RemoteLogs {
    
    private static final Environment env = Environment.getInstance();
    
    private static RemoteLogs instance;
    
    private String logFolder;
    private String logFilePath;
    private String failedLogsPath;
    
    private boolean active;
    
    private LogSender sender;
    private LogController controller;
    
    private RemoteLogs() {
        
        String configDir = env.getDefaultUserConfigDir().getAbsolutePath();
        this.logFolder = configDir + File.separator + "logs";
        this.logFilePath = this.logFolder + File.separator + "TempLog.log";
        this.failedLogsPath = this.logFolder + File.separator + "failedLogs";
        this.sender = new LogSender();
        this.controller = LogController.getInstance();
        this.active = true; // Active by default!!! Change from config.xml...
    }
    
    public synchronized static RemoteLogs getInstance() {
        
        if (instance == null) {
            instance = new RemoteLogs();
        }
        
        return instance;
    }
    
    public synchronized void sendLog(Exception generatedException) {

        if (!active || !controller.canBeSent(generatedException)) {
            return;
        }
        
        boolean success;
        File compressedLog = null;
        try {
            // Compress log file and save to temp.gz file
            File logFile = new File(logFilePath);
            compressedLog = new File(logFolder + "/temp.gz");
            this.compressAndSaveFile(logFile, compressedLog);
            
            success = this.sender.send(compressedLog);
            
            if (success) {
                this.cleanLog();
                compressedLog.delete();
                this.controller.addLogSent(generatedException);
            }
            
        } catch (IOException ex) {        
            success = false;
        }
        
        if (!success && compressedLog != null) {
            this.saveFailedLogs(compressedLog, generatedException);
        }
    }
    
    public void cleanLog() {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(logFilePath, false));
            out.write("");
            out.close();
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }
    
    public void retrySendLogs() {
        
        if (!active) { 
            return;
        }
        
        File failedDir = new File(failedLogsPath);
        if (!failedDir.exists()) {
            return;
        }
        
        String[] fileList = failedDir.list();
        for (String log : fileList) {
                
            File logFile = new File(failedLogsPath + "/" + log);
            try {
                //logFile is already compressed
                boolean success = this.sender.send(logFile);
                if (success) {
                    logFile.delete();
                }
            } catch (IOException ex) {
                // TODO
            }
            
        }
        
    }
    
    private void saveFailedLogs(File compressedLog, Exception generatedException) {
        
        File failedFilesDir = new File(failedLogsPath);
        if (!failedFilesDir.exists()){
            failedFilesDir.mkdir();
        }
        
        // TODO check if this gz already exist to add a number
        String fileName = generatedException.getClass().getName();
        File failedLogFile = new File(failedLogsPath + File.separator + fileName + ".gz");
        try {
            FileUtils.moveFile(compressedLog, failedLogFile);
        } catch (IOException ex) {
            // TODO remove?? try again??
        }
        
    }
    
    private void compressAndSaveFile(File from, File to) throws IOException {
    
        byte[] toCompressByteArray = FileUtils.readFileToByteArray(from);
        GZIPOutputStream zipOut = new GZIPOutputStream(new FileOutputStream(to));
        zipOut.write(toCompressByteArray);
        zipOut.close();
        
    }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
