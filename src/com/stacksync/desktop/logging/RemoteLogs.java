/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import org.apache.commons.io.FileUtils;

public class RemoteLogs {
    
    private static final Config config = Config.getInstance();
    private static final Environment env = Environment.getInstance();
    
    private String logFolder;
    private String logFilePath;
    private String failedLogsPath;
    
    private static RemoteLogs instance;
    
    private boolean active;
    
    private RemoteLogs() {
        
        String configDir = env.getDefaultUserConfigDir().getAbsolutePath();
        this.logFolder = configDir + File.separator + "logs";
        this.logFilePath = this.logFolder + File.separator + "TempLog.log";
        this.failedLogsPath = this.logFolder + File.separator + "failedLogs";
    }
    
    public synchronized static RemoteLogs getInstance() {
        
        if (instance == null) {
            instance = new RemoteLogs();
        }
        
        return instance;
        
    }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    
    
    /*private void sendLogs() throws FileNotFoundException, IOException, IllegalStateException{
        String serverUrl = config.getLogApiRestUrl();

        DefaultHttpClient client = new DefaultHttpClient();
        HttpPut request = new HttpPut(serverUrl+"?type=compress");
        request.addHeader("computer", config.getMachineName());
        request.getParams().setIntParameter("http.socket.timeout", 2000);

        File logFile = new File(logFilePath);
        File logCompressed = new File(logFolder + "/temp.gz");
        compressAndSaveFile(logFile, logCompressed);
        setRequest(request, logCompressed);

        HttpResponse response = client.execute(request);

        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode == 201) {
            BufferedWriter out = new BufferedWriter(new FileWriter(logFilePath, false));
            out.write("");
            out.close();
        } else {
            saveFailedLogs();
        }
    }*/

    private static void setRequest(HttpPut request, File logFile) throws IOException {
        
        byte[] zipFileByteArray = FileUtils.readFileToByteArray(logFile);
        ByteArrayEntity requestEntity = new ByteArrayEntity(zipFileByteArray);
        request.setEntity(requestEntity);
        logFile.delete();
    }
    
    private static void compressAndSaveFile(File from, File to) throws IOException {
    
        byte[] toCompressByteArray = FileUtils.readFileToByteArray(from);
        GZIPOutputStream zipOut = new GZIPOutputStream(new FileOutputStream(to));
        zipOut.write(toCompressByteArray);
        zipOut.close();
        
    }

    private void saveFailedLogs() {
        
        File logFile = new File(logFilePath);
        File failedFilesDir = new File(failedLogsPath);
        if (!failedFilesDir.exists()){
            failedFilesDir.mkdir();
        }
        
        try {
            int fileNumber = failedFilesDir.list().length + 1;

            File failedLogFile = new File(failedLogsPath + File.separator + "log" + fileNumber + ".gz");
            
            //Compress file
            compressAndSaveFile(logFile, failedLogFile);

            //Clean log file
            BufferedWriter out;
            out = new BufferedWriter(new FileWriter(logFile, false));
            out.write("");
            out.close();
        } catch (IOException ex) {
            //old log file is corrupted or doesn't exist
        }
    }
    
    public synchronized void sendLog(Exception ex) {

        if (!active) {
            return;
        }
        
        /*try {
            sendLogs();
        } catch (FileNotFoundException ex) {
            //Can't save failed logs cause logfile does not exist
        } catch (IOException ioex) {
            //logs cant be uploaded cause remote log api is down          
            saveFailedLogs();
        }catch(IllegalStateException exception){
            System.out.println("Error parsing server url");
            saveFailedLogs();
        }*/
    }

    public void sendFailedLogs() {
        String serverUrl = config.getLogApiRestUrl();

        DefaultHttpClient client = new DefaultHttpClient();
        HttpPut request = new HttpPut(serverUrl+"?type=compress");
        request.getParams().setIntParameter("http.socket.timeout", 2000);
        request.addHeader("computer", config.getMachineName());

        File failedDir = new File(failedLogsPath);
        if (!failedDir.exists()) {
            return;
        }
        
        String[] fileList = failedDir.list();
        for (String log : fileList) {
            boolean fileAppendedRequest = true;
            try {
                
                File logFile = new File(failedLogsPath + "/" + log);
                setRequest(request, logFile);

            } catch (IOException ex) {
                System.out.println("File is corrupted");
                fileAppendedRequest = false;
            }
            
            if (fileAppendedRequest) {
                try {
                    HttpResponse response = client.execute(request);
                    HttpEntity entity = response.getEntity();
                    EntityUtils.consume(entity);
                    //only if the request works properly the file is deleted
                    File file = new File(failedLogsPath + File.separator + log);
                    file.delete();
                } catch (IOException exep) {
                    System.out.println("Server Connection failed will try again next time: "+exep.toString());
                    
                    break;
                }
                catch(IllegalStateException exception){
                    System.out.println("Error parsing server url"+exception.toString());
                    break;
                }

            }
        }
    }
}
