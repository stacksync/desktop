package com.stacksync.desktop.util;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.stacksync.commons.models.abe.ATNode;
import com.stacksync.commons.models.abe.AccessTree;
import com.stacksync.commons.models.abe.Gate;
import com.stacksync.commons.models.abe.Leaf;
import java.util.ArrayList;

/**
 *
 * @author marcruiz
 */
public class AccessTreeConverter {
    
    public static ATNode transformTreeFromABEClientToCommons(com.ast.cloudABE.accessTree.AccessTree accessTree){
       return transformTreeFromABEClientToCommons(null,accessTree.getRoot());
    }
    
    public static ATNode transformTreeFromABEClientToCommons(ATNode parent, com.ast.cloudABE.accessTree.ATNode child){
        
        ATNode transformedChild=null;
        
        if(child instanceof com.ast.cloudABE.accessTree.Gate){
            
            if (((com.ast.cloudABE.accessTree.Gate) child).getGateType().equals(com.ast.cloudABE.accessTree.Gate.GateType.OR)){
                   transformedChild = new Gate(parent,Gate.GateType.OR);
            } else {
                transformedChild = new Gate(parent,Gate.GateType.AND);
            }

            ArrayList<com.ast.cloudABE.accessTree.ATNode> childs = ((com.ast.cloudABE.accessTree.Gate) child).getChildren();
            
            for (com.ast.cloudABE.accessTree.ATNode childNode:childs){
                ((Gate)transformedChild).addChild(transformTreeFromABEClientToCommons(transformedChild, childNode));
            }
            
        }
        
        else if(child instanceof com.ast.cloudABE.accessTree.Leaf){
            transformedChild = new Leaf(parent,((com.ast.cloudABE.accessTree.Leaf) child).getAttribute());
        }
        
        return transformedChild;
    }
    
    
    public static com.ast.cloudABE.accessTree.ATNode transformTreeFromCommonsToABEClient(AccessTree accessTree){
       return transformTreeFromCommonsToABEClient(null,accessTree.getRoot());
    }
    
    public static com.ast.cloudABE.accessTree.ATNode transformTreeFromCommonsToABEClient(com.ast.cloudABE.accessTree.ATNode parent,  ATNode child){
        
        com.ast.cloudABE.accessTree.ATNode transformedChild=null;
        
        if(child instanceof Gate){
            
            if (((Gate) child).getGateType().equals(Gate.GateType.OR)){
                   transformedChild = new com.ast.cloudABE.accessTree.Gate(parent,com.ast.cloudABE.accessTree.Gate.GateType.OR);
            } else {
                transformedChild = new com.ast.cloudABE.accessTree.Gate(parent,com.ast.cloudABE.accessTree.Gate.GateType.AND);
            }

            ArrayList<ATNode> childs = ((Gate) child).getChildren();
            
            for (ATNode childNode:childs){
                ((com.ast.cloudABE.accessTree.Gate)transformedChild).addChild(transformTreeFromCommonsToABEClient(transformedChild, childNode));
            }
            
        }
        else if(child instanceof Leaf){
            transformedChild = new com.ast.cloudABE.accessTree.Leaf(parent,((Leaf) child).getAttribute());
        }
        
        return transformedChild;
    }
}
