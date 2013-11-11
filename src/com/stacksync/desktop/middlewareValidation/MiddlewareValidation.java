/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.middlewareValidation;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author pgarcia
 */
public class MiddlewareValidation {

    private static final Logger logger = Logger.getLogger(MiddlewareValidation.class.getName());
    private static HttpClient client = new DefaultHttpClient();
    
    private static void doGet(String line) {
        try {
            
            HttpGet get = new HttpGet("http://10.21.2.18:8080");
            get.addHeader("info", line);
            
            HttpResponse resp = client.execute(get);
            EntityUtils.consumeQuietly(resp.getEntity()); 
            
            logger.info("Do get server!!!!" + get.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void PrintOperationClient(String op, String fileName, String machine) {

        String line = op + "|" + fileName;
        doGet(line);

    }

    public static void PrintOperationServer(String op, String fileName, String machine) {
        String line = op + "|" + fileName;
        doGet(line);
    }

    public static void main(String[] args) {
        String hola = "new|hello";
        doGet(hola);
    }
}
