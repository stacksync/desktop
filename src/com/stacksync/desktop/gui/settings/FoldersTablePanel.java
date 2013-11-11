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

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.Environment.OperatingSystem;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.gui.error.ErrorMessage;
import com.stacksync.desktop.logging.LogConfig;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FoldersTablePanel extends SettingsPanel {
    private final Logger logger = Logger.getLogger(FoldersTablePanel.class.getName());
    private final Environment env = Environment.getInstance(); 

    public FoldersTablePanel(Profile profile) {
        this.profile = profile;
        initComponents();

        initButtons();
        initTable();
        
        if(profile.getFolders().list().size() > 0){
            btnAdd.setVisible(false);
        }
        
        /// setting text///
        btnAdd.setText(resourceBundle.getString("ftp_add_sync_folder"));
        btnEdit.setText(resourceBundle.getString("ftp_edit_sync_folder"));        
    }

    private void initButtons() {
        btnAdd.setVisible(false);
        btnEdit.setEnabled(false);
        lblLoading.setVisible(false);
    }

    private void initTable() {
        // Content Model & Renderer
        tblFolders.setModel(new FolderTableModel());
        //tblFolders.getC

        // Columns
        tblFolders.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        TableColumn colActive = tblFolders.getColumnModel().getColumn(FolderTableModel.COLUMN_INDEX_ACTIVE);
        colActive.setPreferredWidth(50);
        colActive.setMaxWidth(50);
        colActive.setResizable(false);

        TableColumn colRemote = tblFolders.getColumnModel().getColumn(FolderTableModel.COLUMN_INDEX_REMOTE);
        colRemote.setPreferredWidth(70);

        TableColumn colLocal = tblFolders.getColumnModel().getColumn(FolderTableModel.COLUMN_INDEX_LOCAL);
        colLocal.setPreferredWidth(320);

        // Other stuff
        tblFolders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblFolders.setShowHorizontalLines(false);
        tblFolders.setShowVerticalLines(false);
        tblFolders.setBorder(BorderFactory.createEmptyBorder());

        // Listeners
        tblFolders.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                btnEdit.setEnabled(e.getFirstIndex() >= 0);
            }
        });

        tblFolders.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblFolders.getSelectedRow() >= 0) {
                    btnEditActionPerformed(null);
                }
            }
        });
        
        
    }

    @Override
    public void load() {
        // Automatically loaded via FolderTableModel! Nice :-D
        
        // If repository defined, i.g. this is not in the "New Profile" wizard,
        // to try to retrieve the available remoteIds
        if (profile.getRepository().isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {                    
                    logger.info(config.getMachineName()+"#Retrieving available remoteIds from repository.");
                    
                    lblLoading.setVisible(true);
                    lblLoading.setText(resourceBundle.getString("ftp_loading"));

                    try {

                        for (String remoteId : profile.getRepository().getAvailableRemoteIds()) {
                            logger.info(config.getMachineName()+"#remoteID"+remoteId);

                            if (profile.getFolders().get(remoteId) != null) {
                                continue;
                            }

                            Folder folder = new Folder(profile);
                            folder.setRemoteId(remoteId);
                            folder.setLocalFile(new File(env.getDefaultUserHome() + "stacksync_folder"));

                            profile.getFolders().add(folder);
                        }

                        tblFolders.updateUI();
                        lblLoading.setVisible(false);
                        
                        if(profile.getFolders().list().size() > 0){
                            btnAdd.setVisible(false);
                        }
                    } catch (Exception ex) {
                        logger.error("Could not load repository infos: ", ex);
                        LogConfig.sendErrorLogs();
                        lblLoading.setText(resourceBundle.getString("ftp_err_loading_repository_folders"));
                    } 
                }
            }, "UpdateRepo").start();
        } else { //repository is not connected
            
            Folder folder = new Folder(profile);
            folder.setRemoteId("stacksync");
            folder.setLocalFile(new File(env.getDefaultUserHome() + "stacksync_folder"));

            profile.getFolders().add(folder);
            tblFolders.updateUI();
        }
    }

    @Override
    public void save() {        
        int beforeAvailRemoteIds = profile.getRepository().getAvailableRemoteIds().size();        
        for (Folder folder: profile.getFolders().list()) {
            profile.getRepository().getAvailableRemoteIds().add(folder.getRemoteId());
            if(!folder.getLocalFile().exists()){
                folder.getLocalFile().mkdirs();
                
                // Creates a folderLink in favorites
                if(env.getOperatingSystem() == OperatingSystem.Windows){
                    File file = new File(env.getDefaultUserHome() + "Links");
                    if(file.exists()){
                        FileUtil.createWindowsLink(env.getDefaultUserHome() + "Links\\Stacksync.lnk", folder.getLocalFile().getPath());
                    }
                    
                    Locale locale = new Locale("en", "US");        
                    Locale defaultLocale = Locale.getDefault();
                    if(defaultLocale.getLanguage().toLowerCase().compareTo("es") == 0){
                        locale = new Locale("es", "ES");
                    } else if(defaultLocale.getLanguage().toLowerCase().compareTo("fr") == 0){
                        locale = new Locale("fr", "FR");
                    }

                    ResourceBundle resourceBundletmp = ResourceBundle.getBundle(Constants.RESOURCE_BUNDLE, locale);                    
                    String desktopDirectory = resourceBundletmp.getString("desktop_directory");
                    
                    file = new File(env.getDefaultUserHome() + desktopDirectory);
                    if(file.exists()){
                        FileUtil.createWindowsLink(env.getDefaultUserHome() + desktopDirectory + "\\Stacksync.lnk", folder.getLocalFile().getPath());
                    }
                }
            }
        }        
        
        if (beforeAvailRemoteIds < profile.getRepository().getAvailableRemoteIds().size()) {
            logger.info(config.getMachineName()+"#New folders added. Marking repository as CHANGED.");            
            profile.getRepository().setChanged(true);
        }        
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tblFolders = new javax.swing.JTable();
        btnAdd = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        lblLoading = new javax.swing.JLabel();

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        tblFolders.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        tblFolders.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Sync Folder", "Local Folder"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblFolders.setName("tblFolders"); // NOI18N
        tblFolders.setRowHeight(24);
        tblFolders.setShowHorizontalLines(false);
        tblFolders.setShowVerticalLines(false);
        tblFolders.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tblFolders);

        btnAdd.setText("Add Sync Folder");
        btnAdd.setName("btnAdd"); // NOI18N
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnEdit.setText("Edit Sync Folder");
        btnEdit.setName("btnEdit"); // NOI18N
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });

        lblLoading.setText("jLabel1");
        lblLoading.setName("lblLoading"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 464, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblLoading, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                .addGap(117, 117, 117)
                .addComponent(btnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAdd)
                    .addComponent(btnEdit)
                    .addComponent(lblLoading))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        Folder folder = FolderDialog.showDialog(null);
        if (folder != null) {
            profile.getFolders().add(folder);
            tblFolders.updateUI();
            btnAdd.setVisible(false);
        }
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        if (tblFolders.getSelectionModel().isSelectionEmpty()) {
            return;
        }

        Folder folder = profile.getFolders().list().get(tblFolders.getSelectionModel().getLeadSelectionIndex());
        if (FolderDialog.showDialog(null, folder) == null) {
            return;
        }

        tblFolders.updateUI();
    }//GEN-LAST:event_btnEditActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnEdit;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblLoading;
    private javax.swing.JTable tblFolders;
    // End of variables declaration//GEN-END:variables

    @Override
    public void clean() { 
        profile.getFolders().clear();    
    }
    
    @Override
    public boolean check() { 
        boolean check = false;
        
        for(Folder folder: profile.getFolders().list()){
            if(folder.isActive()){
                if(folder.getLocalFile() != null && folder.getLocalFile().getPath().length() > 0){
                    check = true;
                }
            }
        }
        
        if(!check){
            ErrorMessage.showMessage(this, "Error", "You need to configurate a folder to sincronize.");
        }
        
        return check;
    }

    public class FolderTableHeaderRenderer extends JLabel implements TableCellRenderer {
        // This method is called each time a column header
        // using this renderer needs to be rendered.

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
            // 'value' is column header value of column 'vColIndex'
            // rowIndex is always -1
            // isSelected is always false
            // hasFocus is always false

            // Configure the component with the specified value
            setText(value.toString());

            // Set tool tip if desired
            //setToolTipText((String)value);

            // Since the renderer is a component, return itself
            return this;
        }

        // The following methods override the defaults for performance reasons
        @Override
        public void validate() { }

        @Override
        public void revalidate() { }

        @Override
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) { }

        @Override
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) { }
    }

    public class FolderTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {

            JLabel cellSpacingLabel = new JLabel();

            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
                cellSpacingLabel = null;
            } else {
                setBackground(table.getBackground());
                setBorder(null);
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setBorder(null);
            } else {
                setBackground(table.getBackground());
                setBorder(null);
            }

            if (cellSpacingLabel != null) {
                cellSpacingLabel.setBorder(new CompoundBorder(new EmptyBorder(new Insets(1, 4, 1, 4)), cellSpacingLabel.getBorder()));
            }

            setOpaque(true);
            setText((String) value);

            return this;
        }
    }

    private class FolderTableModel implements TableModel {

        public static final int COLUMN_COUNT = 3;
        public static final int COLUMN_INDEX_ACTIVE = 0;
        public static final int COLUMN_INDEX_REMOTE = 1;
        public static final int COLUMN_INDEX_LOCAL = 2;
        private List<TableModelListener> listeners;

        public FolderTableModel() {
            listeners = new LinkedList<TableModelListener>();
        }

        @Override
        public int getRowCount() {
            return profile.getFolders().list().size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case COLUMN_INDEX_ACTIVE:
                    return resourceBundle.getString("ftp_active_index");
                case COLUMN_INDEX_REMOTE:
                    return resourceBundle.getString("ftp_remote_folder_index");
                case COLUMN_INDEX_LOCAL:
                    return resourceBundle.getString("ftp_local_folder_index");
            }

            return null;
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         *
         * @see http://download.oracle.com/javase/tutorial/uiswing/examples/components/TableRenderDemoProject/src/components/TableRenderDemo.java
         */
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COLUMN_INDEX_ACTIVE;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex > getRowCount() - 1) {
                throw new ArrayIndexOutOfBoundsException(resourceBundle.getString("err_outofbound_message_part1") + " " + getRowCount() + " " + resourceBundle.getString("err_outofbound_message_part2_rows") + " " + rowIndex + " " + resourceBundle.getString("err_outofbound_message_part3"));
            }

            Folder folder = profile.getFolders().list().get(rowIndex);

            switch (columnIndex) {
                case COLUMN_INDEX_ACTIVE:
                    return folder.isActive();
                case COLUMN_INDEX_REMOTE:
                    return folder.getRemoteId();
                case COLUMN_INDEX_LOCAL:
                    return (folder.getLocalFile() == null) ? "(not defined)" : folder.getLocalFile().getAbsolutePath();
            }

            throw new ArrayIndexOutOfBoundsException(resourceBundle.getString("ftp_err_outofbound_message_part1") + " " + getColumnCount() + " " + resourceBundle.getString("ftp_err_outofbound_message_part2_cols") + " " + columnIndex + " " + resourceBundle.getString("ftp_err_outofbound_message_part3"));
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex > getRowCount() - 1) {
                throw new ArrayIndexOutOfBoundsException(resourceBundle.getString("ftp_err_outofbound_message_part1") + " " + getRowCount() + " " + resourceBundle.getString("ftp_err_outofbound_message_part2_rows") + " " +  rowIndex + " " + resourceBundle.getString("ftp_err_outofbound_message_part3"));
            }

            Folder folder = profile.getFolders().list().get(rowIndex);

            switch (columnIndex) {
                case COLUMN_INDEX_ACTIVE:
                    folder.setActive((Boolean) aValue);
                    return;
                case COLUMN_INDEX_REMOTE:
                    folder.setRemoteId(aValue.toString());
                    return;
                case COLUMN_INDEX_LOCAL:
                    folder.setLocalFile(new File(aValue.toString()));
                    return;
            }

            throw new ArrayIndexOutOfBoundsException(resourceBundle.getString("ftp_err_outofbound_message_part1") + " " + getColumnCount() + " " + resourceBundle.getString("ftp_err_outofbound_message_part2_cols") + columnIndex + " " + resourceBundle.getString("ftp_err_outofbound_message_part3"));
        }

        @Override
        public synchronized void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        @Override
        public synchronized void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }
    }
}
