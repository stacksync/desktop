package com.stacksync.desktop.syncserver;

import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.commons.requests.GetWorkspacesRequest;
import com.stacksync.commons.requests.ShareProposalRequest;
import com.stacksync.commons.requests.UpdateDeviceRequest;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.sharing.SharingController;
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
    private final SharingController sharingController = SharingController.getInstance();
    private ISyncService syncServer;
    private Broker broker;
    private Map<Long, Workspace> rWorkspaces;
    private int i;

    public ISyncService getSyncServer() {
        return syncServer;
    }

    public Server(Broker broker) throws Exception {
        //env.setProperty(ParameterQueue.DEBUGFILE, "c:\\middlewareDebug");

        this.broker = broker;
        this.syncServer = this.broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);
        this.rWorkspaces = new HashMap<Long, Workspace>();
        this.i = 0;
    }

    public String getRequestId() {
        return config.getDeviceName() + "-" + (new Date()).getTime();
    }

    public List<Update> getChanges(String cloudId, CloneWorkspace workspace) {
        List<Update> updates = new ArrayList<Update>();

        String requestId = getRequestId();
        Workspace rWorkspace = rWorkspaces.get(workspace.getId());
        
        User user = new User();
        user.setCloudId(cloudId);

        List<ItemMetadata> items = syncServer.getChanges(requestId, user, rWorkspace);
        for (ItemMetadata item : items) {
            Update update = Update.parse(item, workspace);
            updates.add(update);
        }

        return updates;
    }

    public List<CloneWorkspace> getWorkspaces(String cloudId) throws NoWorkspacesFoundException {
        List<CloneWorkspace> workspaces = new ArrayList<CloneWorkspace>();

        GetWorkspacesRequest workspacesReq = new GetWorkspacesRequest(cloudId);
        
        List<Workspace> remoteWorkspaces = syncServer.getWorkspaces(workspacesReq);

        for (Workspace rWorkspace : remoteWorkspaces) {
            CloneWorkspace workspace = new CloneWorkspace(rWorkspace);
            workspaces.add(workspace);
            rWorkspaces.put(workspace.getId(), rWorkspace);
        }

        return workspaces;
    }
    
    public void updateDevice(String cloudId) {
        
        long deviceId;
        
        Environment env = Environment.getInstance();
        String osInfo = env.getOperatingSystem().toString() + "-" + env.getArchitecture();
        
        UpdateDeviceRequest deviceReq = new UpdateDeviceRequest(cloudId, config.getDeviceId(),
                config.getDeviceName(), osInfo, null, null);
        try {
            
            deviceId = syncServer.updateDevice(deviceReq);
            logger.debug("Obtained deviceId: "+deviceId);
            // Set registerId in config
            config.setDeviceId(deviceId);
            config.save();

            logger.info("Device registered");

        } catch (UserNotFoundException ex) {
            logger.error(ex);
        } catch (DeviceNotValidException ex) {
            logger.error(ex);
        } catch (DeviceNotUpdatedException ex) {
            logger.error(ex);
        } catch (ConfigException ex) {
            logger.error(ex);
        }
        
    }

    public void commit(String cloudId, CloneWorkspace workspace, List<ItemMetadata> commitItems) throws IOException {
        String requestId = getRequestId();
        Workspace rWorkspace = rWorkspaces.get(workspace.getId());

        User user = new User();
        user.setCloudId(cloudId);
        
        Device device = new Device(config.getDeviceId());
        
        syncServer.commit(requestId, user, rWorkspace, device, commitItems);
        logger.info(" [x] Sent '" + commitItems + "'");
    }
    
    public void createShareProposal(String cloudId, List<String> emails, String folderName) {
        
        Long newWorkspaceId = null;
        logger.info("Sending share proposal.");
        
        ShareProposalRequest shareRequest = new ShareProposalRequest(cloudId, emails, folderName);
        
        try {
            newWorkspaceId = syncServer.createShareProposal(shareRequest);
        } catch (Exception e) {
            // Show error and return
            logger.error("New workspace not created: "+e);
            return;
        }
        
        Workspace newWorkspace = new Workspace(newWorkspaceId);
        
        logger.info("Proposal accepted. New workspace: "+ newWorkspace.getId());
        rWorkspaces.put(newWorkspace.getId(), newWorkspace);
        
        // Save new workspace in DB
        CloneWorkspace cloneWorkspace = new CloneWorkspace(newWorkspace);
        cloneWorkspace.merge();
        
        try {
            config.getProfile().addNewWorkspace(cloneWorkspace);
            sharingController.createNewWorkspace(cloneWorkspace, folderName);
        } catch (Exception e) {
            logger.error("Error trying to listen new workspace: "+e);
        }
    }
    
    public void commit(String cloudId, String requestId, CloneWorkspace workspace, List<ItemMetadata> commitItems) throws IOException {
        Workspace rWorkspace = rWorkspaces.get(workspace.getId());

        User user = new User();
        user.setCloudId(cloudId);
        
        Device device = new Device(config.getDeviceId());
        
        syncServer.commit(requestId, user, rWorkspace, device, commitItems);
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
