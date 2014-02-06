package com.stacksync.desktop.syncserver;

import com.stacksync.commons.notifications.ShareProposalNotification;
import com.stacksync.commons.omq.RemoteClient;
import omq.server.RemoteObject;

public class RemoteClientImpl extends RemoteObject implements RemoteClient {

    @Override
    public void notifyShareProposal(ShareProposalNotification spn) {
        System.out.println(spn.toString());
    }

}
