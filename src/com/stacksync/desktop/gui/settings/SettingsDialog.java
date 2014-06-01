/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.gui.settings;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;

public class SettingsDialog extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(SettingsDialog.class.getName());
    private static final Config config = Config.getInstance();
    private ResourceBundle resourceBundle;
    
    /** Creates new form SettingsDialog */
    public SettingsDialog() {
        super();
        this.resourceBundle = config.getResourceBundle();
        initComponents();

        setTitle(resourceBundle.getString("sd_form_title"));
        setIconImage(new ImageIcon(SettingsDialog.class.getResource("/logo48.png")).getImage()); 
        
        // Init dialog!
        initDialog();

        initTreeModelStatic();
        initTreeListeners();
        initTreeUI();

        initSettingsPanels();
        
        /// setting text ///
        btnOkay.setText(resourceBundle.getString("sd_ok"));
        btnCancel.setText(resourceBundle.getString("sd_cancel"));
    }

    private void initDialog() {
        // Set some stuff
        setResizable(false);
        setLocationRelativeTo(null); // center
        getRootPane().setDefaultButton(btnOkay);

        // Load logos and buttons
        lblTopImage.setText("");
        lblTopImage.setIcon(new ImageIcon(SettingsDialog.class.getResource("/settings-top.png")));

        lblDonate.setText("");
        lblDonate.setIcon(new ImageIcon(SettingsDialog.class.getResource("/logo-urv.png")));

        // Add listeners
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                btnCancelActionPerformed(null);
            }
        });
    }

    private void initTreeModelStatic() {
        // Make model and pass it to tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        // - Application
        DefaultMutableTreeNode appNode = new DefaultMutableTreeNode(new ShowPanelTreeItem(resourceBundle.getString("sd_app_settings"), "application", new ApplicationPanel()));
        root.add(appNode);

        // - Application / Plugins
        appNode.add(new DefaultMutableTreeNode(new ShowPanelTreeItem(resourceBundle.getString("sd_plugins"), "plugins", new PluginsPanel())));

        /*appNode.add(new DefaultMutableTreeNode(new ShowPanelTreeItem("Proxy Configuration", "application", new ProxyPanel())));
        appNode.add(new DefaultMutableTreeNode(new ShowPanelTreeItem("Bandwidth Limits", "bandwidth", new BandwidthLimitPanel())));*/

        //
        // NOTE: Profiles are inserted after everything else, dynamically!
        //

        // - Create new profile
        //Icon customOpenIcon = new ImageIcon(config.getResDir() + File.separator + "settings-profile.png");
        
        // This code actives the option to create a new profile
        /*DefaultMutableTreeNode node = new DefaultMutableTreeNode(new ActionTreeItem(resourceBundle.getString("sd_create_new_profile"), "profile", ActionTreeItem.ActionTreeItemEvent.DOUBLE_CLICKED) {

            @Override
            public void doAction() {
                Profile profile = WizardDialog.showWizard(true);

                if (profile == null) {
                    logger.info("Cancel clicked.");
                    return;
                }

                logger.info("SUCCESS: " + profile);
                config.getProfiles().add(profile);
                addProfileToTree(profile, true);
                
                try {
                    profile.setActive(true);
                } catch (InitializationException ex) {
                    logger.warn("Exception: ", ex);
                } catch (StorageConnectException ex) {
                    logger.warn("Exception: ", ex);
                }
            }
        });
        
        root.add(node);*/

        // Set it!
        tree.setModel(new DefaultTreeModel(root));
    }

    private void initTreeListeners() {
        tree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                if (selectedNode == null) {
                    return;
                }

                Object userObj = selectedNode.getUserObject();
                if (userObj instanceof ShowPanelTreeItem) {
                    scrMain.setViewportView(((ShowPanelTreeItem) userObj).getPanel());
                }
            }
        });

        tree.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());

                if (selPath == null) {
                    return;
                }

                // Get clicked node
                DefaultMutableTreeNode clickedNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                Object userObj = clickedNode.getUserObject();

                if (userObj instanceof ActionTreeItem) {
                    ActionTreeItem action = (ActionTreeItem) userObj;

                    // React!
                    if (e.getClickCount() == 1 && action.getEvent() == ActionTreeItem.ActionTreeItemEvent.CLICKED) {
                        action.doAction();
                    } else if (e.getClickCount() == 2 && action.getEvent() == ActionTreeItem.ActionTreeItemEvent.DOUBLE_CLICKED) {
                        action.doAction();
                    }
                }
            }
        });
    }

    private void initTreeUI() {
        tree.setCellRenderer(new SettingsDialog.SettingsTreeCellRenderer());
        tree.setRootVisible(false);

        // Expand all
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void initSettingsPanels() {
        
        for (SettingsPanel panel : getSettingsPanels()) {
            panel.load();
        }

        tree.setSelectionRow(0);
    }

    public void addProfileToTree(Profile profile, boolean selectRow) {
        // Panels
        ProfilePanel profilePanel = new ProfilePanel(profile);
        RepositoryPanel repoPanel = new RepositoryPanel(profile);
        FoldersPanel foldersPanel = new FoldersPanel(profile);
        
        profilePanel.load();
        repoPanel.load();
        foldersPanel.load();

        // Add to tree
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

        DefaultMutableTreeNode profileNode = new DefaultMutableTreeNode(new ShowPanelTreeItem(profile.getName(), "profile", profilePanel));

        profileNode.add(new DefaultMutableTreeNode(new ShowPanelTreeItem(resourceBundle.getString("sd_remote_storage"), "repository", repoPanel)));
        profileNode.add(new DefaultMutableTreeNode(new ShowPanelTreeItem(resourceBundle.getString("sd_sync_folders"), "folders", foldersPanel)));
        
        model.insertNodeInto(profileNode, root, root.getChildCount() - 1);

        if (selectRow) {
            int profileNodeIndex = tree.getRowForPath(new TreePath(profileNode.getPath()));

            tree.expandRow(profileNodeIndex);
            tree.setSelectionRow(profileNodeIndex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws ConfigException, InterruptedException {
        config.load();
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                SettingsDialog dialog = new SettingsDialog();
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

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);

        // (Re-)Load panels
        if (b) {
            initSettingsPanels();
        }
    }

    private List<DefaultMutableTreeNode> getAllNodes(DefaultMutableTreeNode root) {
        // node is visited exactly once
        List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();

        if (root.getChildCount() >= 0) {
            for (Enumeration e = root.children(); e.hasMoreElements();) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();

                nodes.add(node);
                nodes.addAll(getAllNodes(node));
            }
        }

        return nodes;
    }

    private List<SettingsPanel> getSettingsPanels() {
        List<SettingsPanel> panels = new ArrayList<SettingsPanel>();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();

        for (DefaultMutableTreeNode node : getAllNodes(root)) {
            Object userObj = node.getUserObject();

            if (!(userObj instanceof ShowPanelTreeItem)) {
                continue;
            }

            ShowPanelTreeItem item = (ShowPanelTreeItem) userObj;
            panels.add(item.getPanel());
        }

        return panels;
    }

    /**
     * @see http://download.oracle.com/javase/tutorial/uiswing/examples/components/TreeIconDemo2Project/src/components/TreeIconDemo2.java
     */
    private class SettingsTreeCellRenderer extends DefaultTreeCellRenderer {

        public SettingsTreeCellRenderer() {
            super();

            setBorderSelectionColor(null);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            Component origComp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (!(value instanceof DefaultMutableTreeNode)) {
                return origComp;
            }

            Object userObj = ((DefaultMutableTreeNode) value).getUserObject();

            if (!(userObj instanceof TreeItem)) {
                return origComp;
            }

            setIcon(new ImageIcon(SettingsDialog.class.getResource("/settings-" + ((TreeItem) userObj).getIconFilename() + ".png")));
            return this;
        }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        treeScrollPane = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();
        pnlBottom = new javax.swing.JPanel();
        btnOkay = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        lblDonate = new javax.swing.JLabel();
        scrMain = new javax.swing.JScrollPane();
        pnlTop = new javax.swing.JPanel();
        lblTopImage = new javax.swing.JLabel();

        jLabel2.setText("TESTING");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("(test O_o)");
        setName("Syncany(test)"); // NOI18N

        treeScrollPane.setPreferredSize(new java.awt.Dimension(62, 444));

        tree.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        tree.setMaximumSize(new java.awt.Dimension(58, 68));
        tree.setName("tree"); // NOI18N
        tree.setPreferredSize(new java.awt.Dimension(58, 68));
        tree.setRowHeight(22);
        treeScrollPane.setViewportView(tree);

        pnlBottom.setPreferredSize(new java.awt.Dimension(1024, 55));

        btnOkay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOkayActionPerformed(evt);
            }
        });

        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        lblDonate.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblDonateMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout pnlBottomLayout = new javax.swing.GroupLayout(pnlBottom);
        pnlBottom.setLayout(pnlBottomLayout);
        pnlBottomLayout.setHorizontalGroup(
            pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlBottomLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(lblDonate, javax.swing.GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 320, Short.MAX_VALUE)
                .addComponent(btnOkay, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(102, 102, 102))
        );
        pnlBottomLayout.setVerticalGroup(
            pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlBottomLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlBottomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnOkay, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                    .addComponent(btnCancel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblDonate, javax.swing.GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE))
                .addGap(22, 22, 22))
        );

        scrMain.setBorder(null);

        pnlTop.setMinimumSize(new java.awt.Dimension(0, 0));
        pnlTop.setPreferredSize(new java.awt.Dimension(952, 88));

        javax.swing.GroupLayout pnlTopLayout = new javax.swing.GroupLayout(pnlTop);
        pnlTop.setLayout(pnlTopLayout);
        pnlTopLayout.setHorizontalGroup(
            pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        pnlTopLayout.setVerticalGroup(
            pnlTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 88, Short.MAX_VALUE)
        );

        lblTopImage.setToolTipText("");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTopImage, javax.swing.GroupLayout.PREFERRED_SIZE, 952, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(66, 66, 66)
                        .addComponent(pnlTop, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(treeScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 272, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrMain, javax.swing.GroupLayout.PREFERRED_SIZE, 644, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(pnlBottom, javax.swing.GroupLayout.PREFERRED_SIZE, 977, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(pnlTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTopImage, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(treeScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 424, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(scrMain, javax.swing.GroupLayout.PREFERRED_SIZE, 424, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlBottom, 62, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(60, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnOkayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkayActionPerformed
        for (SettingsPanel pnl : getSettingsPanels()) {
            pnl.save();
        }

        // Save config
        try {
            config.save();
        } catch (ConfigException ex) {
            logger.error(ex);
            RemoteLogs.getInstance().sendLog(ex);
        }
        
        // Update settings dialog
        new Thread(new Runnable() {
            @Override
            public void run() {
               Profile p = config.getProfile()  ;                  
                try {
                    p.setActive(p.isEnabled());
                } catch (InitializationException ex) {
                    logger.warn("Exception: ", ex);
                } catch (StorageConnectException ex) {
                    logger.warn("Exception: ", ex);
                }
             }
            
        },"UpdateProfiles").start();
                
        // Update tray menu
        Tray.getInstance().updateUI();        
        setVisible(false);
    }//GEN-LAST:event_btnOkayActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void lblDonateMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblDonateMouseClicked
        FileUtil.browsePage(Constants.APPLICATION_URL);
    }//GEN-LAST:event_lblDonateMouseClicked

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOkay;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel lblDonate;
    private javax.swing.JLabel lblTopImage;
    private javax.swing.JPanel pnlBottom;
    private javax.swing.JPanel pnlTop;
    private javax.swing.JScrollPane scrMain;
    private javax.swing.JTree tree;
    private javax.swing.JScrollPane treeScrollPane;
    // End of variables declaration//GEN-END:variables
}
