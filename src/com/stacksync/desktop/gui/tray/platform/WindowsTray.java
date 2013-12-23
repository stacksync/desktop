package com.stacksync.desktop.gui.tray.platform;

import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.InitializationException;
import com.stacksync.desktop.gui.linux.UpdateStatusTextRequest;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.gui.tray.TrayEvent;
import com.stacksync.desktop.gui.tray.TrayEventListener;
import com.stacksync.desktop.gui.tray.TrayIconStatus;
import com.stacksync.desktop.util.WinRegistry;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ResourceBundle;
import javax.swing.*;

public class WindowsTray extends Tray {
    
    private final ResourceBundle resourceBundle = Config.getInstance().getResourceBundle();
    private SystemTray tray;
    private PopupMenu menu;
    private TrayIcon icon;
    private MenuItem itemStatus, itemFolder, itemWebsite, itemWebsite2, itemQuit, itemSync;
    private TrayIconStatus status;
    private boolean syncActivated;
    
    public WindowsTray() {
        super();

        syncActivated = true;
        // cp. init
        this.menu = null;
        this.status = new TrayIconStatus(new TrayIconStatus.TrayIconStatusListener() {
            @Override
            public void trayIconUpdated(String filename) {
                if (config != null) {
                    setIcon(new File(config.getResDir()+File.separator+
                            Constants.TRAY_DIRNAME+File.separator+filename));
                }
            }
        });
    }
    
    @Override
    public void notify(String summary, String body, File imageFile) {  
        icon.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO change to open the folder where changes happend
                Profile profile = config.getProfiles().list().get(0);
                Folder folder = profile.getFolders().list().get(0);
                fireTrayEvent(new TrayEvent(TrayEvent.EventType.OPEN_FOLDER, folder.getLocalFile().getAbsolutePath()));
            }
        });
        
        icon.displayMessage(summary, body, TrayIcon.MessageType.INFO);
    }

    @Override
    public void updateUI() {
        addItemFolder();
    }

    @Override
    public void updateStatusText() {
                
        UpdateStatusTextRequest menuRequest = new UpdateStatusTextRequest(processesText);
        synchronized (itemStatus) {
            itemStatus.setLabel(menuRequest.getStatusText());
        }
    }

    @Override
    public void setStatusIconPlatform(Tray.StatusIcon s) {
        status.setIcon(s);
    }
    
    private void setIcon(File file) {
        if(icon != null){
            icon.setImage(Toolkit.getDefaultToolkit().getImage(file.getAbsolutePath()));
        } else{
            logger.debug(config.getUserName()+"#Cannot update status. Tray not initialized yet."); 
        }
    }    

    @Override
    public void init(String initialMessage) throws InitializationException {
        initMenu(initialMessage);
        initIcon();
    }

    @Override
    public void destroy() {
        // Nothing.
    }
    
    private void initMenu(String initialMessage) {
        // Create
        menu = new PopupMenu();

        // Status
        itemStatus = new MenuItem(initialMessage);
        itemStatus.setEnabled(false);

        menu.add(itemStatus);
        menu.addSeparator();
        
        itemFolder = new MenuItem(resourceBundle.getString("tray_folder"));
        itemFolder.setEnabled(false);
        menu.add(itemFolder);
        menu.addSeparator();
                
        // Preferences
        //itemPreferences = new MenuItem("Preferencias");
        //itemPreferences.addActionListener(new ActionListener() {

        //    @Override
        //    public void actionPerformed(ActionEvent ae) {
        //        fireTrayEvent(new TrayEvent(TrayEvent.EventType.PREFERENCES));
        //    }
        //});

        //menu.add(itemPreferences);

        final TrayEvent.EventType eventType = TrayEvent.EventType.PAUSE_SYNC;
        itemSync = new MenuItem(resourceBundle.getString("tray_pause_sync"));
        itemSync.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                pauseOrResumeSync(new TrayEvent(eventType));
            }
        });
        menu.add(itemSync);
        
        menu.addSeparator();
        itemWebsite = new MenuItem(resourceBundle.getString("tray_stacksync_website"));
        itemWebsite.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                fireTrayEvent(new TrayEvent(TrayEvent.EventType.WEBSITE));
            }
        });
        menu.add(itemWebsite);
        
        itemWebsite2 = new MenuItem(resourceBundle.getString("tray_ast_website"));
        itemWebsite2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                fireTrayEvent(new TrayEvent(TrayEvent.EventType.WEBSITE2));
            }
        });
        menu.add(itemWebsite2);
               
        menu.addSeparator();

        // Quit
        itemQuit = new MenuItem(resourceBundle.getString("tray_exit"));
        itemQuit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                fireTrayEvent(new TrayEvent(TrayEvent.EventType.QUIT));
            }
        });

        menu.add(itemQuit);
    }
    
    private void addItemFolder() {
        
        try {
            Profile profile = config.getProfiles().list().get(0);
            for (final Folder folder : profile.getFolders().list()) {
                if (!folder.isActive() || folder.getLocalFile() == null) {
                    continue;
                }

                itemFolder.setLabel(folder.getLocalFile().getName());
                itemFolder.setEnabled(true);
                itemFolder.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        fireTrayEvent(new TrayEvent(TrayEvent.EventType.OPEN_FOLDER, folder.getLocalFile().getAbsolutePath()));
                    }
                });
            }
        } catch(Exception e){
            
        }
    }
    
    private void initIcon() throws InitializationException {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            throw new InitializationException("Unable to set look and feel for tray icon", e);
        }

        tray = SystemTray.getSystemTray();        
        File defaultIconFile = new File(config.getResDir()+File.separator+
                Constants.TRAY_DIRNAME+File.separator+Constants.TRAY_FILENAME_DEFAULT);

        Image image = Toolkit.getDefaultToolkit().getImage(defaultIconFile.getAbsolutePath());

        icon = new TrayIcon(image, "Stacksync", menu);
        icon.setImageAutoSize(true);
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Profile profile = config.getProfiles().list().get(0);
                    Folder folder = profile.getFolders().list().get(0);
                    fireTrayEvent(new TrayEvent(TrayEvent.EventType.OPEN_FOLDER, folder.getLocalFile().getAbsolutePath()));
                }
            }
        });

        try {
            tray.add(icon);
        } catch (AWTException e) {
            throw new InitializationException("Unable to add tray icon.", e);
        }
    }
    
    
    public static void main(String[] args) throws ConfigException, InitializationException, InterruptedException {
        System.out.println("STARTED");

        config.load();
        Tray tray = Tray.getInstance();
        tray.registerProcess(tray.getClass().getSimpleName());
        tray.init("Everything is up to date.");

        File imageFile = new File(config.getResDir() + File.separator + "logo48.png");
        tray.notify("hello asdas dasd dasd asd ", "test asdsad sd asd sa", imageFile);

        tray.setStatusText(tray.getClass().getName(), "testing!!!");
        
        tray.addTrayEventListener(new TrayEventListener() {

            @Override
            public void trayEventOccurred(TrayEvent event) {
                System.out.println(event);
            }
        });
        tray.setStatusIcon(tray.getClass().getSimpleName(), Tray.StatusIcon.UPDATING);
        //System.out.println(FileUtil.showBrowseDirectoryDialog());

        while(true){
            Thread.sleep(1000);
        }
	
    }
    
    private void pauseOrResumeSync(TrayEvent event) {
        
        String name;
        final TrayEvent.EventType newEvent;
        if (event.getType() == TrayEvent.EventType.PAUSE_SYNC) {
            name = resourceBundle.getString("tray_resume_sync");
            newEvent = TrayEvent.EventType.RESUME_SYNC;
            syncActivated = false;
            
        } else {
            name = resourceBundle.getString("tray_resume_sync");
            newEvent = TrayEvent.EventType.RESUME_SYNC;
            syncActivated = true;
        }
        try {
            WinRegistry.setOverlayActivity(syncActivated);
        } catch (Exception ex) {        }
        
        itemSync.setLabel(name);
        itemSync.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                pauseOrResumeSync(new TrayEvent(newEvent));
            }
        });
        
        fireTrayEvent(event);
    }
    
}
