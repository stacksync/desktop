package com.stacksync.desktop.gui.tray;

import com.stacksync.desktop.ApplicationController;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.gui.sharing.SharePanel;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.apache.log4j.Logger;

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
        frame.setIconImage(new ImageIcon(config.getResDir()+File.separator+"logo48.png").getImage());
        
        return frame;
    }
}