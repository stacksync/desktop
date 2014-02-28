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
package com.stacksync.desktop.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.logging.RemoteLogs;
import org.w3c.dom.*;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ConfigNode {
    private final Logger logger = Logger.getLogger(ConfigNode.class.getName());
    
    protected Document doc;
    public Node node;
    
    // Load
    public ConfigNode(Node node) {

        this.doc = node.getOwnerDocument();
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public void setProperty(String name, Object value) {
        ConfigNode property = findChildByName(name, true);
        property.getNode().setTextContent((value == null) ? null : value.toString());
    }

    public boolean hasProperty(String name) {
        return getProperty(name) != null;
    }

    public String getProperty(String name) {
        return getProperty(name, null);
    }

    public String getProperty(String name, String defaultValue) {
        ConfigNode property = findChildByName(name);

        if (property == null || property.getNode().getTextContent() == null || 
                property.getNode().getTextContent().equals("")) {
            return defaultValue;
        }

        return property.getNode().getTextContent();
    }

    public Integer getInteger(String name) throws ConfigException {
        try {
            String property = getProperty(name);
            return (property != null) ? Integer.parseInt(property) : null;
        } catch (Exception e) {
            throw new ConfigException("Not an int value for property '" + name + "': " + getProperty(name), e);
        }
    }

    public Integer getInteger(String name, Integer defaultValue) {
        try {
            Integer property = getInteger(name);
            return (property != null) ? property : defaultValue;
        } catch (ConfigException ex) {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String name) throws ConfigException {
        try {
            String property = getProperty(name);
            return (property != null) ? Boolean.parseBoolean(property) : null;
        } catch (Exception e) {
            throw new ConfigException("Not a bool value for property '" + name + "': " + getProperty(name), e);
        }
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        try {
            Boolean property = getBoolean(name);
            return (property != null) ? property : defaultValue;
        } catch (ConfigException ex) {
            return defaultValue;
        }
    }

    public File getFile(String name) throws ConfigException {
        try {
            String property = getProperty(name);
            return (property != null) ? new File(property) : null;
        } catch (Exception e) {
            throw new ConfigException("Not file value for property '" + name + "': " + getProperty(name), e);
        }
    }

    public File getFile(String name, File defaultValue) {
        try {
            File property = getFile(name);
            return (property != null) ? property : defaultValue;
        } catch (ConfigException ex) {
            return defaultValue;
        }
    }

    public void setAttribute(String name, Object value) {
        Attr attr = findAttribute(name, true);
        attr.setValue(value.toString());
    }

    public String getAttribute(String name) {
        Attr attr = findAttribute(name);
        return (attr == null) ? null : attr.getValue();
    }

    public final ConfigNode findChildByName(String nodeName) {
        return findChildByName(nodeName, false);
    }

    public final ConfigNode findChildByName(String nodeName, boolean create) {
        List<Node> children = findChildren(nodeName);

        if (!children.isEmpty()) {
            return new ConfigNode(children.get(0));
        }

        // Create if it does not exist
        if (create) {
            Node child = doc.createElement(nodeName);
            node.appendChild(child);

            return new ConfigNode(child);
        }

        return null;
    }

    public final ConfigNode findChildByXPath(String xpathExpr) {
        List<ConfigNode> children = findChildrenByXpath(xpathExpr);

        if (!children.isEmpty()) {
            return children.get(0);
        }

        return null;
    }

    public final ConfigNode findOrCreateChildByXpath(String xpathExpr, String createNodeName) {
        ConfigNode child = findChildByXPath(xpathExpr);

        if (child == null) {
            child = createChild(createNodeName);
        }

        return child;
    }

    public final List<ConfigNode> findChildrenByXpath(String xpathExpr) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            Object result = xpath.evaluate(xpathExpr, node, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            List<ConfigNode> nodeList = new ArrayList<ConfigNode>();

            for (int i = 0; i < nodes.getLength(); i++) {
                nodeList.add(new ConfigNode(nodes.item(i)));
            }

            return nodeList;

        } catch (XPathExpressionException ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);
            return null;
        }
    }

    public final List<Node> findChildren(String nodeName) {
        List<Node> nodes = new ArrayList<Node>();
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            if (!nodeName.equals(children.item(i).getNodeName())) {
                continue;
            }

            // Found
            nodes.add(children.item(i));
        }

        return nodes;
    }

    public final Attr findAttribute(String attrName) {
        return findAttribute(attrName, false);
    }

    public final Attr findAttribute(String attrName, boolean create) {
        Attr attr = (Attr) node.getAttributes().getNamedItem(attrName);

        if (attr != null) {
            return attr;
        }

        // Create?
        if (create) {
            attr = doc.createAttribute(attrName);
            ((Element) node).setAttributeNode(attr);
        }

        return attr;
    }

    public final ConfigNode createChild(String nodeName) {
        Node child = doc.createElement(nodeName);
        node.appendChild(child);

        return new ConfigNode(child);
    }
}
