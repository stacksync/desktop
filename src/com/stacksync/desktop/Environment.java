/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Device;
import org.w3c.dom.Document;

/**
 *
 * @author Philipp C. Heckel
 */
public class Environment {
    private final Logger logger = Logger.getLogger(Environment.class.getName());
    private static Environment instance;
    
    public enum OperatingSystem { Windows, Linux, Mac };

    private OperatingSystem operatingSystem;
    private String architecture;
    
    private String defaultUserHome;
    private File defaultUserConfDir;
    private File defaultUserConfigFile;
    
    private File appDir;
    private File appBinDir;
    private File appResDir;
    private File appConfDir;
    private File appLibDir;
    
    /**
     * Local computer name / host name.
     */
    private static String deviceName;

    /**
     * Local user name (login-name).
     */
    private static String userName;


    public synchronized static Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
	
        return instance;
    }

    private Environment() {

        String homePath= "";
        // Check must-haves
        if (System.getProperty("stacksync.home") == null){            
            if(System.getProperty("user.dir") != null){
                homePath = System.getProperty("user.dir");
            } else{
                throw new RuntimeException("Property 'stacksync.home' must be set.");	
            }
        } else{
            homePath = System.getProperty("stacksync.home");
            File tryPath = new File(homePath + File.separator + "res");
            if(!tryPath.exists()){
                homePath = System.getProperty("user.dir");
            }
        }       

        // Architecture
        if ("32".equals(System.getProperty("sun.arch.data.model"))) {
            architecture = "i386";
        } else if ("64".equals(System.getProperty("sun.arch.data.model"))) {
            architecture = "amd64";
        } else {
            throw new RuntimeException("Stacksync only supports 32bit and 64bit systems, not '"+System.getProperty("sun.arch.data.model")+"'.");	
        }
           
        // Do initialization!
        defaultUserHome = System.getProperty("user.home") + File.separator;
        
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            operatingSystem = OperatingSystem.Linux;
            defaultUserConfDir = new File(defaultUserHome + "."+ Constants.APPLICATION_NAME.toLowerCase());	    
        } else if (osName.contains("windows")) {
            operatingSystem = OperatingSystem.Windows;
            if(osName.contains("xp")){//windows xp
                defaultUserConfDir = new File(defaultUserHome + "Application Data" + File.separator + Constants.APPLICATION_NAME.toLowerCase());
            } else { //windows 7, 8
                defaultUserConfDir = new File(defaultUserHome + "AppData" + File.separator + "Roaming" + File.separator + Constants.APPLICATION_NAME.toLowerCase());
            }
        } else if (osName.contains("mac os x")) {
            operatingSystem = OperatingSystem.Mac;
            defaultUserConfDir = new File(defaultUserHome + "." + Constants.APPLICATION_NAME.toLowerCase());	    
        } else {
            throw new RuntimeException("Your system is not supported at the moment: " + System.getProperty("os.name"));
        }

        // Common values
        defaultUserConfigFile = new File(defaultUserConfDir.getAbsoluteFile() + File.separator + Constants.CONFIG_FILENAME);

        appDir = new File(homePath);
        appBinDir = new File(appDir.getAbsoluteFile()+File.separator+"bin");
        appResDir = new File(appDir.getAbsoluteFile()+File.separator+"res");
        appConfDir = new File(appDir.getAbsoluteFile()+File.separator+"conf");
        appLibDir = new File(appDir.getAbsoluteFile()+File.separator+"lib");

        // Errors
        if (!appDir.exists() ) {
            throw new RuntimeException("Could not find application directory at "+appResDir);
        }

        if (!appResDir.exists() ) {
            throw new RuntimeException("Could not find application resources directory at "+appResDir);
        }

        if (!appConfDir.exists() ) {
            throw new RuntimeException("Could not find application config directory at "+appConfDir);
        }

        if (!appLibDir.exists() ) {
            throw new RuntimeException("Could not find application library directory at "+appLibDir);
        }
               
        String defaultDeviceName;
        try { 
            defaultDeviceName = InetAddress.getLocalHost().getHostName();

            if(defaultDeviceName.length() > 10){
                defaultDeviceName = InetAddress.getLocalHost().getHostName().substring(0, 9);
            }
        } catch (UnknownHostException ex) { 
            logger.error("aplicationstarter#ERROR: cannot find host", ex);
            defaultDeviceName = "(unknown)"; 
        }
                
        if(defaultUserConfigFile.exists()){
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();            
            try {
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                
                Document doc = dBuilder.parse(defaultUserConfigFile);
                ConfigNode self = new ConfigNode(doc.getDocumentElement());
                Device device = new Device();
                device.load(self.findChildByName("device"));
                deviceName = device.getName();
                
                if(deviceName.isEmpty()){
                    deviceName = defaultDeviceName;
                }                
            } catch (Exception ex) {
                logger.error("ERROR: cant set machineName", ex);
                deviceName = defaultDeviceName;
            }
        } else{        
            deviceName = defaultDeviceName.replace("-", "_");
        }
        
        userName = System.getProperty("user.name");

        // GUI 
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    //java.util.Enumeration keys = UIManager.getDefaults().keys();
                    
            /*while (keys.hasMoreElements()) {
              Object key = keys.nextElement();
              Object value = UIManager.get (key);
                
              if (value instanceof FontUIResource) {
                  FontUIResource f = (FontUIResource) value;
                  f = new FontUIResource(f.getFamily(), f.getStyle(), f.getSize()-2);
                  System.out.println(key +" = "+value);
                    UIManager.put (key, f);
              
              }
            }*/
        } catch (Exception ex) {
            logger.error("aplicationstarter#Couldn't set native look and feel.", ex);
        }
    }

    public File getAppConfDir() {
        return appConfDir;
    }

    public File getAppDir() {
        return appDir;
    }

    public File getAppBinDir() {
        return appBinDir;
    }

    public File getAppResDir() {
        return appResDir;
    }

    public File getAppLibDir() {
        return appLibDir;
    }        
    
    public File getDefaultUserConfigFile() {
        return defaultUserConfigFile;
    }

    public File getDefaultUserConfigDir() {
        return defaultUserConfDir;
    }
    
    public String getDeviceName() {
        return deviceName.replace("-", "_");
    }

    public String getDeviceNameWithTimestamp() {
        // Machine stuff        
        java.util.Date date = new java.util.Date(); 
        java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat("yyyyMMddHHmm");
        
        return deviceName+sdf.format(date);
    }
    
    public String getUserName() {
        return userName;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public String getArchitecture() {
        return architecture;
    }    
    
    public String getDefaultUserHome() {
        return defaultUserHome;
    }
    
    public void main(String[] args) {
        Properties properties = System.getProperties();

        Enumeration e = properties.propertyNames();

        System.out.println("Properties");
        System.out.println("---------------");

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            System.out.println(key+" = "+System.getProperty(key));	    
        }

        System.out.println("ENV");
        System.out.println("---------------");

        for (Map.Entry<String,String> es : System.getenv().entrySet()) {
            System.out.println(es.getKey()+" = "+es.getValue());	
        }	
    }
}