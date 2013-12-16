/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.syncserver;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.Workspace;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.util.StringUtil;
import com.stacksync.desktop.watch.remote.ChangeManager;
import com.stacksync.syncservice.models.CommitResult;
import com.stacksync.syncservice.omq.RemoteWorkspace;
import com.stacksync.syncservice.models.ObjectMetadata;
import com.stacksync.syncservice.models.CommitInfo;
import java.util.ArrayList;
import java.util.List;
import omq.server.RemoteObject;
import org.apache.log4j.Logger;

/**
 *
 * @author sergi
 */
public class RemoteWorkspaceImpl extends RemoteObject implements RemoteWorkspace {

    private final Logger logger = Logger.getLogger(RemoteWorkspaceImpl.class.getName());
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    private final Config config = Config.getInstance();
    private Workspace workspace;
    private Profile profile;
    private ChangeManager changeManager;

    public RemoteWorkspaceImpl(Profile profile, Workspace workspace, ChangeManager changeManager) {
        this.workspace = workspace;
        this.profile = profile;
        this.changeManager = changeManager;
    }

    private void markAsUpdated(CloneFile cf, Update update) {
        cf.setServerUploadedAck(true);
        cf.setUpdated(update.getUpdated());
        cf.merge();
    }

    @Override
    public void notifyCommit(CommitResult cr) {
        List<CommitInfo> listObjects = cr.getObjects();
        logger.info(" [x] Received in queue(" + workspace.getId() + ") '" + listObjects + "'");

        String requestIdStr = cr.getRequestID();
        String[] requestId = requestIdStr.split("-");
        List<Update> ul = new ArrayList<Update>();

        for (CommitInfo obj : listObjects) {
            boolean committed = obj.isCommitted();
            long version = obj.getVersion();
            long fileId = obj.getFileId();

            try {
                ObjectMetadata objMetadata = obj.getMetadata();
                if (committed) {
                    Update update = StringUtil.parseUpdate(objMetadata, workspace);
                    CloneFile existingVersion = db.getFileOrFolder(profile, fileId, version);
                    if (config.getMachineName().compareTo(requestId[0]) == 0) { //same pc ack
                        if (existingVersion != null) {
                            markAsUpdated(existingVersion, update);
                        } else {
                            logger.info("Exception: existing version is null");
                        }
                    } else { //other client
                        ul.add(update);
                    }
                } else {
                    if (config.getMachineName().compareTo(requestId[0]) == 0) { //same pc ack
                        Update update = StringUtil.parseUpdate(objMetadata, workspace);
                        CloneFile existingVersion = db.getFileOrFolder(profile, update.getFileId(), update.getVersion());
                        if (existingVersion == null) {
                            update.setConflicted(true);
                            ul.add(update);
                        } else {
                            markAsUpdated(existingVersion, update);
                        }
                    }
                }

            } catch (NullPointerException ex) {
                logger.info("Error parsing: " + obj, ex);
            }
        }

        logger.info("Queuing updates(" + ul.size() + ")");
        if (ul.size() > 0) {
            changeManager.queueUpdates(ul);
        }
    }
}
