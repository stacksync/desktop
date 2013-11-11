/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.syncserver;

/**
 *
 * @author gguerrero
 */
import com.stacksync.syncservice.models.ObjectMetadata;
import com.stacksync.syncservice.omq.ISyncService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.stacksync.desktop.db.models.Workspace;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.util.StringUtil;
import com.stacksync.desktop.config.Config;
import com.stacksync.syncservice.models.WorkspaceInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;

import omq.common.broker.Broker;
import omq.common.util.Serializers.JavaImp;
import org.apache.commons.io.IOUtils;

public class Server {

    private final Logger logger = Logger.getLogger(Server.class.getName());
    private final Config config = Config.getInstance();
    private ISyncService syncServer;
    private Broker broker;
    private Map<String, WorkspaceInfo> rWorkspaces;
    private Properties env;

    public ISyncService getSyncServer() {
        return syncServer;
    }

    public Server(Broker broker) throws Exception {
        //env.setProperty(ParameterQueue.DEBUGFILE, "c:\\middlewareDebug");

        this.broker = broker;
        this.syncServer = this.broker.lookup(ISyncService.class.getSimpleName(), ISyncService.class);
        this.rWorkspaces = new HashMap<String, WorkspaceInfo>();
    }

    public String getRequestId() {
        return config.getMachineName() + "-" + (new Date()).getTime();
    }

    public List<Update> getChanges(String cloudId, Workspace workspace) {
        List<Update> updates = new ArrayList<Update>();

        String requestId = getRequestId();
        WorkspaceInfo rWorkspace = rWorkspaces.get(workspace.getId());

        List<ObjectMetadata> objects = syncServer.getChanges(cloudId, requestId, rWorkspace);
        for (ObjectMetadata obj : objects) {
            Update update = StringUtil.parseUpdate(obj, workspace);
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

    public void commit(String cloudId, Workspace workspace, List<ObjectMetadata> commitObjects) throws IOException {
        String requestId = getRequestId();
        String device = config.getMachineName();
        WorkspaceInfo rWorkspace = rWorkspaces.get(workspace.getId());

        syncServer.commit(cloudId, requestId, rWorkspace, device, commitObjects);
//        saveLog(commitObjects);
        logger.info(config.getMachineName() + "# [x] Sent '" + commitObjects + "'");
    }
    
    public void commit(String cloudId, String requestId, Workspace workspace, List<ObjectMetadata> commitObjects) throws IOException {
        String device = config.getMachineName();
        WorkspaceInfo rWorkspace = rWorkspaces.get(workspace.getId());

        syncServer.commit(cloudId, requestId, rWorkspace, device, commitObjects);
        saveLog(commitObjects);
        logger.info(config.getMachineName() + "# [x] Sent '" + commitObjects + "'");
    }

    private void saveLog(List<ObjectMetadata> commitObjects) {
        String debugPath = "debug_file.txt";
        if (debugPath.length() > 0) {
            try {
                long timeNow = (new Date()).getTime();

                File outputFolder = new File(debugPath + File.separator + "Client");
                outputFolder.mkdirs();

                JavaImp serializer = new JavaImp();
                byte[] bytes = serializer.serialize(commitObjects);

                File outputFileContent = new File(outputFolder.getAbsoluteFile() + File.separator + "client-files-" + timeNow);
                FileOutputStream outputStream = new FileOutputStream(outputFileContent);
                IOUtils.write(bytes, outputStream);
                outputStream.close();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
