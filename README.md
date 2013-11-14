Welcome to StackSync!
=====================

> **NOTE:** This is BETA quality code!

**Table of Contents**

- [Introduction](#introduction)
- [Architecture](#architecture)
- [Desktop](#desktop-client)
  - [Which are the differences with Syncany?](#which-are-the-differences-with-syncany)
- [Requirements](#requirements)
- [Build, installation and execution](#build-installation-and-execution)
  - [Linux](#linux)
  - [Windows](#windows)
  - [Mac OS](#mac-os)
  - [General execution](#general-execution)
- [Things you need to know](#things-you-need-to-know)
- [Issue Tracking](#issue-tracking)
- [Licensing](#licensing)
- [Contact](#contact)

# Introduction

StackSync (<http://stacksync.com>) is a scalable open source Personal Cloud
that implements the basic components to create a synchronization tool.


# Architecture

In general terms, StackSync can be divided into three main blocks: clients
(desktop and mobile), synchronization service (SyncService) and storage
service (Swift, Amazon S3, FTP...). An overview of the architecture
with the main components and their interaction is shown in the following image.

<p align="center">
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/res/stacksync-architecture.png">
</p>

The StackSync client and the SyncService interact through the communication
middleware called ObjectMQ. The sync service interacts with the metadata
database. The StackSync client directly interacts with the storage back-end
to upload and download files.

As storage back-end we are using OpenStack Swift, an open source cloud storage
software where you can store and retrieve lots of data in virtual containers.
It's based on the Cloud Files offering from Rackspace. But it is also possible
to use other storage back-ends, such as a FTP server or S3.

# Desktop client

This repository contains desktop client code. StackSync client is a branch of 
the Syncany project developed by Philipp Heckel (http://www.syncany.org/).

StackSync is an application that monitors local folders and synchronizes them
with a remote repository. It interacts with both the synchronization and storage
services. The first one handles metadata communication, such as time stamps or
file names, and the second one is in charge of raw data.

Some of the main features of the client are:
* Client based encryption.
* Optimize the storage usage.
* Desktop integration (Linux (debian distributions) and Windows 7)
* Push notifications to save server bandwidth.

## Which are the differences with Syncany?
### Pull vs Push
The original Syncany uses a pulling strategy to discover changes in the repository.
We use ObjectMQ to provide push notifications to all the clients. This change
implies to remove all the "update" file logic, which was a bit inneficient.

### Synchronization algorithm
We have simplified the sync algorithm removing the merged files logic.

### Desktop integration
We have implemented some dll's to create Windows overlays (link needed). For
this project we used [Liferay nativity library](https://github.com/liferay/liferay-nativity).

<p align="center">
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/res/win_integration.png">
</p>

# Requirements
StackSync is developed using NetBeans and Java 1.7.

The client requires a SyncService running with the user metadata initialized. In the
SyncService repository (link needed) it is possible to read the installation
instrucctions and how to add users to the system.

StackSync sends error logs to a log server in order to control clients errors and 
try to fix them as soon as possible. As it can be read in the log-server repository (link needed),
it uses a REST API. To configure the URL in the client, it is necessary to change
"URL_LOG_SERVER_API" from [config-default.xml](src/com/stacksync/desktop/config/config-default.xml)
for your log-server URL.

Now there isn't an easy way to enable or disable this feature, but in a future
version we will fix it.

# Build, installation and execution
To build StackSync it is necessary Apache Ant. Build.xml gets the SVN version to use
in the manifest file. This is usefull for the updater, but since we are not working
with svn, it has no sense and it has to be changed.

When you build the project, the folder "dist" is created with the executable jar and
all the dependencies.

## Linux
In the dist folder there are two scripts
- **install.sh**: Creates a desktop shortcut, install linux overlays and autoexecute 
StackSync when the SO starts.
- **uninstall.sh**: It undo the install.sh actions.

## Windows
We have a NSIS script that creates an installer ([here](installers/StackSyncInstallScript.nsi)).
Of course, it requires NSIS to be installed in your system. build.xml can be
configured to create the installer automatically.

The installer will create a folder in ~\AppData\Roaming\stacksync_client with the
jars and the dependencies. It will also add a shortcut, install the overlays and
make StackSync autoexecutable when Windows starts.

**NOTE:** You have to take into account that the default installer script will
execute the Updater.jar instead of Stacksync.jar. This is because the updater will
check for new updates and then it will launch StackSync. If you don't want to use
the updater, you have to change the script.

**NOTE 2:** Our overlays only work fine on Windows 7. We have to fix them in Win XP
and 8.

## Mac OS
StackSync is fully functional on Mac OS, but it lacks desktop integration (no overlays
and notifications aren't beautiful enough)

We don't have a dmg file, but we are working on it.

## General execution
If you don't want to install StackSync in your computer, you can directly execute
Stacksync.jar:

    $ java -jar Stacksync.jar
  
You can add some parameters:
- **Daemon mode (-d)**: Runs StackSync with desktop integration.
- **Configuration file (-c)**: Load a config.xml file located at the given path.
- **Extended mode (-ext)**: Runs StackSync wizard and tray with more functionalities
(SyncService panel, encryp panel, ...).
- **Help (-h)**: Shows help menu.

# Things you need to know
We are mainly using StackSync with OpenStack Swift. For the data communication we are
using the Rackspace plugin. As you could see there are 2 different panels:
- **Commercial**: This is a user friendly panel only with user and password. To
    set the auth URL it is necessary to change the value of the variable AUTH_URL
    in the com.stacksync.desktop.connection.plugins.rackspace.RackspaceConfigPanelFactory.java
    from:

        public static final String AUTH_URL = "AUTH_SERVER_URL";
        
    to:
    
        public static final String AUTH_URL = "http://your_auth_url:5000/v2.0/tokens";
    for example.
    
- **Dev**: This panel is fully configurable.
    
In addition, users in Swift have a tenant, but with our current implementation
the tenant name and the user name must be the same. We know that this is not
correct but for some reasons, long time ago, we decided to do in this way, and
for other reasons we haven't got enough time to change it...

If you create in Swift the user (testuser:testuser) the field username in the
rackspace panel must be testuser only:

<p align="center">
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/res/wizard_credentials.png">
</p>

**IMPORTANT: The container must be allways stacksync.**

# Issue Tracking
For the moment, we are going to use the github issue tracking.

# Licensing
StackSync is licensed under the GPLv3. Check [license.txt](license.txt) for the latest
licensing information.

# Contact
Visit www.stacksync.com to contact information.
