#!/bin/bash

if [ $(id -u) != "0" ]; then
    echo "$0 must be run as root"
    exit 1
fi

echo "Installing StackSync..."

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
executeFileName=$path/launch_stacksync.sh
echo "#!/bin/bash" > $executeFileName
echo "cd $path" >> $executeFileName
echo "java -jar UpdaterClient.jar" >> $executeFileName

chmod +x $executeFileName
chown $SUDO_USER:$SUDO_USER $executeFileName

echo "StackSync launcher successfully created"

echo "Creating shortcuts..."

USER_HOME=$(getent passwd $SUDO_USER | cut -d: -f6)
SHORTCUT_FILE=stacksync.desktop
DESKTOP_PATH=$USER_HOME/Desktop
USR_APPS_PATH=/usr/share/applications
AUTOSTART_PATH=$USER_HOME/.config/autostart

echo "[Desktop Entry]" > $SHORTCUT_FILE
echo "Name=StackSync" >> $SHORTCUT_FILE
echo "GenericName=Personal Cloud" >> $SHORTCUT_FILE
echo "Comment=Keep your files securely synced between all your devices" >> $SHORTCUT_FILE
echo "Icon=$path/res/logo48.ico" >> $SHORTCUT_FILE
echo "Terminal=false" >> $SHORTCUT_FILE
echo "Type=Application" >> $SHORTCUT_FILE
echo "Categories=Network;Application;FileTransfer;" >> $SHORTCUT_FILE
echo "Exec=$path/launch_stacksync.sh" >> $SHORTCUT_FILE

chmod +x $SHORTCUT_FILE
chown $SUDO_USER:$SUDO_USER $SHORTCUT_FILE

cp $SHORTCUT_FILE $DESKTOP_PATH
cp $SHORTCUT_FILE $USR_APPS_PATH
cp $SHORTCUT_FILE $AUTOSTART_PATH

chown $SUDO_USER:$SUDO_USER $DESKTOP_PATH/$SHORTCUT_FILE
chown $SUDO_USER:$SUDO_USER $AUTOSTART_PATH/$SHORTCUT_FILE
rm $SHORTCUT_FILE

LIB_OVERLAY=$path/dlls/libnautilus-syncany.so

chmod +x $path/scripts/linux_install_overlay.sh
$path/scripts/linux_install_overlay.sh $LIB_OVERLAY

echo "StackSync has been installed successfully"
