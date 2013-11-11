#/bin/sh
if [ $(id -u) != "0" ]; then
    echo "$0 must be run as root"
    exit 2
fi

if [ "$#" != 1 ]; then 
    echo "Please pass the library name (e.g.: $0 libname.so"
    exit 1
fi

echo "Uninstalling overlay icons..." 

EXT2=/usr/lib/nautilus/extensions-2.0
EXT3=/usr/lib/nautilus/extensions-3.0
LIBNAME=$1

overlay_found=false

if [ -f "$EXT3/$LIBNAME" ]; then 
    rm $EXT3/$LIBNAME
    echo "Library deleted from Nautilus extension 3.0" 
    overlay_found=true
fi

if [ -f "$EXT2/$LIBNAME" ]; then 
    rm $EXT2/$LIBNAME
    echo "Library deleted from Nautilus extension 2.0"
    overlay_found=true
fi


if $overlay_found ; then
    echo "Closing all Nautilus instances..." 
    killall nautilus 2> /dev/null
    echo "Overlay icons successfully uninstalled" 
else
    echo "Overlay icons not found" 
fi

