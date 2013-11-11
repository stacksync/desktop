#/bin/sh
if [ $(id -u) != "0" ]; then
    echo "$0 must be run as root"
    exit 2
fi

if [ "$#" != 1 ] || ! [ -f $1 ]; then 
    echo "Please pass an existing file eg. $0 PATH/file"
    exit 1
fi

echo "Installing Nautilus overlay icons..."

EXT2=/usr/lib/nautilus/extensions-2.0
EXT3=/usr/lib/nautilus/extensions-3.0
LIB_PATH=$1
LIB_NAME=$(basename $LIB_PATH)

if [ -d $EXT2 ]; then
    echo "Nautilus extension 2.0 found"
    
    cp $LIB_PATH $EXT2
    chown root:root $EXT2/$LIB_NAME
    chmod 644 $EXT2/$LIB_NAME

    
    if [ -d $EXT3 ]; then
        ln -s $EXT2/$LIB_NAME $EXT3/$LIB_NAME
        echo "Nautilus extension 3.0 found, symbolic link created"
    fi

    killall nautilus 2> /dev/null
    
    echo "Library successfully installed"
elif [ -d $EXT3 ]; then
    echo "Nautilus extension 3.0 found"
    
    cp $LIB_PATH $EXT3
    chown root:root $EXT3/$LIB_NAME
    chmod 644 $EXT3/$LIB_NAME

    killall nautilus 2> /dev/null
    
    echo "Library successfully installed"
else
    echo "Nautilus extension not found"
fi


