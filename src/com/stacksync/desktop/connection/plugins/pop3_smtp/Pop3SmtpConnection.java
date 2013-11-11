/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package com.stacksync.desktop.connection.plugins.pop3_smtp;

import java.util.Properties;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Pop3SmtpConnection implements Connection {

    @Override
    public ConfigPanel createConfigPanel() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PluginInfo getPluginInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save(ConfigNode node) {
        throw new UnsupportedOperationException("Not supported yet.");
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
    
    public enum SmtpMode { NO_ENCRYPTION, SSL /* port 465 */, TLS_STARTTLS /* port 587 */};

    private String rcptAddress;

    private String pop3Host;
    private int pop3Port;
    private String pop3Username;
    private String pop3Password;
    private boolean pop3SslEnabled;

    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private SmtpMode smtpMode;

    public void init(Properties properties) throws ConfigException {
        // Mandatory
        rcptAddress = properties.getProperty("recipient");

        pop3Host = properties.getProperty("pop3host");
        pop3Username = properties.getProperty("pop3username");
        pop3Password = properties.getProperty("pop3password");

        smtpHost = properties.getProperty("smtphost");
        smtpUsername = properties.getProperty("smtpusername");
        smtpPassword = properties.getProperty("smtppassword");

        if (rcptAddress == null || pop3Host == null || pop3Username == null || pop3Password == null
             || smtpHost == null || smtpUsername == null || smtpPassword == null) {

            throw new ConfigException("POP3/SMTP connection properties must at least contain the parameters 'recipient', 'pop3host', 'pop3username', 'pop3password', 'smtphost', 'smtpusername' and 'smtppassword'");
        }

        // Optional

        // Pop3
        try { 
            pop3Port = Integer.parseInt(properties.getProperty("pop3port", "110")); 
        } catch (NumberFormatException e) { 
            throw new ConfigException("Invalid POP3 port number in config exception: "+properties.getProperty("pop3port")); }

        try { 
            pop3SslEnabled = Boolean.parseBoolean(properties.getProperty("pop3sslenabled", "false"));
        } catch (NumberFormatException e) { 
            throw new ConfigException("Invalid value for 'pop3sslenabled' in config exception: "+properties.getProperty("pop3sslenabled")); 
        }

        // Smtp
        String sSmtpMode = properties.getProperty("smtpmode");
        String sDefaultSmtpPort;

        if ("none".equals(sSmtpMode)) {
            smtpMode = SmtpMode.NO_ENCRYPTION;
            sDefaultSmtpPort = "25";
        } else if ("ssl".equals(sSmtpMode)) {
            smtpMode = SmtpMode.SSL;
            sDefaultSmtpPort = "465";
        } else if ("tls-starttls".equals(sSmtpMode)) {
            smtpMode = SmtpMode.TLS_STARTTLS;
            sDefaultSmtpPort = "587";
        } else {
            throw new ConfigException("Invalid value for 'smtpmode': "+sSmtpMode);
        }

        try { 
            smtpPort = Integer.parseInt(properties.getProperty("smtpport", sDefaultSmtpPort)); 
        } catch (NumberFormatException e) { 
            throw new ConfigException("Invalid SMTP port number in config exception: "+properties.getProperty("smtpport")); 
        }

    }

    @Override
    public TransferManager createTransferManager() {
        return new Pop3SmtpTransferManager(this);
    }

    public String getRcptAddress() {
        return rcptAddress;
    }

    public void setRcptAddress(String rcptAddress) {
        this.rcptAddress = rcptAddress;
    }

    public String getPop3Host() {
        return pop3Host;
    }

    public void setPop3Host(String pop3Host) {
        this.pop3Host = pop3Host;
    }

    public String getPop3Password() {
        return pop3Password;
    }

    public void setPop3Password(String pop3Password) {
        this.pop3Password = pop3Password;
    }

    public int getPop3Port() {
        return pop3Port;
    }

    public void setPop3Port(int pop3Port) {
        this.pop3Port = pop3Port;
    }

    public String getPop3Username() {
        return pop3Username;
    }

    public void setPop3Username(String pop3Username) {
        this.pop3Username = pop3Username;
    }

    public boolean isPop3SslEnabled() {
        return pop3SslEnabled;
    }

    public void setPop3SslEnabled(boolean pop3SslEnabled) {
        this.pop3SslEnabled = pop3SslEnabled;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public SmtpMode getSmtpMode() {
        return smtpMode;
    }

    public void setSmtpMode(SmtpMode smtpMode) {
        this.smtpMode = smtpMode;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }
}
