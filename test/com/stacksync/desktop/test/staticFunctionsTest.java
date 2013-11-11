/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import org.apache.log4j.extras.DOMConfigurator;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.index.requests.NewIndexRequest;
import com.stacksync.desktop.util.FileUtil;
import com.stacksync.desktop.watch.remote.ChangeManager;

/**
 *
 * @author fox
 */
public class staticFunctionsTest {
    
    private static final Environment env = Environment.getInstance();
    private static DatabaseHelper db = DatabaseHelper.getInstance();
    
    public static int MAXIMUMTIMEOUT = 5000;
    
    public static String fileName1 = File.separator + "file1";
    public static String fileName2 = File.separator + "file2";
    public static String fileName3 = File.separator + "file3";
    
    public static String badFileName1 = File.separator + "filèïç·ñô1";
    
    public static String ignoreFileName1 = File.separator + ".ignore-file1";
    
    
    public static Folder initConfig(Config config) throws ConfigException, IOException{
        
        try {
            File file = new File(env.getDefaultUserConfigDir() + File.separator + Constants.LOGGING_DEFAULT_FILENAME);
            DOMConfigurator.configure(file.toURI().toURL());
        } catch(NullPointerException e){
            System.out.println("No log4j config file was found no logs will be saved for this stacksync instance please make sure LogProperties.xml file is correctly placed " + e.toString());
        } catch (MalformedURLException ex) {
            System.out.println("No log4j config file was found no logs will be saved for this stacksync instance please make sure LogProperties.xml file is correctly placed " + ex.toString());
        }
        
        InputStream is = Environment.class.getResourceAsStream("/org/stacksync/test/config.xml");
                
        File tmpDir = new File(env.getAppDir().getAbsolutePath() + File.separator + "tmpdb");
        if(!tmpDir.exists()){
            tmpDir.mkdirs();
        }
        config.setConfigDir(tmpDir);
        config.load(is);
        is.close();
        
        if(config.getProfiles().list().isEmpty()){
            throw new ConfigException("Error can't load the profile!");
        }
        
        Profile profile = config.getProfiles().get(1);
        if(profile.getFolders().list().isEmpty()){
            throw new ConfigException("The profile hasn't the folder configuration!");
        }
        
        cleanCache(config.getCache().getFolder());
        
        profile.getUploader().start();
        ChangeManager cm = profile.getRemoteWatcher().getChangeManager();
        cm.start();
        
        Tray tray = Tray.getInstance(); 
        tray.setStartDemonOnly(true);
        
        Folder root = profile.getFolders().list().get(0);
        return root;
    }
    
    
    public static void cleanCache(File directory){        
        FileUtil.deleteRecursively(directory);
        directory.mkdirs();
    }
            
    
    public static File createFile(String fileName, String content) {
        File file = new File(fileName);
                
        try {
            
            if(file.exists()){
                file.delete();
            }
            
            FileOutputStream fis = new FileOutputStream(file);
            fis.write(content.getBytes());
            fis.close();
        } catch (FileNotFoundException ex) {
            file = null;
        } catch (IOException ex){
            file = null;
        }
        
        return file;
    }
    
    public static CloneFile indexNewRequest(Folder root, File file, CloneFile previousVersion) throws InterruptedException{
        NewIndexRequest index = new NewIndexRequest(root, file, previousVersion, -1);
        index.process();
        
        System.out.println("Sleep!!!!\n\n");
        Thread.sleep(5000); 
        CloneFile dbFile = db.getFile(root, file);
        System.out.println(dbFile);
        
        return dbFile;
    }
    
}
