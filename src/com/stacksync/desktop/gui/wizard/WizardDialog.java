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
package com.stacksync.desktop.gui.wizard;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.gui.settings.SettingsPanel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import org.apache.log4j.Logger;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class WizardDialog extends JDialog {
    private static final Config config = Config.getInstance();
    private final ResourceBundle resourceBundle = config.getResourceBundle();
    private static final Logger logger = Logger.getLogger(WizardDialog.class.getName());
    
    private JFrame parent;
    private int currentPanelIndex;
    private SettingsPanel[] panels;
    
    private StackSyncServerPanel panelStackSyncServer;
    private StackSyncTestPanel panelStackSyncTest;
    private ConnectionPanel panelProfileBasic;
    private ConnectionsPanel panelProfileBasics;
    private MetadataPanel panelMetadataServer;
    private EncryptionPanel panelEncryption;
    private RepositoryTestPanel panelRepositoryTest;
    private FoldersPanel panelFolders;

    private Profile profile;

    /** Creates new form WizardDialog */   
    public WizardDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        
        this.parent = parent;
        
        initComponents();
        initWizard();
        
        String title = getTitle();
        if(title == null || title.isEmpty()){
            setTitle("Stacksync");
        }
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }    


    private void initWizard() {        
        setLocationRelativeTo(null); // center
        getRootPane().setDefaultButton(btnNext);

        // Image
        lblLeftImage.setText("");
        lblLeftImage.setIcon(new ImageIcon(WizardDialog.class.getResource("/logo48.png")));
        
        // Profile
        profile = new Profile(); // set if successfully created!

        // Panels
        currentPanelIndex = 0;
        panelStackSyncServer = new StackSyncServerPanel(profile);
        panelProfileBasic = new ConnectionPanel(profile);
        panelProfileBasics = new ConnectionsPanel(profile);
        panelMetadataServer = new MetadataPanel(profile);
        panelEncryption = new EncryptionPanel(profile);
        panelRepositoryTest = new RepositoryTestPanel(profile);
        panelFolders = new FoldersPanel(profile);
        panelStackSyncTest = new StackSyncTestPanel(profile);
        
        if(Config.getInstance().isExtendedMode()){
            panels = new SettingsPanel[] {panelStackSyncServer,
                                          //panelProfileBasics,
                                          //panelMetadataServer,
                                          panelStackSyncTest,
                                          panelEncryption,
                                          panelFolders
                                          //panelRepositoryTest
            };

        } else{
            panels = new SettingsPanel[] {panelStackSyncServer,
                                          //panelProfileBasic,
                                          //panelMetadataServer,
                                          panelStackSyncTest,
                                          panelEncryption,
                                          panelFolders
                                          //panelRepositoryTest
            };

        }
        
        // Listeners
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                profile = null;
            }
        });

        showCurrentPanel(true);
    }



    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblLeftImage = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        btnNext = new javax.swing.JButton();
        btnBack = new javax.swing.JButton();
        scrMain = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Storage preferences"); // NOI18N
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(254, 254, 254));
        jPanel1.setName("jPanel1"); // NOI18N

        lblLeftImage.setText("(image)");
        lblLeftImage.setName("lblLeftImage"); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblLeftImage, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblLeftImage, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jPanel2.setName("jPanel2"); // NOI18N

        btnNext.setFont(btnNext.getFont());
        btnNext.setText("Next >");
        btnNext.setName("btnNext"); // NOI18N
        btnNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextActionPerformed(evt);
            }
        });

        btnBack.setFont(btnBack.getFont());
        btnBack.setText("< Back");
        btnBack.setName("btnBack"); // NOI18N
        btnBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBackActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(0, 426, Short.MAX_VALUE)
                .addComponent(btnBack, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnNext, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnNext)
                    .addComponent(btnBack))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(172, 431, 650, -1));

        scrMain.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        scrMain.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrMain.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrMain.setViewportBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        scrMain.setName("scrMain"); // NOI18N
        getContentPane().add(scrMain, new org.netbeans.lib.awtextra.AbsoluteConstraints(172, 0, 660, 430));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBackActionPerformed
        switch (currentPanelIndex) {
            case 0: // "Cancel"
                profile = null; // Nullify!
                dispose();
                parent.dispose();
                break;
                
            default:
                panels[currentPanelIndex].clean();                
                                
                currentPanelIndex--;
                
                if (!config.isExtendedMode()) {
                    if (panels[currentPanelIndex].equals(panelEncryption)) {
                        currentPanelIndex--;
                    }
                }
                
                if(panels[currentPanelIndex].equals(panelMetadataServer) && !config.isExtendedMode()){ //hides the metadata panel
                    currentPanelIndex--;
                }
                                
                showCurrentPanel(true);
                break;
        }
    }//GEN-LAST:event_btnBackActionPerformed

    private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextActionPerformed
        SettingsPanel lastPanel = panels[currentPanelIndex];
        
        if(lastPanel.check()){
            lastPanel.save();
            if (lastPanel == panelStackSyncServer){

                currentPanelIndex++;
                showCurrentPanel(false);
                
                if(Config.getInstance().isExtendedMode()){                    
                    panelStackSyncTest.doRepoAction(new TestListener() {
                        @Override
                        public void actionCompleted(boolean success) {
                            if (success) {
                                btnBack.setEnabled(false);
                                btnNext.setEnabled(true);                        
                            } else {
                                btnBack.setEnabled(true);
                                btnNext.setEnabled(false);                                                
                            }                    
                        }

                        @Override
                        public void setError(Throwable e) { panelStackSyncTest.setError(e); }
                        @Override
                        public void setStatus(String s) { panelStackSyncTest.setStatus(s); }
                    });                           
                } else {
                    panelStackSyncTest.doRepoAction(new TestListener() {
                        @Override
                        public void actionCompleted(boolean success) {
                            if (success) {
                                btnBack.setEnabled(false);
                                btnNext.setEnabled(true);                        
                            } else {
                                btnBack.setEnabled(true);
                                btnNext.setEnabled(false);                                                
                            }                    
                        }

                        @Override
                        public void setError(Throwable e) { panelStackSyncTest.setError(e); }
                        @Override
                        public void setStatus(String s) { panelStackSyncTest.setStatus(s); }
                    });
                }

                return;
            } else if (currentPanelIndex == panels.length-1) { // Last panel done!
                
                logger.info("profile = " + profile);
                setVisible(false);
                dispose();
                
                parent.setVisible(false);
                parent.dispose();
                return;
            }

            currentPanelIndex++; 
            showCurrentPanel(true);
              
            //hides the metadata panel
            if(panels[currentPanelIndex].equals(panelMetadataServer) && !config.isExtendedMode()){                
                panels[currentPanelIndex].save();
                currentPanelIndex++;                     
                showCurrentPanel(true);
            }
            
            if (!config.isExtendedMode()) {
                if (panels[currentPanelIndex].equals(panelEncryption)) {
                    panels[currentPanelIndex].save();
                    currentPanelIndex++;
                    showCurrentPanel(true);
                }
            }
        }
    }//GEN-LAST:event_btnNextActionPerformed
    
    static class DummyFrame extends JFrame {
        DummyFrame(String title) {
            super(title);
            URL test = WizardDialog.class.getResource("/logo48.png");
            super.setIconImage(new ImageIcon(WizardDialog.class.getResource("/logo48.png")).getImage());
            
            setUndecorated(true);
            setVisible(true);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
        }
    }
    
    public static Profile showWizard(Boolean modal) {
        WizardDialog dialog = new WizardDialog(new DummyFrame("Stacksync"), modal);        
        
        dialog.setIconImage(new ImageIcon(WizardDialog.class.getResource("logo48.png")).getImage());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.pack();
        
        dialog.setVisible(true);
        return dialog.getProfile();
    }   
    
    //solve bug in openJdk that blocked when the forms is closing
    public static Profile showWizardOpenJdk(Boolean modal){
        WorkerWizardDialog worker = new WorkerWizardDialog(null, modal);
        
        try {
            java.awt.EventQueue.invokeAndWait(worker);
        } catch (InterruptedException ex) {
            logger.error("Exception: ", ex);
        } catch (InvocationTargetException ex) {
            logger.error("Exception: ", ex);
        }        
        
        return worker.getProfile();
    }
    

    public Profile getProfile() {
        return profile;
    }

    private void showCurrentPanel(boolean enableNext) {
        btnBack.setEnabled(true);
        btnNext.setEnabled(enableNext);
        
        if (enableNext) {
            btnNext.requestFocus();
        }

        if (currentPanelIndex == 0) {
            btnBack.setText(resourceBundle.getString("wizard_cancel"));
            btnNext.setText(resourceBundle.getString("wizard_next"));
        } else if (panels[currentPanelIndex] == panelFolders) {
            btnBack.setText(resourceBundle.getString("wizard_back"));
            //btnNext.setText(resourceBundle.getString("wizard_create_profile"));
            btnNext.setText(resourceBundle.getString("wizard_next"));
        } else if (currentPanelIndex == panels.length-1) {            
            btnBack.setText(resourceBundle.getString("wizard_cancel"));
            btnBack.setEnabled(false);
            btnNext.setText(resourceBundle.getString("wizard_activate_profile"));
            //btnNext.setText(resourceBundle.getString("wizard_next"));
        } else {
            btnBack.setText(resourceBundle.getString("wizard_back"));
            btnNext.setText(resourceBundle.getString("wizard_next"));
        }
        
        panels[currentPanelIndex].load();        
        scrMain.setViewportView(panels[currentPanelIndex]);

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack;
    private javax.swing.JButton btnNext;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel lblLeftImage;
    private javax.swing.JScrollPane scrMain;
    // End of variables declaration//GEN-END:variables

    
    public static void main(String args[]) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException, ConfigException {
        config.load();
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("Wizard opened");
                System.out.println("RESULT = " + WizardDialog.showWizard(true));
            }
        });
    }
    
}
