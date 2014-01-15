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
package com.stacksync.desktop.config;

import com.stacksync.desktop.config.profile.BrokerProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.util.FileUtil;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStreamWriter;
import org.w3c.dom.Document;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Config {
    // Note: Do NOT add a logger here, as the logger needs the Config instance.

    private final Logger logger = Logger.getLogger(Config.class.getName());
    private final Environment env = Environment.getInstance();
    private static final Config instance = new Config();
    private File configDir;
    private File configFile;
    private Encryption encryption;
    private Document doc;
    private ConfigNode self;
    private String logApiRestUrl;
    // Config values
    private String userName;
    private Device device;
    private String queueName;
    private boolean autostart;
    private boolean notificationsEnabled;
    private ResourceBundle resourceBundle;
    private boolean remoteLogs;
    private File resDir;
    private Database database;
    private Cache cache;
    private Profile profile;
    private BrokerProperties brokerProps;
    private boolean extendedMode;
    private boolean daemonMode;

    private Config() {
        // Note: Do NOT add a logger here, as the logger needs the Config instance.        
        configDir = null;
        configFile = null;
        logApiRestUrl = null;
        userName = null;
        queueName = null;
        extendedMode = false;
        daemonMode = false;

        Locale locale = new Locale("en", "US");
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale.getLanguage().toLowerCase().compareTo("es") == 0) {
            locale = new Locale("es", "ES");
        } else if (defaultLocale.getLanguage().toLowerCase().compareTo("fr") == 0) {
            locale = new Locale("fr", "FR");
        } else if (defaultLocale.getLanguage().toLowerCase().compareTo("ca") == 0) {
            locale = new Locale("ca", "ES");
        }

        resourceBundle = ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE, locale);

        /*
         * WARNING: Do NOT add 'Config' as a static final if the class  
         *          is created in the Config constructor, Config.getInstance()
         *          will return NULL. 	
         */
        device = new Device();
        brokerProps = new BrokerProperties();
        database = new Database();
        cache = new Cache();

        encryption = getEncryption();
    }

    public synchronized static Config getInstance() {
        /*
         * WARNING: Do NOT add 'Config' as a static final if the class  
         *          is created in the Config constructor, Config.getInstance()
         *          will return NULL. 	
         */
        if (instance == null) {
            throw new RuntimeException("WARNING: Config instance cannot be null. Fix the dependencies.");
        }

        return instance;
    }

    private Encryption getEncryption() {
        Encryption encrypt = null;
        try {
            encrypt = new Encryption();
            encrypt.setCipherStr("aes");
            encrypt.setKeylength(128);
            encrypt.setPassword("stacksync");
            encrypt.init();

        } catch (ConfigException ex) {
            logger.error("Error creating config file encrypter.", ex);
        }

        return encrypt;
    }

    public String getUserName() {
        return (userName != null) ? userName : env.getUserName();
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDeviceName() {
        return (device != null) ? device.getName() : env.getDeviceName();
    }

    public void setDeviceName(String deviceName) {
        this.device.setName(deviceName.replace("-", "_"));
    }
    
    public Long getDeviceId() {
        return this.device.getId();
    }
        
    public void setDeviceId(Long id) {
        this.device.setId(id);
    }
    
    public String getQueueName() {
        return this.queueName;
    }
    
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
    
    public boolean isAutostart() {

        return autostart;
    }

    public String getLogApiRestUrl() {
        return logApiRestUrl;
    }

    public void setAutostart(boolean autostart) {
        this.autostart = autostart;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public File getConfDir() {
        if (configDir == null) {
            throw new RuntimeException("configDir is null. This cannot be!");
        }

        return configDir;
    }

    public void setConfigDir(File configDir) {
        this.configDir = configDir;
    }

    public File getResDir() {
        return (resDir != null) ? resDir : env.getAppResDir();
    }

    public File getResImage(String imageFilename) {
        return new File(getResDir().getAbsoluteFile() + File.separator + imageFilename);
    }

    public void setResDir(File resDir) {
        this.resDir = resDir;
    }

    public Database getDatabase() {
        return database;
    }

    public Cache getCache() {
        return cache;
    }
    
    public Profile getProfile(){
        return profile;
    }
    
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public boolean isExtendedMode() {
        return extendedMode;
    }

    public void setExtendedMode(boolean extendedMode) {
        this.extendedMode = extendedMode;
    }
    
    public boolean isDaemonMode() {
        return daemonMode;
    }

    public void setDaemonMode(boolean daemonMode) {
        this.daemonMode = daemonMode;
    }

    public boolean isRemoteLogs() {
        return remoteLogs;
    }

    public void setRemoteLogs(boolean remoteLogs) {
        this.remoteLogs = remoteLogs;
    }

    public void load() throws ConfigException {
        load(env.getDefaultUserConfigDir());
    }

    public void load(File configFolder) throws ConfigException {
        logger.info("Loading configuration from " + configFolder);

        configDir = configFolder;
        configFile = new File(configDir.getAbsoluteFile() + File.separator + Constants.CONFIG_FILENAME);

        // Default config dir
        if (!configDir.equals(env.getDefaultUserConfigDir())) {
            // Create if it does not exist
            createDirectory(configDir);
        } // Not default config folder: Must exist!
        else if (!configDir.equals(env.getDefaultUserConfigDir())) {
            if (!configDir.exists()) {
                throw new ConfigException("Config folder " + configDir + " does not exist!");
            }
        }

        createDirectory(new File(configDir + File.separator + Constants.CONFIG_DATABASE_DIRNAME));
        createDirectory(new File(configDir + File.separator + Constants.PROFILE_IMAGE_DIRNAME));

        // Config file: copy from res-dir, if non-existant
        if (!configFile.exists()) {
            InputStream is = null;

            try {
                is = Environment.class.getResourceAsStream(Constants.CONFIG_DEFAULT_FILENAME);

                FileUtil.writeFile(is, configFile);
                //byte[] packed = encryptConfigFile(IOUtils.toByteArray(is));
                //FileUtil.writeFile(packed, configFile);
            } catch (IOException e) {
                throw new ConfigException("Could not copy default config file from " + Constants.CONFIG_DEFAULT_FILENAME + " to " + configFile, e);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException ex) {
                    logger.error("Exception: ", ex);
                }
            }
        }

        InputStream stream = null;

        try {

            //byte[] byteStream = decrytpConfigFile();
            //stream = new ByteArrayInputStream(byteStream);

            stream = new FileInputStream(configFile);
            load(stream);
        } catch (FileNotFoundException ex) {
            logger.error(ex);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ex) {
                logger.error("Exception: ", ex);
            }
        }
    }

    public synchronized void load(InputStream configStream) throws ConfigException {

        // Parse and load!
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            doc = dBuilder.parse(configStream);
            self = new ConfigNode(doc.getDocumentElement());

            loadDOM(self);

        } catch (Exception e) {
            throw new ConfigException(e);
        }

    }

    /**
     * Saves the configuration to the file it was loaded from, or the default
     * config file if it has not been loaded at all.
     *
     * <p>Note: This does not save the config to the default config file. To do
     * that, call
     * <code>save(env.getDefaultConfigFile());</code>
     *
     * @throws ConfigException
     */
    public void save() throws ConfigException {
        FileOutputStream out = null;
        OutputStreamWriter outputStream = null;

        saveDOM(self);

        // Save file
        doc.getDocumentElement().normalize();

        try {
            // This prints a correct formated XML file, but functions are
            // deprecated.
            /*OutputFormat format = new OutputFormat(doc);
            format.setLineWidth(100);
            format.setIndenting(true);
            format.setIndent(4);
            Writer writer = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(writer, format);
            serializer.serialize(doc);
            
            FileUtil.writeFile(writer.toString(), configFile);*/

            out = new FileOutputStream(configFile);
            DOMSource ds = new DOMSource(doc);
            outputStream = new OutputStreamWriter(out, "utf-8");
            StreamResult sr = new StreamResult(outputStream);

            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute("indent-number", 4);

            Transformer trans = tf.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(ds, sr);

            //byte[] packed = encryptConfigFile(outputStream.toByteArray());
            //FileUtil.writeFile(packed, configFile);

            outputStream.close();

        } catch (Exception e) {
            throw new ConfigException(e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ex) {
                logger.error("I/O Exception.", ex);
            }
        }
    }

    private byte[] decrytpConfigFile() throws ConfigException {
        byte[] packed = null;
        try {
            byte[] fileData = new byte[(int) configFile.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(configFile));
            dis.readFully(fileData);
            dis.close();

            packed = FileUtil.unpack(fileData, this.encryption);
        } catch (Exception e) {
            logger.error(e);
            throw new ConfigException(e);
        }

        return packed;
    }

    private byte[] encryptConfigFile(byte[] fileData) throws ConfigException {
        byte[] packed = null;
        try {
            packed = FileUtil.pack(fileData, this.encryption);
        } catch (Exception e) {
            logger.error(e);
            throw new ConfigException(e);
        }

        return packed;
    }

    public void createDirectory(File directory) throws ConfigException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new ConfigException("Directory '" + directory + "' does not exist and could not be created.");
        } else if (!directory.isDirectory() || !directory.canRead() || !directory.canWrite()) {
            throw new ConfigException("Path '" + directory + "' is not a directory or is not read/writable.");
        }
    }

    public BrokerProperties getBrokerProps() {
        return brokerProps;
    }

    private void loadDOM(ConfigNode node) throws ConfigException {
        // Flat values
        userName = node.getProperty("username", env.getUserName());
        queueName = node.getProperty("queuename", env.getDeviceNameWithTimestamp());
        autostart = node.getBoolean("autostart", Constants.DEFAULT_AUTOSTART_ENABLED);
        notificationsEnabled = node.getBoolean("notifications", Constants.DEFAULT_NOTIFICATIONS_ENABLED);

        logApiRestUrl = node.getProperty("apiLogUrl", "URL_LOG_SERVER_API");
        remoteLogs = node.getBoolean("remoteLogs", false);

        if (userName.isEmpty()) {
            userName = env.getUserName();
        }

        // Resource bundle
        String language = node.getProperty("language", null);

        if (language != null) {
            try {

                String[] languageSplit = language.split("_");
                if (languageSplit.length == 2) {
                    resourceBundle = ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE, new Locale(languageSplit[0].toLowerCase(), languageSplit[1].toUpperCase()));
                }
            } catch (MissingResourceException e) {
                logger.warn("COULD NOT LOAD resource bundle for " + language, e);
                /* Use default; Loaded in constructor */
            }
        }

        // Directories	
        resDir = node.getFile("resdir", env.getAppResDir());

        // Tests
        if (!resDir.exists() || !resDir.isDirectory() || !resDir.canRead()) {
            throw new ConfigException("Cannot read resource directory '" + resDir + "'.");
        }

        // Complex subvalues
        device.load(node.findChildByName("device"));
        brokerProps.load(node.findChildByName("rabbitMQ"));
        database.load(node.findChildByName("database"));
        cache.load(node.findChildByName("cache"));
        profile = new Profile();
        profile.load(node.findChildByName("profile"));
    }

    private void saveDOM(ConfigNode node) {
        // Flat values
        node.setProperty("username", userName);
        node.setProperty("queuename", queueName);
        node.setProperty("autostart", autostart);
        node.setProperty("notifications", notificationsEnabled);
        node.setProperty("apiLogUrl", logApiRestUrl);
        node.setProperty("remoteLogs", remoteLogs);

        // Complex
        // DO NOT SAVE "database"
        device.save(node.findOrCreateChildByXpath("device", "device"));
        brokerProps.save(node.findOrCreateChildByXpath("rabbitMQ", "rabbitMQ"));
        cache.save(node.findOrCreateChildByXpath("cache", "cache"));
        profile.save(node.findOrCreateChildByXpath("profile", "profile"));
    }
}
