Welcome to StackSync!
=====================

> **NOTE:** This is BETA quality code!

**Table of Contents**

- [Introduction](#introduction)
- [Architecture](#architecture)
- [Desktop](#desktop-client)
- [Attribute-based Encryption] (#attribute-based-encryption)
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
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/resources/res/stacksync-architecture.png">
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
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/resources/res/win_integration.png">
</p>
* **Push notifications**: ObjectMQ provides push notification to desktop clients.
* **Data deduplication**: We deduplicate data across a single user in order to optimize bandwidth and storage.

# Attribute-based Encryption
In the context of a scalable Personal Cloud like that of StackSync, our approach for privacy-aware data sharing involves the design of a cryptographic component ready to be adapted to an existing architecture and able to work efficiently as an extension to our software. Data privacy in the Personal Cloud can easily get compromised, because clients typically delegate tasks such as protection and honest use of the files to remote storage servers that are out of their control. But, what if their information is sensitive enough not to take the risk of trusting a third party?

For this aim, we will implement the KP-ABE protocol into StackSync. KP-ABE is a public key cryptography specially designed for data-sharing environments. By using its encryption technique, any data encrypted gets associated with a set of attributes. On the other hand, each user is provided with an access structure composed of a logical definition of attributes. The secret key of any user reflects the access structure in such a way that a user will be able to access certain content if and only if the data attributes satisfy his access structure. 

In KP-ABE, each user must hold an access structure defining its privileges, which is a Boolean expression over attributes. This logical expression is often represented as a logical tree, where the leafs are attributes and the interior nodes are threshold gates. For instance, the data owner could choose to assign Alice an access structure. The data owner can define her access privileges as ("C" AND ("A" OR "B")), where "A", "B" and "C" are attributes (e.g. Accounting, Budgeting, Computing). The data owner would then generate and distribute to Alice her new secret key (USK) next implicitly those three attributes. The corresponding access tree would be represented as shown in the following image. Similarly, the data owner can also define an access structure to Bob as ("A" AND ("B" OR "C")) and provide him with his USK accordingly. From now on, Alice and Bob should be able to decrypt files according to their access policy. 
<p align="center">
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/resources/res/abe_example.png">
</p>

On the other hand, KP-ABE encrypted files must specify a set of attributes in order to define in which context will be shared. For instance, the data owner can encrypt and upload a file with the attributes "A" and "B", as we can see in the image. After Alice and Bob download the file, Alice cannot see the underlying plaintext, while Bob is able to correctly decrypt it. The KP-ABE construction ensures that only users with the proper access structure will be able to decrypt data encrypted under a certain set of attributes. In this case, Bob satisfies the condition ("A" AND "B"), where "A" and "B" are the attributes used in the encryption of the downloaded file. Alice will nevertheless not be able to decrypt the content since her access structure requires files to be encrypted under the attribute "C", in addition to "A" or "B". 

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
