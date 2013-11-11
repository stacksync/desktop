Distrubuted TEST

What do we need to execute this test?

2 or more computers that accept ssh connection, you need the computers.txt file with the username, password and ip for all the computers that you want to use for this test.
The format of the computers.txt is the following:

Path to stacksync Folder
ip,username,password
ip,username,password
ip,username,password
ip,username,password

We need path to stacksync folder cause we dont know where you have placed it in your local computer. Also, we need you to put in each distrubuted computer the stacksync Folder at the same path that is given to the ssh conection for example an ssh connection to a client will probably set the working path to /home/username.

Min. Req:
JRE installed
You need junit 3 or higher library installed.
Last working stacksync client copy.

To execute:

Run the PersistentTest.java File

cases when the test will shutdown:

Computers.txt file is corrupted
Stacksync is not running



