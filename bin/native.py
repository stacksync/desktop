#!/usr/bin/python
#
# Syncany Linux Native Functions
# Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

################################################################################
#                                                                              #
# This script is used by the main application to use native GTK methods        #
# and objects. In particular, it is used for the application indicator         #
# (or the status icon) and a native browse dialog.                             #
#                                                                              #
# It opens a server at a random port and passes this port to the client via    #
# STDOUT. It then reads required config data from STDIN and starts the server. #
#                                                                              #
# Requests are sent by the client in form of JSON objects. Responses are one   #
# line string objects (arbitrarily formed).                                    #
#                                                                              #
# Since Java does not terminate its child processes when it dies, this script  #
# contains two mechanisms to kill itself if it detects that the parent process #
# died. It dies if the STDOUT socket breaks, or if no request arrives within   #
# CONNECT_TIMEOUT seconds.                                                     #
#                                                                              #
# Author: Philipp Heckel <philipp.heckel@gmail.com>                            #
#                                                                              #
################################################################################

import os
import sys
import time
import gtk
import pynotify
import socket
import threading
import SocketServer
import json
import Queue
import subprocess


# This script kills itself after X seconds if no request arrives
# Make sure that this is higher than the NOP_INTERVAL in LinuxNativeClient.java
CONNECT_TIMEOUT = 15

sync_activated = True
    
class RequestHandler(SocketServer.StreamRequestHandler):		
	def handle(self):
		global last_request 
		do_print("Client connected.")
		
		try:
			request_str = self.rfile.readline().strip()
			request = json.loads(request_str)

			last_request = time.time()
			do_print("Received request: " + request_str)				

			if request["request"] == "BrowseFileRequest":
				response = do_browse_file(request)
		
			elif request["request"] == "ListenForTrayEventRequest":
				response = do_listen_for_event(request)
			
			elif request["request"] == "NopRequest":
				response = do_nop(request)
			
			elif request["request"] == "NotifyRequest":
				response = do_notify(request)
			
			elif request["request"] == "UpdateMenuRequest":
				response = do_update_menu(request)
			
			elif request["request"] == "UpdateStatusIconRequest":
				response = do_update_icon(request)
			
			elif request["request"] == "UpdateStatusTextRequest":
				response = do_update_text(request)
			
			else:
				response = "UNKNOWN_REQUEST"

#			gtk.gdk.threads_leave()

		except ValueError:
			response = "INVALID_REQUEST"	
	
		except:
			response = "REQUEST_ERROR"	
			do_print("Unexpected error: {0}".format(sys.exc_info()[0]))
#			raise	

		do_print("Sending response: "+response)
		try:
			self.wfile.write(response+"\n")		
			self.wfile.flush()
		
			self.request.close()
		except:
			# Do nothing.
			dummy = 1
	
def get_last_request():
	global last_request
	return last_request	

def do_browse_file(request):
	do_print("Opening Browse Window ...")
	
	if request["type"] == "FILES_ONLY":
		action = gtk.FILE_CHOOSER_ACTION_OPEN
		title = "Select file"
		
	else:
		action = gtk.FILE_CHOOSER_ACTION_SELECT_FOLDER
		title = "Select folder"
				
	# Init chooser and show it
	dialog = gtk.FileChooserDialog(title=title, parent=None, action=action, 
		buttons=(gtk.STOCK_CANCEL, gtk.RESPONSE_CANCEL, gtk.STOCK_OPEN, gtk.RESPONSE_OK))

	dialog.set_default_response(gtk.RESPONSE_OK)

	dlgfilter = gtk.FileFilter()
	dlgfilter.set_name("All files")
	dlgfilter.add_pattern("*")
	dialog.add_filter(dlgfilter)

	gtk.gdk.threads_enter()
	response = dialog.run()
	gtk.gdk.threads_leave()
	
	if response == gtk.RESPONSE_OK:
		filename = dialog.get_filename()
		
	else: 
		filename = ""
				
	dialog.destroy()
	return filename
	
def do_listen_for_event(request):
	global event_queue	
	
	do_print("Listening for tray event...")

	event = event_queue.get()	
	event_queue.task_done()
		
	do_print("Event '" + event + "' occurred. Passing on to client.")
	return event
	
def do_nop(request):
	do_print("Doing nothing. That's what I do best :-)")
	return "OK"
	
def do_notify(request):
	global resdir
	
	do_print("Creating notification...")

	if request["image"] == "":
		image = resdir + "/logo48.png"
	else:
		image = request["image"]

	
	# Alterantive using 'notify-send'
	# os.system("notify-send -t 2000 -i '{0}' '{1}' '{2}'".format(image, request["summary"], request["body"]))

	gtk.gdk.threads_enter()
	pynotify.init("Stacksync")
	notification = pynotify.Notification(request["summary"], request["body"], image)
	notification.show()
	gtk.gdk.threads_leave()

	return "OK"		
	
def do_update_icon(request):
	global indicator, status_icon, updating_count, resdir
	
	do_print("Update icon: count= {0}, resdir={1} ".format(updating_count, resdir))		
	
	if request["status"] == "DISCONNECTED":
		do_print("Update icon to DISCONNECTED.")
		
		updating_count = 0
		image = resdir + "/tray/tray.png"
		
		if indicator is not None:
			indicator.set_icon(image)		
		else:
			status_icon.set_from_file(image)
			
		return "OK"	
	
	if request["status"] == "UPDATING":
		updating_count += 1
		
		if updating_count == 1:
			do_print("Update icon to UPDATING.")
			image = resdir + "/tray/tray-updating1.png"
			
			if indicator is not None:
				indicator.set_icon(image)		
			else:
				status_icon.set_from_file(image)

	elif request["status"] == "UPTODATE":
		updating_count -= 1
		
		if updating_count < 0:
			updating_count = 0
		
		if updating_count == 0:
			do_print("Update icon to UPTODATE.")		
			image = resdir + "/tray/tray-uptodate.png"

			if indicator is not None:
				indicator.set_icon(image)		
			else:
				status_icon.set_from_file(image)
			
	return "OK"	
	
def do_update_text(request):
	global menu_item_status
	
	gtk.gdk.threads_enter()
	
	label = menu_item_status.get_child()
	label.set_text(request["status"])
	
	menu_item_status.show()
	
	gtk.gdk.threads_leave()
	
	return "OK"			
	
def do_update_menu(request):
	global menu, menu_item_status, sync_activated
	global status_text

	gtk.gdk.threads_enter()

	# Remove all children
	for child in menu.get_children():
		menu.remove(child)		

	'''Status'''
	menu_item_status.child.set_text(status_text)
	menu_item_status.set_can_default(0);	
	menu_item_status.set_sensitive(0);

	menu.append(menu_item_status)

	'''---'''
	menu.append(gtk.SeparatorMenuItem())	

	'''Profiles'''
	if request is not None:
		profiles = request["profiles"]
		
		'''Only one profile: just list the folders'''
		if len(profiles) == 1:
			for folder in profiles[0]["folders"]:				
				menu_item_folder = gtk.MenuItem(os.path.basename(folder["folder"]))
				menu_item_folder.connect("activate", menu_item_folder_clicked, folder["folder"])
	
				menu.append(menu_item_folder)					
		
		elif len(profiles) > 1:
			for profile in profiles:
				submenu_folders = gtk.Menu()

				menu_item_profile = gtk.MenuItem(os.path.basename(profile["name"]))
				menu_item_profile.set_submenu(submenu_folders)			
				
				for folder in profile["folders"]:				
					menu_item_folder = gtk.MenuItem(os.path.basename(folder["folder"]))
					menu_item_folder.connect("activate", menu_item_folder_clicked, folder["folder"])
	
					submenu_folders.append(menu_item_folder)
				
				menu.append(menu_item_profile)				
		
		if len(profiles) > 0:
			'''---'''
			menu.append(gtk.SeparatorMenuItem())	
	
	'''Preferences'''
	#menu_item_prefs = gtk.MenuItem("Preferencias")
	#menu_item_prefs.connect("activate", menu_item_clicked, "PREFERENCES")
	
	#menu.append(menu_item_prefs)
	
	'''---'''
	#menu.append(gtk.SeparatorMenuItem())	
	
	'''Donate ...'''
	#menu_item_donate = gtk.MenuItem("Donate1 ...")
	#menu_item_donate.connect("activate", menu_item_clicked, "DONATE")
	
	#menu.append(menu_item_donate)
 
	if sync_activated:
		'''Pause sync'''
		menu_item_pause = gtk.MenuItem("Pause Syncing")
		menu_item_pause.connect("activate", menu_item_clicked_sync, "PAUSE_SYNC")

		menu.append(menu_item_pause)
	else:
		'''Resume sync'''
		menu_item_pause = gtk.MenuItem("Resume Syncing")
		menu_item_pause.connect("activate", menu_item_clicked_sync, "RESUME_SYNC")

		menu.append(menu_item_pause)

	'''---'''
	menu.append(gtk.SeparatorMenuItem())

	'''Website'''
	menu_item_website = gtk.MenuItem("Go to StackSync website")
	menu_item_website.connect("activate", menu_item_clicked, "WEBSITE")
	
	menu.append(menu_item_website)


	'''Website'''
	menu_item_website2 = gtk.MenuItem("Go to AST website")
	menu_item_website2.connect("activate", menu_item_clicked, "WEBSITE2")
	
	menu.append(menu_item_website2)

	
	'''---'''
	menu.append(gtk.SeparatorMenuItem())	

	'''Quit'''
	menu_item_quit = gtk.MenuItem("Quit StackSync")
	menu_item_quit.connect("activate", menu_item_clicked, "QUIT")
	
	menu.append(menu_item_quit)	
	
	'''Set as menu for indicator'''
	if indicator is not None:
		indicator.set_menu(menu)

	'''Show'''
	menu.show_all()
	gtk.gdk.threads_leave()
	
	return "OK"

def init_menu():
	do_update_menu(None)

def init_tray_icon():
	global resdir, indicator, status_icon

	# Default image
	image = resdir + "/tray/tray.png"	
			
	# Display manager detection
	display_manager = get_display_manager()
	use_indicator = display_manager == "unity" or display_manager == "gnome2"
	
	do_print("Detected display manager '{0}'.".format(display_manager))
	
	# Try loading "appindicator" package; if not existant, 
	# fall back to status icons
	if use_indicator:
		try:
			import appindicator
		except:
			do_print("Couldn't load 'appindicator' package. Using status icons.")
			use_indicator = 0
	
	# Go!
	if use_indicator:
		do_print("Initializing indicator...")
		
		indicator = appindicator.Indicator("Stacksync", image, appindicator.CATEGORY_APPLICATION_STATUS)
		indicator.set_status(appindicator.STATUS_ACTIVE)
		indicator.set_attention_icon("indicator-messages-new")	
	
	else:
		do_print("Initializing status icon...")

		status_icon = gtk.StatusIcon()
		status_icon.set_from_file(image)
		status_icon.connect('popup-menu', status_icon_popup_menu_cb) 
		status_icon.set_visible(True)
		status_icon.set_tooltip("Stacksync")

def status_icon_popup_menu_cb(status_icon, button, time):
	global menu	
	menu.show_all()
	menu.popup(None, None, gtk.status_icon_position_menu, button, time, status_icon) 	
	
def menu_item_clicked(widget, cmd):
	do_print("Menu item '" + cmd + "' clicked.")
	event_queue.put(cmd)
	
def menu_item_clicked_sync(widget, cmd):
	global sync_activated
	do_print("Menu item '" + cmd + "' clicked.")
	if sync_activated:
		sync_activated = False
	else:
		sync_activated = True
	event_queue.put(cmd)

def menu_item_folder_clicked(widget, folder):
	do_print("Folder item '" + folder + "' clicked.")
	event_queue.put("OPEN_FOLDER	{0}".format(folder))
#	os.system('xdg-open "%s"' % foldername)

def do_exec(cmd):
	ps = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
	output = ps.stdout.read()
	ps.stdout.close()
	ps.wait()
	
	return output


def get_display_manager():
	# Unity
	if do_exec("ps --no-headers -C unity-panel-service").strip():
		return "unity"
	
	# Gnome 2/3
	if do_exec("ps --no-headers -C gnome-session").strip():
		gnome_session_output = do_exec("gnome-session --version 2> /dev/null")
	
		if gnome_session_output.startswith("gnome-session 2"):
			return "gnome2"
		
		if gnome_session_output.startswith("gnome-session 3"):
			return "gnome3"
		
	# TODO KDE, Xfce, ...
	
	# Others
	return "other"

def do_kill_loop():
	global last_request, CONNECT_TIMEOUT
	global server, terminated
	
	while not terminated:
		do_print("time {0} - last req {1} > timeout {2}".format(time.time(), last_request, CONNECT_TIMEOUT))
		
		if time.time() - last_request > CONNECT_TIMEOUT:
			do_print("Socket timeout occurred. Java client died? EXITING.")					
			do_kill()			
			return
							
		time.sleep(5)

def do_kill():
	# Note: this method cannot contain any do_print() calls since it is called
	#       by do_print if the STDOUT socket breaks!
	
	pid = os.getpid()
	os.system("kill -9 {0}".format(pid))
		
	#	global server, terminated
	#	terminated = 1
	#	server.shutdown()
	#	server_thread.interrupt_main()
	#	gtk.main_quit()
	#	kill_thread.interrupt_main()
	#	sys.exit()
	
def do_print(msg):
	try:
		sys.stdout.write("{0}\n".format(msg))
		sys.stdout.flush()
	except:
		# An IOError happens when the calling process is killed unexpectedly		
		do_kill()		
	
#	try:
#		sys.stderr.write("{0}\n".format(msg))
#		sys.stderr.flush()
#	except:
#		dummy = 1

def main():
	global CONNECT_TIMEOUT
	global last_request
	global server
	
	try:
		server = SocketServer.ThreadingTCPServer(("localhost", 0), RequestHandler)
		server.timeout = CONNECT_TIMEOUT
		server.socket.settimeout(CONNECT_TIMEOUT)
		
		'''Tell the Java client the port.'''
		ip, port = server.server_address
		do_print("PORT={0}".format(port))
					
		'''Init application and menu'''
		init_tray_icon()
		init_menu()	
		
		'''Wait forever ...'''
		do_print("Now listening for requests ...")

		# Note: Do not try "multiprocessing". It doesn't work with 
		#       global variables

		server_thread = threading.Thread(target=server.serve_forever)
		server_thread.setDaemon(True)
		server_thread.start()
		
		kill_thread = threading.Thread(target=do_kill_loop)
		kill_thread.setDaemon(True)
		kill_thread.start()			
		
		gtk.gdk.threads_init()
		gtk.gdk.threads_enter()		
		gtk.main()
		#gtk.gdk.threads_leave()
			
	except:
		do_print("FATAL EXCEPTION: {0}".format(sys.exc_info()[0]))
		do_kill()

	do_kill()

if __name__ == "__main__":
	# Parse command line
	if len(sys.argv) != 3:
		do_print("Syntax: {0} RESOURCE_DIR INTIAL_STATUS_TEXT".format(sys.argv[0]))
		sys.exit()		

	# Global variables
	resdir = sys.argv[1]
	status_text = sys.argv[2]
	
	updating_count = 0
	indicator = None
	status_icon = None
	last_request = time.time()
	event_queue = Queue.Queue()
	terminated = 0
	server = None
	server_thread = None
	kill_thread = None
		
	# Default values
	menu = gtk.Menu()	
	menu_item_status = gtk.MenuItem(status_text)

	# Go!
	main()
