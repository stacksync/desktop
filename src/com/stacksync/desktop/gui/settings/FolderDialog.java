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
package com.stacksync.desktop.gui.settings;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FolderDialog extends javax.swing.JDialog {
    private final Config config = Config.getInstance();
    
    private Folder folder;
    private boolean txtRemoteChanged;
    private boolean remoteFolderEditable;
    private ResourceBundle resourceBundle;

    private FolderDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        resourceBundle = config.getResourceBundle();
        initComponents();
        initDialog();

        updateOkayButton();	
        
        /// setting text ///
        setTitle(resourceBundle.getString("fd_sync_folder"));                
        jLabel1.setText(resourceBundle.getString("fd_local_folder"));                
        jLabel2.setText(resourceBundle.getString("fd_remote_identifier"));
        jLabel3.setText(resourceBundle.getString("fd_one_time_identifier"));
                
        btnBrowse.setText(resourceBundle.getString("fd_browse"));
        btnOkay.setText(resourceBundle.getString("fd_ok"));
        btnCancel.setText(resourceBundle.getString("fd_cancel"));
        jLabel4.setText(resourceBundle.getString("fd_note"));

    }

    private void initDialog() {
        folder = new Folder();
        txtRemoteChanged = false;
        remoteFolderEditable = true;

        // UI
        setLocationRelativeTo(null); // center
        getRootPane().setDefaultButton(btnOkay);

        // Listeners
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (remoteFolderEditable) {
                    btnBrowseActionPerformed(null);		
                }
            }
        });

        txtLocal.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {		
                updateOkayButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateOkayButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateOkayButton();
            }
        });

        txtRemote.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {		
                txtRemoteChanged = true;
                updateOkayButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                txtRemoteChanged = true;
                updateOkayButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                txtRemoteChanged = true;
                updateOkayButton();
            }
        });	
    }


    public static Folder showDialog(JFrame parent) {
        return showDialog(parent, new Folder(), true);
    }

    public static Folder showDialog(JFrame parent, Folder folder) {
        return showDialog(parent, folder, false);
    }
    
    public static Folder showDialog(JFrame parent, Folder folder, boolean remoteFolderEditable) {
        FolderDialog dialog = new FolderDialog(parent, true);

        dialog.setFolder(folder);
        dialog.setRemoteFolderEditable(remoteFolderEditable);	
        dialog.setVisible(true);

        return dialog.getFolder();
    }

    public void setFolder(Folder folder) {
        this.folder = folder;

        if (folder.getLocalFile() != null) {
            txtLocal.setText(folder.getLocalFile().getAbsolutePath());
        }

        if (folder.getRemoteId() != null) {
            txtRemote.setText(folder.getRemoteId());	
        }
    }

    public Folder getFolder() {
        // Updated by {okay,cancel}Clicked()
        return folder;
    }

    private void setRemoteFolderEditable(boolean b) {
        remoteFolderEditable = b;

        txtRemote.setEditable(b);
        txtRemote.setEnabled(b);
    }   

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtRemote = new javax.swing.JTextField();
        txtLocal = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        btnBrowse = new javax.swing.JButton();
        btnOkay = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Sync Folder");
        setResizable(false);

        jLabel1.setText("Local Folder:");
        jLabel1.setName("jLabel1"); // NOI18N

        jLabel2.setText("Remote Folder Identifier:");
        jLabel2.setName("jLabel2"); // NOI18N

        txtRemote.setName("txtRemote"); // NOI18N
        txtRemote.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtRemoteActionPerformed(evt);
            }
        });

        txtLocal.setName("txtLocal"); // NOI18N
        txtLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtLocalActionPerformed(evt);
            }
        });

        jLabel3.setText("Once set, this identifier cannot be changed!");
        jLabel3.setName("jLabel3"); // NOI18N

        btnBrowse.setText("Browse ...");
        btnBrowse.setName("btnBrowse"); // NOI18N
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });

        btnOkay.setText("OK");
        btnOkay.setName("btnOkay"); // NOI18N
        btnOkay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkayActionPerformed(evt);
            }
        });

        btnCancel.setText("Cancel");
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel4.setText("Note:");
        jLabel4.setName("jLabel4"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtLocal)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel2)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addComponent(txtRemote, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 104, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnBrowse))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnOkay, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtLocal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtRemote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnOkay)
                    .addComponent(btnCancel))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtRemoteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtRemoteActionPerformed

    }//GEN-LAST:event_txtRemoteActionPerformed

    private void btnOkayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkayActionPerformed
        folder.setLocalFile(new File(txtLocal.getText()));
        folder.setRemoteId(txtRemote.getText());

        setVisible(false);
    }//GEN-LAST:event_btnOkayActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        folder = null;
        setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseActionPerformed
        File selectedDirectory = FileUtil.showBrowseDirectoryDialog();

        if (selectedDirectory == null) {
            return;
        }

        // Set new
        txtLocal.setText(selectedDirectory.getAbsolutePath());

        if (!txtRemoteChanged) {
            txtRemote.setText(selectedDirectory.getName());
        }
    }//GEN-LAST:event_btnBrowseActionPerformed

    private void txtLocalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtLocalActionPerformed

    }//GEN-LAST:event_txtLocalActionPerformed

    private void updateOkayButton(){
        boolean valuesValid = 
            !txtLocal.getText().isEmpty() && 
            !txtRemote.getText().isEmpty() &&
            new File(txtLocal.getText()).exists();

        btnOkay.setEnabled(valuesValid);	
    }
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) throws InitializationException {
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                FolderDialog dialog = new FolderDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBrowse;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOkay;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JTextField txtLocal;
    private javax.swing.JTextField txtRemote;
    // End of variables declaration//GEN-END:variables


}
