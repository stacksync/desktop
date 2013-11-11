#!/bin/bash

if [ $(id -u) != "0" ]; then
    echo "$0 must be run as root"
    exit 1
fi

read -p "Are you sure you want to uninstall StackSync? [y/N] " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    exit 2
fi

echo "Uninstalling StackSync..."

path="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

USER_HOME=$(getent passwd $SUDO_USER | cut -d: -f6)
SHORTCUT_FILE=stacksync.desktop
DESKTOP_PATH=$USER_HOME/Desktop
USR_APPS_PATH=/usr/share/applications
AUTOSTART_PATH=$USER_HOME/.config/autostart

if [ -f "$USR_APPS_PATH/$SHORTCUT_FILE" ]; then 
    rm $USR_APPS_PATH/$SHORTCUT_FILE
    echo "$USR_APPS_PATH/$SHORTCUT_FILE removed"
fi

if [ -f "$AUTOSTART_PATH/$SHORTCUT_FILE" ]; then 
    rm $AUTOSTART_PATH/$SHORTCUT_FILE
    echo "$AUTOSTART_PATH/$SHORTCUT_FILE removed"
fi

if [ -f "$DESKTOP_PATH/$SHORTCUT_FILE" ]; then 
    rm $DESKTOP_PATH/$SHORTCUT_FILE
    echo "$DESKTOP_PATH/$SHORTCUT_FILE removed"
fi

if [ -d "$USER_HOME/.stacksync" ]; then 
    rm -r $USER_HOME/.stacksync
    echo "Account information removed"
fi
    
LIB_OVERLAY=libnautilus-syncany.so

if [ -f "$path/scripts/linux_uninstall_overlay.sh" ]; then 
    chmod +x $path/scripts/linux_uninstall_overlay.sh
    $path/scripts/linux_uninstall_overlay.sh $LIB_OVERLAY
fi

echo "StackSync has been uninstalled successfully"
