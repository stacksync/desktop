/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.syncserver;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.Workspace;
import com.stacksync.desktop.dummy.Stacksync_dummy;
import com.stacksync.desktop.watch.remote.ChangeManager;
import com.stacksync.syncservice.models.CommitResult;
import com.stacksync.syncservice.omq.RemoteWorkspace;
import com.stacksync.syncservice.models.CommitInfo;
import java.io.IOException;
import java.util.List;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

public class RemoteWorkspaceDummy extends RemoteObject implements RemoteWorkspace {

    private final Logger logger = Logger.getLogger(RemoteWorkspaceDummy.class.getName());
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    private final Config config = Config.getInstance();
    private Workspace workspace;

    public RemoteWorkspaceDummy(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public void notifyCommit(CommitResult cr) {
        List<CommitInfo> listObjects = cr.getObjects();
        logger.info(config.getMachineName() + "# [x] Received in queue(" + workspace.getId() + ") '" + listObjects + "'");

        try {
            Stacksync_dummy.saveTimeSendRequestLog("Client-time-commit", cr.getRequestID(), "commit-end");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
