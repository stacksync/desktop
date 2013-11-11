/*
 * Syncany, www.syncany.org
 * Copyright (C) 2012 Maurus Cuelenaere<mcuelenaere@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.connection.plugins.dropbox;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 * @author Maurus Cuelenaere <mcuelenaere@gmail.com>
 */
public class DropboxConnection implements Connection {
    private static final AppKeyPair APP_KEY = new AppKeyPair("<FILLME>", "<FILLME>");
    private static final Logger logger = Logger.getLogger(DropboxConnection.class.getSimpleName());

    private WebAuthSession authSession;

    public DropboxConnection() {
        authSession = new WebAuthSession(APP_KEY, Session.AccessType.APP_FOLDER);
    }

    public WebAuthSession getAuthSession() {
        return authSession;
    }

    public boolean isAuthenticated() {
        return authSession.getAccessTokenPair() != null;
    }

    @Override
    public String getUsername() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getHost() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public class DropboxAuthentication {
        private final WebAuthInfo authInfo;

        public DropboxAuthentication() throws DropboxException {
            authInfo = authSession.getAuthInfo();
        }

        public String getUrl() {
            return authInfo.url;
        }

        public boolean complete() {
            try {
                authSession.retrieveWebAccessToken(authInfo.requestTokenPair);
                return true;
            } catch (DropboxException ex) {
                logger.log(Level.WARNING, "Couldn't complete Dropbox authentication", ex);
                return false;
            }
        }
    }

    public DropboxAuthentication authenticate() {
        try {
            return new DropboxAuthentication();
        } catch (DropboxException ex) {
            logger.log(Level.WARNING, "Couldn't start Dropbox authentication", ex);
            return null;
        }
    }

    @Override
    public TransferManager createTransferManager() {
        return new DropboxTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        return new DropboxConfigPanel(this);
    }

    @Override
    public PluginInfo getPluginInfo() {
        return new DropboxPluginInfo();
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        String key = node.getProperty("accessKey");
        String secret = node.getProperty("accessSecret");

        if (key == null || secret == null) {
            throw new ConfigException("Dropbox connection properties must at least contain the parameters 'accessKey' and 'accessSecret'.");
        }

        authSession.setAccessTokenPair(new AccessTokenPair(key, secret));
    }

    @Override
    public void save(ConfigNode node) {
        AccessTokenPair tokenPair = authSession.getAccessTokenPair();

        node.setAttribute("type", getPluginInfo().getId());
        node.setProperty("accessKey", tokenPair.key);
        node.setProperty("accessSecret", tokenPair.secret);
    }
}