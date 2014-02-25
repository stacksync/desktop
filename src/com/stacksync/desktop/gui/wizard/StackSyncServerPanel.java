package com.stacksync.desktop.gui.wizard;

import com.stacksync.desktop.config.profile.Account;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.gui.error.ErrorMessage;
import com.stacksync.desktop.gui.settings.SettingsPanel;

public class StackSyncServerPanel extends SettingsPanel {
    
    private String password;
    private String email;
	
    public StackSyncServerPanel(Profile profile) {
        this.profile = profile;
        initComponents();	
        
        /// setting text ///                        
        lblEmail.setText(resourceBundle.getString("cp_simple_email"));
        lblPassword.setText(resourceBundle.getString("cp_simple_password"));  
        
        txtEmail.requestFocus();
    }

    @Override
    public void load() {
        txtEmail.setText(this.email);
        txtPassword.setText(this.password);
    }

    @Override
    public void save() {
        this.email = getEmail();
        this.password = getPassword();
        
        Account account = this.profile.getAccount();
        account.setEmail(email);
        account.setPassword(password);
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        txtEmail = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();
        lblEmail = new javax.swing.JLabel();
        lblPassword = new javax.swing.JLabel();

        txtEmail.setName("txtEmail"); // NOI18N

        txtPassword.setName("txtPassword"); // NOI18N

        lblEmail.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblEmail.setText("__Email:");
        lblEmail.setName("lblEmail"); // NOI18N

        lblPassword.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblPassword.setText("__Password:");
        lblPassword.setName("lblPassword"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblPassword)
                    .addComponent(lblEmail))
                .addGap(72, 72, 72)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                    .addComponent(txtEmail))
                .addContainerGap(64, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblEmail))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPassword))
                .addContainerGap(96, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JPasswordField txtPassword;
    // End of variables declaration//GEN-END:variables

    @Override
    public void clean() {
        txtEmail.setText("");
        txtPassword.setText("");
    }

    private String getEmail(){
        return txtEmail.getText().trim();
    }
    
    private String getPassword(){
        return new String(txtPassword.getPassword());
    }
        
    @Override
    public boolean check() {
        String tmpEmail = getEmail();
        String tmpPassword = getPassword();
        
        // check UserName
        if(tmpEmail.isEmpty()){
            ErrorMessage.showMessage(this, "Error", "The email is empty.");
            return false;
        } else{        
            if(!tmpEmail.contains("@") || !tmpEmail.contains(".") || tmpEmail.contains("/")
                    || tmpEmail.contains("\\")){
               ErrorMessage.showMessage(this, "Error", "Invalid email.");
               return false;
            }
        }

        // check Password
        if(tmpPassword.isEmpty()){            
            ErrorMessage.showMessage(this, "Error", "The password is empty.");
            return false;
        }
        
        return true;
    }
}