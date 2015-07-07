package com.stacksync.desktop.gui.tray;

import com.ast.cloudABE.GUI.UIUtils;
import com.ast.cloudABE.cloudABEClient.CloudABEClient;
import com.ast.cloudABE.cloudABEClient.CloudABEClientAdapter;
import com.stacksync.commons.models.abe.KPABESecretKey;
import com.stacksync.commons.exceptions.ShareProposalNotCreatedException;
import com.stacksync.commons.exceptions.UserNotFoundException;
import com.stacksync.commons.models.abe.AccessTree;
import com.stacksync.desktop.ApplicationController;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.gui.error.ErrorMessage;
import com.stacksync.desktop.gui.sharing.SharePanel;
import com.stacksync.desktop.syncserver.Server;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import java.util.HashMap;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.apache.log4j.Logger;
import com.stacksync.desktop.util.AccessTreeConverter;

/**
 *
 * @author cotes
 */
public class TrayEventListenerImpl implements TrayEventListener {

    private ApplicationController controller;
    private final Logger logger = Logger.getLogger(TrayEventListenerImpl.class.getName());

    public TrayEventListenerImpl(ApplicationController controller) {
        this.controller = controller;
    }

    @Override
    public void trayEventOccurred(TrayEvent event) {
        switch (event.getType()) {
            case OPEN_FOLDER:
                File folder = new File((String) event.getArgs().get(0));
                FileUtil.openFile(folder);
                break;

            /*case PREFERENCES:
             settingsDialog.setVisible(true);
             break;*/
            case WEBSITE:
                FileUtil.browsePage(Constants.APPLICATION_URL);
                break;

            case WEBSITE2:
                FileUtil.browsePage(Constants.APPLICATION_URL2);
                break;

            case PAUSE_SYNC:
                this.controller.pauseSync();
                break;

            case RESUME_SYNC:
                this.controller.resumeSync();
                break;
            case SHARE:
                //Show share panel
                
                JFrame frame = createSharingFrame();

                SharePanel panel = new SharePanel(frame);

                frame.setContentPane(panel);
                frame.pack();
                frame.setVisible(true);

                synchronized (panel.lock) {
                    try {
                        panel.lock.wait();
                    } catch (InterruptedException ex) {
                        logger.error(ex);
                    }
                }
                
                String resourcePath = Environment.getInstance().getDefaultUserConfigDir().getAbsolutePath()+"/conf/abe/";
                CloudABEClient cabe = null;
                try {
                    cabe = new CloudABEClientAdapter(resourcePath);
                    cabe.loadABESystem(0);
                } catch (Exception ex) {
                    logger.error(ex);
                }
                
                HashMap<String,KPABESecretKey> emailsKeys = null;
                
                if (panel.isAbeEncrypted()) {
                    
                    emailsKeys = new HashMap<String,KPABESecretKey>();
                          
                    for (String email : panel.getEmails()) {
                        System.out.println("Setting permissions for: " + email);
                        try {
                            
                            String attSet = UIUtils.getAccessStructure(resourcePath, "(MarketingA & ResearchA)", email);
                            
                            com.ast.cloudABE.userManagement.User invitedUser = cabe.newABEUserInvited(attSet);
                            com.ast.cloudABE.accessTree.AccessTree accessTree = invitedUser.getSecretKey().getAccess_tree();

                            AccessTree adaptedTree = new AccessTree(AccessTreeConverter.transformTreeFromABEClientToCommons(accessTree));
                            KPABESecretKey secretKey = new KPABESecretKey(invitedUser.getSecretKey().getLeaf_keys(),adaptedTree);
            
                            emailsKeys.put(email,secretKey);
                            
                            System.out.println("[" + email + "] Setting up access logical expression to: " + attSet);
                            
                        }catch (Exception ex) {
                            logger.error(ex);
                        }
                         
                        
                    }
                }

                Config config = Config.getInstance();
                Profile profile = config.getProfile();
                Server server = profile.getServer();

                DatabaseHelper db = DatabaseHelper.getInstance();
                CloneFile sharedFolder = db.getFileOrFolder(panel.getFolderSelected());
                
                if(panel.isAbeEncrypted()){
                        try {
                            /*FIXME! Be careful, emails and keys are sent in plain text without encryption, 
                                    key distribution problem should be solved in order to guarantee security and privacy */
                            server.createShareProposal(profile.getAccountId(), emailsKeys, sharedFolder.getId(), false, panel.isAbeEncrypted());
                        } catch (ShareProposalNotCreatedException ex) {
                            ErrorMessage.showMessage(panel, "Error", "An error ocurred, please try again later.\nVerify email accounts.");
                        } catch (UserNotFoundException ex) {
                            ErrorMessage.showMessage(panel, "Error", "An error ocurred, please try again later.");
                        }
                }else {
                    {
                        try {
                            server.createShareProposal(profile.getAccountId(), panel.getEmails(), sharedFolder.getId(), false, panel.isAbeEncrypted());
                        } catch (ShareProposalNotCreatedException ex) {
                            ErrorMessage.showMessage(panel, "Error", "An error ocurred, please try again later.\nVerify email accounts.");
                        } catch (UserNotFoundException ex) {
                            ErrorMessage.showMessage(panel, "Error", "An error ocurred, please try again later.");
                        }
                    }
                }

                break;
            case QUIT:
                this.controller.doShutdownTray();
                break;

            default:
                //checkthis
                logger.warn("Unknown tray event type: " + event);
            // Fressen.
        }
    }

    private JFrame createSharingFrame() {

        Config config = Config.getInstance();
        ResourceBundle resourceBundle = config.getResourceBundle();

        String title = resourceBundle.getString("share_panel_title");
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(new ImageIcon(config.getResDir() + File.separator + "logo48.png").getImage());

        return frame;
    }
}
