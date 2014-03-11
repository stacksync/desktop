package com.stacksync.desktop.syncserver;

import com.stacksync.commons.exceptions.DeviceNotUpdatedException;
import com.stacksync.commons.exceptions.DeviceNotValidException;
import com.stacksync.commons.exceptions.NoWorkspacesFoundException;
import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.exceptions.WorkspaceNotUpdatedException;
import com.stacksync.commons.models.AccountInfo;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.Workspace;
import com.stacksync.commons.omq.ISyncService;
import com.stacksync.commons.requests.CommitRequest;
import com.stacksync.commons.requests.GetAccountRequest;
import com.stacksync.commons.requests.GetChangesRequest;
import com.stacksync.commons.requests.GetWorkspacesRequest;
import com.stacksync.commons.requests.ShareProposalRequest;
import com.stacksync.commons.requests.UpdateDeviceRequest;
import com.stacksync.commons.requests.UpdateWorkspaceRequest;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.repository.Update;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import omq.common.broker.Broker;
import org.apache.log4j.Logger;

public class Server {

    private final Logger logger = Logger.getLogger(Server.class.getName());
    private final Config config = Config.getInstance();
    private ISyncService syncServer;
    private Broker broker;

    public ISyncService getSyncServer() {
        return syncServer;
    }

    public Server(Broker broker) throws Exception {
        this.broker = broker;
        this.syncServer = this.broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);
    }

    public String getRequestId() {
        return config.getDeviceName() + "-" + (new Date()).getTime();
    }

    public List<Update> getChanges(String accountId, CloneWorkspace workspace) {
        List<Update> updates = new ArrayList<Update>();

        GetChangesRequest request = new GetChangesRequest(UUID.fromString(accountId), UUID.fromString(workspace.getId()));

        List<ItemMetadata> items = syncServer.getChanges(request);
        for (ItemMetadata item : items) {
            Update update = Update.parse(item, workspace);
            updates.add(update);
        }

        return updates;
    }

    public List<CloneWorkspace> getWorkspaces(String accountId) throws NoWorkspacesFoundException {
        List<CloneWorkspace> workspaces = new ArrayList<CloneWorkspace>();

        GetWorkspacesRequest request = new GetWorkspacesRequest(UUID.fromString(accountId));
        List<Workspace> remoteWorkspaces = syncServer.getWorkspaces(request);

        for (Workspace rWorkspace : remoteWorkspaces) {
            CloneWorkspace workspace = new CloneWorkspace(rWorkspace);
            workspaces.add(workspace);
        }

        return workspaces;
    }
    
    public void updateDevice(String accountId) {
        
        UUID deviceId;
        
        Environment env = Environment.getInstance();
        String osInfo = env.getOperatingSystem().toString() + "-" + env.getArchitecture();
        
        UpdateDeviceRequest request = new UpdateDeviceRequest(UUID.fromString(accountId), config.getDeviceId(),
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

        CommitRequest request = new CommitRequest(UUID.fromString(accountId), UUID.fromString(workspace.getId()),
                config.getDeviceId(), commitItems);
        
        request.setRequestId(getRequestId());
        syncServer.commit(request);
        logger.info(" [x] Sent '" + commitItems + "'");
    }
    
    public void createShareProposal(String accountId, List<String> emails, String folderName)
            throws ShareProposalNotCreatedException, UserNotFoundException {
        
        logger.info("Sending share proposal.");
        
        ShareProposalRequest request = new ShareProposalRequest(UUID.fromString(accountId), emails, folderName);
        request.setRequestId(getRequestId());
        syncServer.createShareProposal(request);
    }
    
    public void updateWorkspace(String accountId, String workspaceId, String workspaceName, Long parent) {
        
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest(UUID.fromString(accountId), 
                UUID.fromString(workspaceId), workspaceName, parent);
        request.setRequestId(getRequestId());
        try {
            syncServer.updateWorkspace(request);
        } catch (UserNotFoundException ex) {
            logger.error("Cannot update workspace due to: ", ex);
        } catch (WorkspaceNotUpdatedException ex) {
            logger.error("Cannot update workspace due to: ", ex);
        }
    }
    
    public AccountInfo getAccountInfo(String email) {
        AccountInfo info;
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
