package com.stacksync.desktop.test.distributed;

import com.sshtools.j2ssh.transport.ConsoleKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey; 

public class AlwaysAllowingConsoleKnownHostsKeyVerification extends
        ConsoleKnownHostsKeyVerification { 

    public AlwaysAllowingConsoleKnownHostsKeyVerification()
            throws InvalidHostFileException {
        super();
        // Don't not do anything else
    } 

    @Override
    public void onHostKeyMismatch(String s, SshPublicKey sshpublickey,
            SshPublicKey sshpublickey1) {
        try
        {
                sshpublickey1.getFingerprint();
                sshpublickey.getFingerprint();
            allowHost(s, sshpublickey, false);
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
    } 

    @Override
    public void onUnknownHost(String s, SshPublicKey sshpublickey) {
        try
        {
                sshpublickey.getFingerprint();
            allowHost(s, sshpublickey, false);
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
        }
    } 

}