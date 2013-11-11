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

import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.config.profile.BrokerProperties;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.gui.error.ErrorMessage;
import com.stacksync.desktop.gui.settings.SettingsPanel;

/**
 *
 * @author pheckel
 */
public class MetadataPanel extends SettingsPanel {
    
    private final Environment env = Environment.getInstance();
    private BrokerProperties rabbitConnection;
    
    /** Creates new form ProfileBasicsPanel2 */
    public MetadataPanel(Profile profile) {
        this.profile = profile;   
        initComponents();
        
        /// setting text ///     
        lblTitle.setText("Metadata Server");
        lblConnectionTitle.setText("Connection details:");
        lblServerIp.setText("Server Ip:");
        lblMachineName.setText("Machine Name:");
         
        txtServerIp.setText("");
        txtMachineName.setText("");
        
        chkAuthenticated.setSelected(false);
        chkUseSSL.setSelected(false);
        txtUsername.setText("guest");
        txtPassword.setText("guest");
        
        rabbitConnection = config.getBrokerProps();        
    }

    @Override
    public void clean(){
        txtServerIp.setText("");
        txtMachineName.setText("");
        
        chkAuthenticated.setSelected(false);
        chkUseSSL.setSelected(false);
        txtUsername.setText("guest");
        txtPassword.setText("guest");
    }
    
    @Override
    public boolean check() {        
        boolean check = true;
        
        if(txtServerIp.getText().isEmpty()){            
            ErrorMessage.showMessage(this, "Error", "The server ip is empty.");
            check = false;
        }
        
        if(txtMachineName.getText().isEmpty()){            
            ErrorMessage.showMessage(this, "Error", "The machine name is empty.");
            check = false;
        }
        
        if(chkAuthenticated.isSelected()){
            if(txtUsername.getText().isEmpty()){
                ErrorMessage.showMessage(this, "Error", "The username is empty.");
                check = false;
            }

            String password = new String(txtPassword.getPassword());
            if(password.isEmpty()){
                ErrorMessage.showMessage(this, "Error", "The password is empty.");
                check = false;
            }
        }
        
        return check;
    }

    @Override
    public void load() {        
        // set the connection parameters
        Connection conn = profile.getRepository().getConnection();        
        rabbitConnection.setHost(conn.getHost());
        rabbitConnection.setUsername(conn.getUsername());
        rabbitConnection.setPassword(conn.getPassword());        
        
        txtServerIp.setText(rabbitConnection.getHost());        
        spnPort.setValue(rabbitConnection.getPort());
        
        chkUseSSL.setSelected(rabbitConnection.enableSsl());
        
        txtUsername.setText(rabbitConnection.getUsername());
        txtPassword.setText(rabbitConnection.getPassword());
        
        txtMachineName.setText(env.getMachineName());
    }

    @Override
    public void save() {        
        config.setMachineName(txtMachineName.getText());
        
        rabbitConnection.setHost(txtServerIp.getText());
        rabbitConnection.setPort(Integer.parseInt(spnPort.getValue().toString()));
                
        rabbitConnection.setEnableSsl(chkUseSSL.isSelected());
                
        String username = "guest";
        String password = "guest";
        if(chkAuthenticated.isSelected()){
            username = txtUsername.getText();        
            password = new String(txtPassword.getPassword());            
        }
        
        rabbitConnection.setUsername(username);                    
        rabbitConnection.setPassword(password);
        
        rabbitConnection.setRPCReply(txtMachineName.getText());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked") 
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle = new javax.swing.JLabel();
        lblConnectionTitle = new javax.swing.JLabel();
        lblServerIp = new javax.swing.JLabel();
        txtServerIp = new javax.swing.JTextField();
        lblMachineName = new javax.swing.JLabel();
        txtMachineName = new javax.swing.JTextField();
        lblPort = new javax.swing.JLabel();
        spnPort = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();
        chkAuthenticated = new javax.swing.JCheckBox();
        chkUseSSL = new javax.swing.JCheckBox();

        lblTitle.setFont(lblTitle.getFont().deriveFont(lblTitle.getFont().getStyle() | java.awt.Font.BOLD, lblTitle.getFont().getSize()+3));
        lblTitle.setText("Metadata Server");
        lblTitle.setName("lblTitle"); // NOI18N

        lblConnectionTitle.setFont(lblConnectionTitle.getFont().deriveFont(lblConnectionTitle.getFont().getStyle() | java.awt.Font.BOLD));
        lblConnectionTitle.setText("Connection Details");
        lblConnectionTitle.setName("lblConnectionTitle"); // NOI18N

        lblServerIp.setText("Server Ip:");
        lblServerIp.setName("lblServerIp"); // NOI18N

        txtServerIp.setName("txtServerIp"); // NOI18N

        lblMachineName.setText("Machine Name:");
        lblMachineName.setName("lblMachineName"); // NOI18N

        txtMachineName.setName("txtMachineName"); // NOI18N

        lblPort.setText("Port:");
        lblPort.setName("lblPort"); // NOI18N

        spnPort.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(5672), null, null, Integer.valueOf(1)));
        spnPort.setEditor(new javax.swing.JSpinner.NumberEditor(spnPort, "0000"));
        spnPort.setName("spnPort"); // NOI18N
        spnPort.setValue(5672);

        jLabel1.setText("Username:");
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText("Password:");
        jLabel2.setName("jLabel2"); // NOI18N

        txtUsername.setEnabled(false);
        txtUsername.setName("txtUsername"); // NOI18N

        txtPassword.setEnabled(false);
        txtPassword.setName("txtPassword"); // NOI18N

        chkAuthenticated.setText("Authenticated");
        chkAuthenticated.setName("chkAuthenticated"); // NOI18N
        chkAuthenticated.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkAuthenticatedActionPerformed(evt);
            }
        });

        chkUseSSL.setText("Use ssl");
        chkUseSSL.setName("chkUseSSL"); // NOI18N
        chkUseSSL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkUseSSLActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTitle)
                            .addComponent(lblConnectionTitle))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblMachineName)
                            .addComponent(lblServerIp)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkAuthenticated)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(txtServerIp)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(lblPort)
                                    .addGap(4, 4, 4)
                                    .addComponent(spnPort, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(txtMachineName, javax.swing.GroupLayout.PREFERRED_SIZE, 375, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(chkUseSSL))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle)
                .addGap(33, 33, 33)
                .addComponent(lblConnectionTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblServerIp)
                    .addComponent(txtServerIp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPort)
                    .addComponent(spnPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkUseSSL)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(chkAuthenticated)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMachineName)
                    .addComponent(txtMachineName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void chkAuthenticatedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkAuthenticatedActionPerformed
        if(chkAuthenticated.isSelected()){
            txtUsername.setEnabled(true);
            txtPassword.setEnabled(true);
        } else{            
            txtUsername.setEnabled(false);
            txtPassword.setEnabled(false);
        }
    }//GEN-LAST:event_chkAuthenticatedActionPerformed

    private void chkUseSSLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkUseSSLActionPerformed
        if(chkUseSSL.isSelected()){
            spnPort.setValue(5673);
        } else{
            spnPort.setValue(5672);
        }
    }//GEN-LAST:event_chkUseSSLActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox chkAuthenticated;
    private javax.swing.JCheckBox chkUseSSL;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblConnectionTitle;
    private javax.swing.JLabel lblMachineName;
    private javax.swing.JLabel lblPort;
    private javax.swing.JLabel lblServerIp;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JSpinner spnPort;
    private javax.swing.JTextField txtMachineName;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtServerIp;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
