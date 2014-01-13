/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.dummy;

import com.stacksync.syncservice.models.ObjectMetadata;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import omq.common.util.Serializers.JavaImp;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.BrokerProperties;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.db.models.Workspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.syncserver.RemoteWorkspaceDummy;
import com.stacksync.desktop.syncserver.Server;
import java.util.Arrays;
import omq.common.broker.Broker;

/**
 *
 * @author gguerrero
 */
public class Stacksync_dummy {

    private static final String DEBUGPATH = "C:\\middlewareDebug";
    private static final Config config = Config.getInstance();
    private static final CommandLineParser parser = new PosixParser();
    
    private Server server;
    private Broker broker;
    private Profile profile;
    private File path;
    private int commitsSecond;
    private String cloudId;
    private TransferManager trans;
    
    public Stacksync_dummy(File configFolder, File configFile, File path, int commitsSecond) throws Exception {

        config.load(configFolder);
        
        if (config.getProfile() == null) {
            throw new IOException("Could not load a profile, check the configuration file.");
        }
        
        this.profile = config.getProfile();
        this.trans = profile.getRepository().getConnection().createTransferManager();
        this.cloudId = trans.getUser();

        BrokerProperties brokerProps = config.getBrokerProps();
        brokerProps.setRPCReply(config.getDeviceName());
        
        this.broker = new Broker(brokerProps.getProperties());
        this.server = new Server(broker);
        
        this.path = path;
        this.commitsSecond = commitsSecond;
        
    }
    
    public void start() throws Exception {
        
        Workspace workspace = this.loadWorkspaces();
        List<File> files = Arrays.asList(this.path.listFiles());
        
        for(int i=0; i<this.path.listFiles().length; i+=this.commitsSecond){
            if(i>this.path.listFiles().length){
                break;                
            } else{
                //calculate time
                long timeStart = System.currentTimeMillis();
                for(int j=i; j<i+this.commitsSecond; j++){
                    File file = this.path.listFiles()[j];
                    this.doCommit(j, cloudId, workspace, file);
                    System.out.println("==============================");
                }
                long timeDiff = System.currentTimeMillis() - timeStart;
                long timeSleep = 1000 - timeDiff;
                
                if(timeSleep > 0){
                    System.out.println("Sleeping: " + timeSleep);
                    Thread.sleep(timeSleep);
                }
                //si mayor de segundo no dormir sino dormir diferencia
            }
        }
    }
    
    public Workspace loadWorkspaces() throws Exception {

        List<Workspace> remoteWorkspaces = server.getWorkspaces(cloudId);

        if(remoteWorkspaces.isEmpty()){
            throw new IOException();
        }
        
        for (Workspace w : remoteWorkspaces) {
            // From now on, there will exist a new RemoteWorkspaceImpl which will be listen to the changes that are done in the SyncServer
            broker.bind(w.getId(), new RemoteWorkspaceDummy(w));
        }
        
        return remoteWorkspaces.get(0);
    }
    
    private void doCommit(int j, String cloudId, Workspace workspace, File file) throws Exception{
        JavaImp serializer = new JavaImp();
        byte[] bytes = readFile(file);

        List<ObjectMetadata> objects = (List<ObjectMetadata>) serializer.deserializeObject(bytes);
        System.out.println("Sending(" + j + "/" + this.commitsSecond + ")!! -> " + file.getAbsolutePath() + " -- " + objects);

        String requestId = server.getRequestId();
        saveTimeSendRequestLog("Client-time-commit", requestId, "commit-start");
        server.commit(cloudId, requestId, workspace, objects);                
    }

    private static void showHelp(Options options) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp("java -jar stacksync_dumy.jar", options);

    }

    private static Options createOptions() {
        // create the Options
        Options options = new Options();

        options.addOption("p", "path", true, "Path for dummy files");
        options.addOption("c", "config", true, "Alternative path o config.xml file (Default: ~/.stacksync)");
        options.addOption("cs", "commit_second", true, "Commit in a second.");
        options.addOption("h", "help", false, "Print this message.");

        return options;
    }

    private static byte[] readFile(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        byte[] output = IOUtils.toByteArray(inputStream);

        inputStream.close();
        return output;
    }

    public static void saveTimeSendRequestLog(String processName, String requestId, String method) throws IOException{
        long timeNow = System.currentTimeMillis();

        File outputFolder = new File(DEBUGPATH + File.separator + processName);
        outputFolder.mkdirs();

        File outputFileLog = new File(outputFolder + File.separator + "log");
        boolean exist = outputFileLog.exists();

        FileWriter fw = new FileWriter(outputFileLog, true); // the true will append the new data
        if (!exist) {
            fw.write("#RequestId\tMethod\tDate\n");
        }
        fw.write(requestId + "\t" + method + "\t" + timeNow + "\n");
        fw.close();
    }

    /*
     * Run parameters: -p C:\middlewareDebug\Client -c c:\
     */
    public static void main(String[] args) throws ParseException, IOException, ConfigException, InitializationException, Exception {

        
        // create the command line parser        
        Options options = createOptions();
        CommandLine line = parser.parse(options, args);

        // Help
        if (line.hasOption("help") || !line.hasOption("path")) {
            showHelp(options);
            System.exit(0);
        }

        File path = new File(line.getOptionValue("path"));
        if (!path.isDirectory()) {
            System.out.println("Error: the path is not a folder.");
            System.exit(-1);
        }

        // Load config
        if (!line.hasOption("config")) {
            System.out.println("Error: the config is not setted.");
            System.exit(-2);
        }
        
        if(!line.hasOption("commit_second")){
            System.out.println("Error: must specifie commits/second.");
            System.exit(-3);  
        }

        File configFolder = new File(line.getOptionValue("config"));
        File configFile = new File(line.getOptionValue("config") + File.separator + "config.xml");
        int commitsSecond = Integer.parseInt(line.getOptionValue("commit_second"));
        
        if (!configFolder.exists()) {
            throw new ConfigException("config folder " + configFolder + " doesn't exist.");
        } else if (!configFile.exists()){
            throw new ConfigException(configFile + " doesn't exist.");
        }
        
        Stacksync_dummy app = new Stacksync_dummy(configFolder, configFile, path, commitsSecond);

        app.start();
        
    }
}
