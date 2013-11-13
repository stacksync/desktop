Welcome to StackSync!
=====================

> **NOTE:** This is BETA quality code!

**Table of Contents**

- [Introduction](#introduction)
- [Architecture](#architecture)
- [Desktop](#desktop-client)
  - [Which are the differences with Syncany?](#which-are-the-differences-with-syncany)
- [Execution parameters](#execution-parameters)
- [Things you need to know](#things-you-need-to-know)
- [Issue Tracking](#issue-tracking)
- [Licensing](#licensing)
- [Contact](#contact)

# Introduction

StackSync (<http://stacksync.com>) is a scalable open source Personal Cloud
that implements the basic components to create a synchronization tool.


# Architecture

In general terms, StackSync can be divided into three main blocks: clients,
synchronization service and storage service. An overview of the architecture
with the main components and their interaction is shown in the following figure.

LINK BIG PICTURE

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
services. The first one handles metadata communication, such as time stamps or file
names, and the second one is in charge of raw data.

Some of the main features of the client are:
* Client based encryption.
* Optimize the storage usage.
* Desktop integration (Linux (debian distributions) and Windows 7)
* Push notifications to save server bandwidth.

## Which are the differences with Syncany?
### Pull vs Push
The original Syncany uses a pulling strategy to discover changes in the repository.
We use ObjectMQ to provide push notifications to all the clients. This change
implies to remove all the "update file" logic, which was a bit inneficient.

### Synchronization algorithm
We have simplified the sync algorithm removing the merged files logic.

### Desktop integration
We have implemented some dll's to create windows overlays (link a nuestro repo). For this project we
used Liferay nativity library (link liferay repo).

IMAGE WITH WINDOWS

# Requirements
StackSync is developed using NetBeans and Java 1.7.

The client requires a SyncService running with the user metadata initialized.
In the SyncService repository it is possible to read the install instrucctions
and how to add users to the system.

StackSync sends error logs to a log server in order to control clients
errors and try to fix them as soon as possible. As it can be read in the log-server
repository, it uses a REST API. To configure the URL in the client, it is necessary
to change "URL_LOG_SERVER_API" from com.stacksync.desktop.config.config-default.xml
for your own log-server.

Now there isn't an easy way to enable or disable this feature, but in a future
version we will try to fix it.

# Execution parameters
StackSync supports some arguments:
- **Daemon mode (-d)**: Runs StackSync with desktop integration.
- **Configuration file (-c)**: Load a config.xml file located at the given path.
- **Extended mode (-ext)**: Runs StackSync wizard and tray with more functionalities
(SyncService panel, encryp panel, ...).
- **Help (-h)**: Shows help menu.

# Things you need to know
We are mainly using StackSync with OpenStack Swift. For the data channel we are
using the Rackspace panel. As you could see there are 2 different panels:
- **Commercial**: This is a user friendly panel only with user and password. To
    set the auth URL it is necessary to change the value of the variable AUTH_URL
    in the com.stacksync.desktop.connection.plugins.rackspace.RackspaceConfigPanelFactory.java
    from:

        public static final String AUTH_URL = "AUTH_SERVER_URL";
        
    to:
    
        public static final String AUTH_URL = "http://auth_server:5000/v2.0/tokens";
    for example.
    
- **Dev**: This panel is fully configurable.
    
In addition, users in Swift have a tenant, but with our current implementation
the tenant name and the user name must be the same. We know that this is not
correct but for some reasons we decided to do in this way long time ago, and
for other reasons we haven't got enough time to change it...

If you create in Swift the user (testuser:testuser) the field username in the
rackspace panel must be testuser only.

<img width="500" src="https://raw.github.com/stacksync/desktop/master/res/wizard_credentials.png">
    
**IMPORTANT: The container must be allways stacksync.**
    

# Issue Tracking
For the moment, we are going to use the github issue tracking.

# Licensing
StackSync is licensed under the GPLv3. Check [license.txt](license.txt) for the latest
licensing information.

# Contact
Visit www.stacksync.com to contact information.
