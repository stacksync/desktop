/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.config.profile;

import java.util.Properties;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Configurable;
import com.stacksync.desktop.exceptions.ConfigException;
import omq.common.util.ParameterQueue;

/**
 * TODO this class seems a typical legacy code example, but it's necessary to
 * load the brokers configuration
 *
 * @author gguerrero
 */
public class BrokerProperties implements Configurable {

    private static final String port = "5672";
    private static final String host = "10.30.236.175";
    
    private Properties properties = new Properties();

    public BrokerProperties() {
        properties.setProperty(ParameterQueue.USER_NAME, "guest");
        properties.setProperty(ParameterQueue.USER_PASS, "guest");

        // Set host info of rabbimq (where it is)
        properties.setProperty(ParameterQueue.RABBIT_HOST, host);
        properties.setProperty(ParameterQueue.RABBIT_PORT, port);
        properties.setProperty(ParameterQueue.ENABLE_SSL, "false");

        // Set info about where the message will be sent
        properties.setProperty(ParameterQueue.RPC_EXCHANGE, "rpc_global_exchange");

        // Set info about the queue & the exchange where the ResponseListener
        // will listen to.
        properties.setProperty(ParameterQueue.RPC_REPLY_QUEUE, "reply_queue");
    }

    public void setRPCExchange(String rpc_exchange) {
        properties.setProperty(ParameterQueue.RPC_EXCHANGE, rpc_exchange);
    }

    public String getRPCExchange() {
        return properties.getProperty(ParameterQueue.RPC_EXCHANGE);
    }

    public void setRPCReply(String queueName) {
        properties.setProperty(ParameterQueue.RPC_REPLY_QUEUE, "reply_" + queueName);
    }

    public String getHost() {
        return properties.getProperty(ParameterQueue.RABBIT_HOST);
    }

    public void setHost(String host) {
        properties.setProperty(ParameterQueue.RABBIT_HOST, host);
    }

    public Integer getPort() {
        return Integer.parseInt(properties.getProperty(ParameterQueue.RABBIT_PORT));
    }

    public void setPort(Integer port) {
        properties.setProperty(ParameterQueue.RABBIT_PORT, port.toString());
    }

    public String getUsername() {
        return properties.getProperty(ParameterQueue.USER_NAME);
    }

    public void setUsername(String username) {
        properties.setProperty(ParameterQueue.USER_NAME, username);
    }

    public String getPassword() {
        return properties.getProperty(ParameterQueue.USER_PASS);
    }

    public void setPassword(String password) {
        properties.setProperty(ParameterQueue.USER_PASS, password);
    }

    public Boolean enableSsl() {
        return Boolean.valueOf(properties.getProperty(ParameterQueue.ENABLE_SSL));
    }

    public void setEnableSsl(Boolean enable) {
        properties.setProperty(ParameterQueue.ENABLE_SSL, enable.toString());
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        setUsername(node.getProperty("username", "guest"));
        setPassword(node.getProperty("password", "guest"));

        // Set host info of rabbimq (where it is)
        setHost(node.getProperty("host", host));
        setPort(node.getInteger("port", 5672));
        setEnableSsl(node.getBoolean("enableSSL", false));

        setRPCExchange(node.getProperty("rpc_exchange", "rpc_global_exchange"));
    }

    @Override
    public void save(ConfigNode node) {
        node.setProperty("host", getHost());
        node.setProperty("port", getPort());

        node.setProperty("username", getUsername());
        node.setProperty("password", getPassword());
        node.setProperty("enableSSL", enableSsl());

        node.setProperty("rpc_exchange", getRPCExchange());
    }

    @Override
    public String toString() {
        return BrokerProperties.class.getSimpleName() + "[properties=" + properties + "]";
    }
}
