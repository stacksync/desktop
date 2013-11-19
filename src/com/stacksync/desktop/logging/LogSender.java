package com.stacksync.desktop.logging;

import com.stacksync.desktop.config.Config;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class LogSender {
    
    private static final Config config = Config.getInstance();
    
    private String serverURL;
    
    public LogSender() {
        this.serverURL = config.getLogApiRestUrl();
    }
    
    public boolean send(File compressedLog) throws IOException {
        
        boolean success = true;
        
        HttpClient client = HttpClientBuilder.create().build();
        HttpPut request = new HttpPut(serverURL+"?type=compress");
        request.addHeader("computer", config.getMachineName());
        
        // Set request parameters
        RequestConfig.Builder reqConf = RequestConfig.custom();
        reqConf.setSocketTimeout(2000);
        reqConf.setConnectTimeout(1000);
        request.setConfig(reqConf.build());

        setRequest(request, compressedLog);

        HttpResponse response = client.execute(request);

        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode != 201) {
            success = false;
        }
        
        return success;
    }
    
    private void setRequest(HttpPut request, File logFile) throws IOException {
        byte[] zipFileByteArray = FileUtils.readFileToByteArray(logFile);
        ByteArrayEntity requestEntity = new ByteArrayEntity(zipFileByteArray);
        request.setEntity(requestEntity);
    }
}
