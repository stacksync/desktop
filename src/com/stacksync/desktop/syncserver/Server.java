/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.syncserver;

/**
 *
 * @author gguerrero
 */
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.models.Workspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.repository.Update;
import com.stacksync.syncservice.models.DeviceInfo;
import com.stacksync.syncservice.models.ItemMetadata;
import com.stacksync.syncservice.models.WorkspaceInfo;
import com.stacksync.syncservice.omq.ISyncService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import omq.common.broker.Broker;
import omq.common.util.Serializers.JavaImp;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class Server {

    private final Logger logger = Logger.getLogger(Server.class.getName());
    private final Config config = Config.getInstance();
    private ISyncService syncServer;
    private Broker broker;
    private Map<String, WorkspaceInfo> rWorkspaces;
    private int i;

    public ISyncService getSyncServer() {
        return syncServer;
    }

    public Server(Broker broker) throws Exception {
        //env.setProperty(ParameterQueue.DEBUGFILE, "c:\\middlewareDebug");

        this.broker = broker;
        this.syncServer = this.broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);
        this.rWorkspaces = new HashMap<String, WorkspaceInfo>();
        this.i = 0;
    }

    public String getRequestId() {
        return config.getDeviceName() + "-" + (new Date()).getTime();
    }

    public List<Update> getChanges(String cloudId, Workspace workspace) {
        List<Update> updates = new ArrayList<Update>();

        String requestId = getRequestId();
        WorkspaceInfo rWorkspace = rWorkspaces.get(workspace.getId());

        List<ItemMetadata> items = syncServer.getChanges(cloudId, requestId, rWorkspace);
        for (ItemMetadata item : items) {
            Update update = Update.parse(item, workspace);
            updates.add(update);
        }

        return updates;
    }

    public List<Workspace> getWorkspaces(String cloudId) throws IOException {
        String requestId = getRequestId();
        List<Workspace> workspaces = new ArrayList<Workspace>();

        List<WorkspaceInfo> remoteWorkspaces = syncServer.getWorkspaces(cloudId, requestId);
        if (remoteWorkspaces.isEmpty()) {
            throw new IOException("get_workspaces hasn't workspaces");
        }

        for (WorkspaceInfo rWorkspace : remoteWorkspaces) {
            Workspace workspace = new Workspace(rWorkspace);
            workspaces.add(workspace);
            rWorkspaces.put(workspace.getId(), rWorkspace);
        }

        return workspaces;
    }
    
    public void updateDevice(String cloudId) {
        
        String requestId = getRequestId();
        long deviceId;
        
        Environment env = Environment.getInstance();
        String osInfo = env.getOperatingSystem().toString() + "-" + env.getArchitecture();
        
        DeviceInfo device = new DeviceInfo(config.getDeviceId(), config.getDeviceName(),
                osInfo, null, null);
        deviceId = syncServer.updateDevice(cloudId, requestId, device);
        logger.debug("Obtained deviceId: "+deviceId);
        
        if (deviceId != -1) {
            try {
                // Set registerId in config
                config.setDeviceId(deviceId);
                config.save();
                // Something else?

                logger.info("Device registered");
            } catch (ConfigException ex) {
                
            }
        } else {
            // What to do here??
            logger.error("Device not registered!!");
        }
        
    }

    public void commit(String cloudId, Workspace workspace, List<ItemMetadata> commitItems) throws IOException {
        String requestId = getRequestId();
        Long device = config.getDeviceId();
        WorkspaceInfo rWorkspace = rWorkspaces.get(workspace.getId());

        syncServer.commit(cloudId, requestId, rWorkspace, device, commitItems);
//        saveLog(commitObjects);
        logger.info(" [x] Sent '" + commitItems + "'");
    }
    
    public void commit(String cloudId, String requestId, Workspace workspace, List<ItemMetadata> commitItems) throws IOException {
        Long device = config.getDeviceId();
        WorkspaceInfo rWorkspace = rWorkspaces.get(workspace.getId());

        syncServer.commit(cloudId, requestId, rWorkspace, device, commitItems);
        saveLog(commitItems);
        logger.info(" [x] Sent '" + commitItems + "'");
    }

    private void saveLog(List<ItemMetadata> commitItems) {
        String debugPath = "test";
        if (debugPath.length() > 0) {
            try {

                File outputFolder = new File(debugPath + File.separator + "Client");
                outputFolder.mkdirs();

                JavaImp serializer = new JavaImp();
                byte[] bytes = serializer.serialize(commitItems);

                File outputFileContent = new File(outputFolder.getAbsoluteFile() + File.separator + "client-files-" + i);
                FileOutputStream outputStream = new FileOutputStream(outputFileContent);
                IOUtils.write(bytes, outputStream);
                outputStream.close();
                this.i++;
            } catch (Exception ex) {
                //java.util.logging.Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
