Welcome to StackSync!
=====================

> **NOTE:** This is BETA quality code!

**Table of Contents**

- [Introduction](#introduction)
- [Architecture](#architecture)
- [Desktop](#desktop-client)
- [Requirements](#requirements)
- [Build, installation and execution](#build-installation-and-execution)
  - [Linux](#linux)
  - [Windows](#windows)
  - [Mac OS](#mac-os)
  - [General execution](#general-execution)
- [Issue Tracking](#issue-tracking)
- [Licensing](#licensing)
- [Contact](#contact)

# Introduction

StackSync (<http://stacksync.com>) is an open source Personal Cloud
that implements the basic components to create a synchronization tool.


# Architecture

In general terms, StackSync can be divided into three main blocks: clients
(desktop and mobile), synchronization service (SyncService) and storage
service (OpenStack Swift). An overview of the architecture
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

# Desktop client

This repository contains desktop client code. StackSync client is a branch of 
the Syncany project developed by Philipp Heckel (http://www.syncany.org/).

StackSync is an application that monitors local folders and synchronizes them
with a remote repository. It interacts with both the synchronization and storage
services. The first one handles metadata communication, such as time stamps or
file names, and the second one is in charge of raw data.

Some of the main features of the client are:
* **Sharing**: Share folders with other StackSync users.
* **Client side encryption**: Upload your files encrypted.
* **Desktop integration**: We are using [Liferay nativity library](https://github.com/liferay/liferay-nativity).

<p align="center">
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/res/win_integration.png">
</p>
* **Push notifications**: ObjectMQ provides push notification to desktop clients.
* **Data deduplication**: We deduplicate data across a single user in order to optimize bandwidth and storage.

# Requirements
* Java 1.7
* Maven 2 (build)
* A StackSync server installation running. Intallation instruction can be found [here](https://github.com/stacksync/sync-service)
* A user initilialized in StackSync server.

# Build, installation and execution

You just need to assemble the project into a JAR using Maven:

    $ mvn assembly:assembly

This will generate a "target" folder containing a JAR file called "desktop-client-X.X-jar-with-dependencies.jar"

> **NOTE**: if you get an error (BUILD FAILURE), cleaning your local Maven repository may fix the problem.

    $ rm -rf ~/.m2/repository/*
    
## Linux
Under the folder [packaging/debian](packaging/debian) there is the Makefile to create the
deb file.

    $ cd packaging/debian
    $ make compile
    $ make package

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

# Issue Tracking
For the moment, we are going to use the github issue tracking.

# Licensing
StackSync is licensed under the GPLv3. Check [license.txt](license.txt) for the latest
licensing information.

# Contact
Visit www.stacksync.com to contact information.
