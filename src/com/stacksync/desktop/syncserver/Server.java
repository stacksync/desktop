package com.stacksync.desktop.syncserver;

import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.AccountInfo;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.notifications.ShareProposalNotification;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.commons.requests.CommitRequest;
import com.stacksync.commons.requests.GetAccountRequest;
import com.stacksync.commons.requests.GetChangesRequest;
import com.stacksync.commons.requests.GetWorkspacesRequest;
import com.stacksync.commons.requests.ShareProposalRequest;
import com.stacksync.commons.requests.UpdateDeviceRequest;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.sharing.SharingController;
import com.stacksync.desktop.util.StringUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import omq.common.broker.Broker;
import org.apache.log4j.Logger;

public class Server {

    private final Logger logger = Logger.getLogger(Server.class.getName());
    private final Config config = Config.getInstance();
    private final SharingController sharingController = SharingController.getInstance();
    private ISyncService syncServer;
    private Broker broker;
    private Map<String, Workspace> rWorkspaces;
    private int i;

    public ISyncService getSyncServer() {
        return syncServer;
    }

    public Server(Broker broker) throws Exception {
        this.broker = broker;
        this.syncServer = this.broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);
        this.rWorkspaces = new HashMap<String, Workspace>();
        this.i = 0;
    }

    public String getRequestId() {
        return config.getDeviceName() + "-" + (new Date()).getTime();
    }

    public List<Update> getChanges(String accountId, CloneWorkspace workspace) {
        List<Update> updates = new ArrayList<Update>();

        GetChangesRequest request = new GetChangesRequest(accountId, workspace.getId());

        List<ItemMetadata> items = syncServer.getChanges(request);
        for (ItemMetadata item : items) {
            Update update = Update.parse(item, workspace);
            updates.add(update);
        }

        return updates;
    }

    public List<CloneWorkspace> getWorkspaces(String accountId) throws NoWorkspacesFoundException {
        List<CloneWorkspace> workspaces = new ArrayList<CloneWorkspace>();

        GetWorkspacesRequest request = new GetWorkspacesRequest(accountId);
        List<Workspace> remoteWorkspaces = syncServer.getWorkspaces(request);

        for (Workspace rWorkspace : remoteWorkspaces) {
            CloneWorkspace workspace = new CloneWorkspace(rWorkspace);
            workspaces.add(workspace);
            rWorkspaces.put(workspace.getId(), rWorkspace);
        }

        return workspaces;
    }
    
    public void updateDevice(String accountId) {
        
        UUID deviceId;
        
        Environment env = Environment.getInstance();
        String osInfo = env.getOperatingSystem().toString() + "-" + env.getArchitecture();
        
        UpdateDeviceRequest request = new UpdateDeviceRequest(accountId, config.getDeviceId(),
                config.getDeviceName(), osInfo, null, null);
        try {
            
            deviceId = syncServer.updateDevice(request);
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

    public void commit(String accountId, CloneWorkspace workspace, List<ItemMetadata> commitItems) throws IOException {

        CommitRequest request = new CommitRequest(accountId, workspace.getId(), config.getDeviceId(), commitItems);
        request.setRequestId(getRequestId());
        syncServer.commit(request);
        logger.info(" [x] Sent '" + commitItems + "'");
    }
    
    public void createShareProposal(String accountId, List<String> emails, String folderName) {
        
        logger.info("Sending share proposal.");
        
        String container = StringUtil.generateRandomString();
        String storageURL = "http://10.30.236.175:8080/v1/"+accountId;
        
        // TODO quitar storageURL y container de aqui
        ShareProposalRequest request = new ShareProposalRequest(accountId, emails, folderName, container, storageURL);
        ShareProposalNotification response;
        
        try {
            response = syncServer.createShareProposal(request);
        } catch (Exception e) {
            // Show error and return
            logger.error("New workspace not created: "+e);
            return;
        }
        
        Workspace newWorkspace = new Workspace(response.getWorkspaceId());
        newWorkspace.setSwiftContainer(container);
        newWorkspace.setSwiftURL(storageURL);
        newWorkspace.setName(folderName);
        
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
    
    public AccountInfo getAccountInfo(String email) {
        AccountInfo info = null;
        try {
            GetAccountRequest request = new GetAccountRequest(email);
            info = syncServer.getAccountInfo(request);
            return info;
        } catch (UserNotFoundException ex) {
            logger.error("User not found: "+ex);
            return null;
        }
        
    }
}
