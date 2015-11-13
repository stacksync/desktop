package com.stacksync.desktop.gui.wizard;

import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.config.profile.BrokerProperties;
import com.stacksync.desktop.gui.error.ErrorMessage;
import com.stacksync.desktop.gui.settings.SettingsPanel;

public class MetadataPanel extends SettingsPanel {
    
    private final Environment env = Environment.getInstance();
    private BrokerProperties rabbitConnection;
    
    public MetadataPanel(Profile profile) {
        this.profile = profile;   
        initComponents();
        
        /// setting text ///     
        lblTitle.setText("Connection details");
        lblServerIp.setText("RabbitMQ Ip:");
        lblMachineName.setText("Machine Name:");
         
        txtServerIp.setText("");
        txtServerIp.setSize(200, 24);
        txtMachineName.setText("");
        
        chkAuthenticated.setSelected(false);
        chkUseSSL.setSelected(false);

        txtUsername.setText("guest");
        txtPassword.setText("guest");

        rabbitConnection = config.getBrokerProps();
        
        hideFields();
        
        this.setupDefaultInfo();
    }
    
    private void setupDefaultInfo() {
        txtMachineName.setText(env.getDeviceName());
        rabbitConnection.setHost("iostack.urv.cat");
        rabbitConnection.setPort(5672);
                
        rabbitConnection.setEnableSsl(false);
                
        String username = "guest";
        String password = "guest";
        rabbitConnection.setUsername(username);                    
        rabbitConnection.setPassword(password);
    }
    
    private void hideFields() {
        chkAuthenticated.setVisible(false);
        chkUseSSL.setVisible(false);
        txtUsername.setVisible(false);
        txtPassword.setVisible(false);
        lblPassword.setVisible(false);
        lblUsername.setVisible(false);
        lblMachineName.setVisible(false);
        txtMachineName.setVisible(false);
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
        txtMachineName.setText(env.getDeviceName());
    }

    @Override
    public void save() {
        
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
        lblServerIp = new javax.swing.JLabel();
        txtServerIp = new javax.swing.JTextField();
        lblMachineName = new javax.swing.JLabel();
        txtMachineName = new javax.swing.JTextField();
        lblPort = new javax.swing.JLabel();
        spnPort = new javax.swing.JSpinner();
        lblUsername = new javax.swing.JLabel();
        lblPassword = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();
        chkAuthenticated = new javax.swing.JCheckBox();
        chkUseSSL = new javax.swing.JCheckBox();

        lblTitle.setFont(lblTitle.getFont().deriveFont(lblTitle.getFont().getStyle() | java.awt.Font.BOLD, lblTitle.getFont().getSize()+3));
        lblTitle.setText("Connection details");
        lblTitle.setName("lblTitle"); // NOI18N

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

        lblUsername.setText("Username:");
        lblUsername.setName("lblUsername"); // NOI18N

        lblPassword.setText("Password:");
        lblPassword.setName("lblPassword"); // NOI18N

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
                        .addComponent(lblTitle)
                        .addGap(0, 0, 0))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblMachineName)
                            .addComponent(lblServerIp, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblUsername, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblPassword, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addGap(31, 31, 31)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(chkAuthenticated)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addComponent(txtServerIp, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                .addGap(61, 61, 61)
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
                    .addComponent(lblUsername)
                    .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPassword))
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
    private javax.swing.JLabel lblMachineName;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JLabel lblPort;
    private javax.swing.JLabel lblServerIp;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblUsername;
    private javax.swing.JSpinner spnPort;
    private javax.swing.JTextField txtMachineName;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtServerIp;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
