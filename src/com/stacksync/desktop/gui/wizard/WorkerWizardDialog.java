/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.gui.wizard;

import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.WindowConstants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;

/**
 *
 * @author gguerrero
 */
public class WorkerWizardDialog extends Thread{

    private static final Config config = Config.getInstance();
    
    private Profile profile;
    private Boolean modal;
    
    public WorkerWizardDialog(Profile profile, Boolean modal){
        super("WorkerWizardDialog");
        
        this.profile = profile;
        this.modal = modal;
    }
    
    
    public Profile getProfile(){
        return this.profile;
    }      
    
    
    @Override
    public void run() {
        WizardDialog dialog = new WizardDialog(new WizardDialog.DummyFrame("Stacksync"), modal);        
        
        dialog.setIconImage(new ImageIcon(WizardDialog.class.getResource("/logo48.png")).getImage());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.pack();
        
        dialog.setVisible(true);
        profile = dialog.getProfile();
    }
    
}
