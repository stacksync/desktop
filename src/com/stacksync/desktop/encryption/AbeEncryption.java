/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.encryption;

import com.ast.cloudABE.cloudABEClient.CloudABEClient;
import com.ast.cloudABE.cloudABEClient.CloudABEClientAdapter;
import com.stacksync.desktop.exceptions.ConfigException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author javigd
 */
public class AbeEncryption implements Encryption {

    // TODO: Move constant into the proper config file
    private static final String defaultXMLPath = "./src/com/stacksync/desktop/encryption/attribute_universe.xml";
    private CloudABEClientAdapter cabe;

    public AbeEncryption() throws ConfigException {
        try {
            cabe = new CloudABEClientAdapter("null");
            //TODO: attribute universe should be obtained remotely beforehand
            cabe.setupABESystem(0, parseAttributeUniverse());
        } catch (Exception e) {
            throw new ConfigException(e.getMessage() + "\n ABE Encryption: wrong initializing parameters");
        }
    }

    @Override
    public void init() throws ConfigException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] encrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //TODO
        //cabe.encryptData(data, attributes);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] decrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //TODO
        //cabe.decryptData(data, attributes);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    //TODO: Move to ABE library
    private ArrayList<String> parseAttributeUniverse() throws SAXException, IOException, ParserConfigurationException {
        Node root = null;
        ArrayList<String> universe = new ArrayList<String>();

        /*
         Parse the xml file
         */
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        doc = builder.parse(defaultXMLPath);
        root = (Node) doc.getDocumentElement();
        /*
         Build the attribute universe set
         */
        if (root != null) {
            universe = buildAttSet(root);
        }

        return universe;
    }

    private static ArrayList<String> buildAttSet(Node root) {
        NodeList nodeList = root.getChildNodes();
        ArrayList<String> universe = new ArrayList<String>();
        if (nodeList.getLength() == 1) {
            universe.add(root.getNodeName());
        }
        for (int count = 0; count < nodeList.getLength(); count++) {
            Node tempNode = nodeList.item(count);
            /* make sure it is a node element */
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
                if (tempNode.hasChildNodes()) {
                    /* loop again if has child nodes */
                    universe.addAll(buildAttSet(tempNode));
                }
            }
        }
        return universe;
    }
}
