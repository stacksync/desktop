package com.stacksync.desktop.test.logs;

import com.stacksync.desktop.Constants;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.log4j.Logger;
import org.apache.log4j.extras.DOMConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogsTests {
    
    private static final Logger logger = Logger.getLogger(LogsTests.class.getName());
    private static final Environment env = Environment.getInstance();
    private static final Config config = Config.getInstance();
    
    @BeforeClass
    public static void testSetup() throws ConfigException {
        
        loadLogsConfig();
        loadClientConfig();
        logger.info("Start log test.");
    }
    
    private static void loadLogsConfig() {
        
        File fileLogConfig = new File(env.getDefaultUserConfigDir() + File.separator + Constants.LOGGING_DEFAULT_FILENAME);        
        if(!fileLogConfig.exists()){
            File fileLogConfigTemplate = new File(env.getAppConfDir()+ File.separator + Constants.LOGGING_DEFAULT_FILENAME);            
            try {
                String content = FileUtil.readFileToString(fileLogConfigTemplate);
                File file = new File(env.getDefaultUserConfigDir().getAbsolutePath() + File.separator + "logs");
                
                content = content.replace("REPLACED_BY_APPLICATION", file.toURI().toURL().getFile());
                FileUtil.writeFile(content, fileLogConfig);                
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
        
        try{
            File file = new File(env.getDefaultUserConfigDir() + File.separator + Constants.LOGGING_DEFAULT_FILENAME);
            DOMConfigurator.configure(file.toURI().toURL());
        } catch(NullPointerException e){
            System.out.println("No log4j config file was found no logs will be "
                    + "saved for this stacksync instance please make sure "
                    + "LogProperties.xml file is correctly placed " + e.toString());
        } catch (MalformedURLException ex) {
            System.out.println("No log4j config file was found no logs will be "
                    + "saved for this stacksync instance please make sure "
                    + "LogProperties.xml file is correctly placed " + ex.toString());
        }
    }
    
    private static void loadClientConfig() throws ConfigException {
        
        File configurationDir = env.getAppConfDir();                
        if(configurationDir.exists()){
            config.load();
            if (config.getProfiles().list().isEmpty()) {
                File folder = new File(config.getConfDir()+File.separator+Constants.CONFIG_DATABASE_DIRNAME);                    
                File configFile = new File(config.getConfDir()+File.separator+Constants.CONFIG_FILENAME);

                folder.delete();
                configFile.delete();
                config.load();
            } 
        } else { // new configuration
            config.load();
        }
    }
    
    @Test
    public void sendLog() {
        
        logger.info("Trying to send a log.");
        logger.info("This test has to save a compressed file in the failedLogs folder");
        
        try {
            throw new IOException("This is an exception");
        } catch (IOException e){
            logger.error("There has been an exception: ", e);
            RemoteLogs.getInstance().sendLog(e);
        }
        
    }
    
    @Test
    public void sendFailedLog() {
        RemoteLogs.getInstance().retrySendLogs();
    }
    
}
