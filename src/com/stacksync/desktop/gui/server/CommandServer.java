/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.gui.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.Socket;
import java.util.*;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.SyncStatus;
import com.stacksync.desktop.Environment.OperatingSystem;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.exceptions.CommandException;
import com.stacksync.desktop.exceptions.CouldNotApplyUpdateException;
import com.stacksync.desktop.util.DateUtil;
import com.stacksync.desktop.util.FileUtil;
import com.stacksync.desktop.util.StringUtil;

/**
 *
 * @author Philipp C. Heckel
 */
public class CommandServer extends AbstractServer implements Runnable /* THIS MUST BE HERE. Otherwise the thread won't start! */ {

    private final Environment env = Environment.getInstance();
    private DatabaseHelper db = DatabaseHelper.getInstance();
    private Map<File, Date> queryCache;

    public CommandServer() {
        super(Constants.COMMANDSERVER_PORT);
        queryCache = new HashMap<File, Date>();
    }

    @Override
    protected AbstractWorker createWorker(Socket clientSocket) {
        if (env.getOperatingSystem() == OperatingSystem.Linux) {
            return new CommandWorkerLinux(clientSocket);
        } else if (env.getOperatingSystem() == OperatingSystem.Windows) {
            return new CommandWorkerWindows(clientSocket);
        } else {
            //FIXME: what do we do for unknown OS?
            return new CommandWorkerLinux(clientSocket);
        }
    }
    
    private class CommandWorkerWindows extends AbstractWorker {

        public CommandWorkerWindows(Socket clientSocket) {
            super(clientSocket);
        }

        @Override
        public void run() {
            logger.debug("CommandServer: Client connected.");

            try {

                OutputStreamWriter outStream = new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-16LE");
                InputStreamReader inStream = new InputStreamReader(clientSocket.getInputStream(), "UTF-16LE");

                PrintWriter out = new PrintWriter(outStream, true);
                BufferedReader in = new BufferedReader(inStream);

                try {
                    String command = in.readLine();

                    if (command == null) {
                        logger.debug("CommandServer (worker " + Thread.currentThread().getName() + "): Client disconnected. EXITING WORKER.", null);
                        return;
                    }

                    command = command.replace("\\", "\\\\");
                    JsonParser parser = new JsonParser();
                    JsonElement elem = parser.parse(command);
                    JsonObject jobject = elem.getAsJsonObject();

                    //TODO: check if the "command" value is set to "getFileIconId". However, it will decrease performance.

                    String path = jobject.get("value").getAsString();

                    String resultValue = getOverlayValueByPath(path);
                    String result = String.format("{\"value\":\"%s\"}", resultValue);
                    out.print(result);
                    out.flush();

                } catch (Exception e) {
                    logger.error("CommandServer (worker " + Thread.currentThread().getName() + "): Client disconnected. EXITING WORKER.", e);
                } finally {
                    try {
                        clientSocket.close();
                    } catch (IOException ex) {
                        logger.debug("I/O Exception.", ex);
                    }
                }

            } catch (IOException ex) {
                logger.debug("Could not create Input/OutputStream for CommandWorker.", ex);
            }
        }

        private SyncStatus getSyncStatusChildren(CloneFile cf) {
            SyncStatus status = cf.getSyncStatus();

            if (cf.isFolder() && status == SyncStatus.UPTODATE) {
                for (CloneFile cf2 : cf.getChildren()) {
                    status = getSyncStatusChildren(cf2);
                    if (status != SyncStatus.UPTODATE) {
                        break;
                    }
                }
            }

            return status;
        }

        private String getOverlayValueByPath(String path) {

            File file = new File(path);
            CloneFile cf = db.getFileOrFolder(file);
            String resultValue = "0";

            if (cf != null) {

                SyncStatus status = getSyncStatusChildren(cf);
                queryCache.put(file, new Date());

                switch (status) {
                    case UNSYNC:
                    case CONFLICT:
                        resultValue = "3";
                        break;
                    case SYNCING:
                    case LOCAL:
                    case REMOTE:
                        resultValue = "1";
                        break;
                    case UPTODATE:
                        resultValue = "2";
                        break;
                    default:
                        resultValue = "0";
                }
            }

            return resultValue;
        }
    }

    private class CommandWorkerLinux extends AbstractWorker {

        public CommandWorkerLinux(Socket clientSocket) {
            super(clientSocket);
        }

        @Override
        public void run() {
            logger.debug("CommandServer: Client connected.");

            try {
                OutputStream outStream = clientSocket.getOutputStream();
                InputStreamReader inStream = new InputStreamReader(clientSocket.getInputStream());

                PrintWriter out = new PrintWriter(outStream, true);
                BufferedReader in = new BufferedReader(inStream);
                try {
                    while (true) {
                        String command = in.readLine();

                        if (command == null) {
                            logger.debug("CommandServer (worker " + Thread.currentThread().getName() + "): Client disconnected. EXITING WORKER.", null);
                            return;
                        }

                        if ("exit".equals(command)) {
                            processExitCommand(out);
                            return;
                        }

                        // Commands
                        try {
                            logger.debug("Received command " + command + ".");

                            Map<String, List<String>> args = readArguments(in);
                            if ("get_emblems".equals(command) || "icon_overlay_file_status".equals(command)) {
                                processGetEmblemsCommand(out, command, args);
                            } else if ("icon_overlay_context_options".equals(command)) {
                                processContextOptionsCommand(out, command, args);
                            } else if ("get_folder_tag".equals(command)) {
                                processGetFolderTagCommand(out, command, args);
                            } else if ("get_emblem_paths".equals(command)) {
                                processGetEmblemsPathsCommand(out, command, args);
                            } else if ("icon_overlay_context_action".equals(command)) {
                                processContextActionCommand(out, command, args);
                            } else {
                                throw new CommandException("Unknown command '" + command + "'.");
                            }
                        } catch (CommandException e) {
                            logger.debug("Error in command, sending error:", e);

                            out.print("notok\n");
                            out.print(e.getMessage() + "\n");
                            out.print("done\n");
                            out.flush();
                        }
                    }

                } catch (IOException ex) {
                    logger.debug("Exception in CommandWorker.", ex);
                } finally {
                    try {
                        if (outStream != null) {
                            outStream.close();
                        }

                        clientSocket.close();
                    } catch (IOException ex) {
                        logger.debug("I/O Exception.", ex);
                    }
                }

            } catch (IOException ex) {
                logger.debug("Could not create Input/OutputStream for CommandWorker.", ex);
            }
        }

        private void processExitCommand(PrintWriter out) {
            out.print("ok\n");
            out.print("Exiting\t\n");
            out.print("done\n");
            out.flush();
        }
        
        private SyncStatus getSyncStatusChildren(CloneFile cf) {
            SyncStatus status = cf.getSyncStatus();

            if (cf.isFolder() && status == SyncStatus.UPTODATE) {
                for (CloneFile cf2 : cf.getChildren()) {
                    SyncStatus oldStatus = status;
                    status = getSyncStatusChildren(cf2);
                    
                    // If file UPTODATE check next
                    if (status == SyncStatus.UPTODATE) {
                        continue;
                    }
                    
                    // If file is UNSYNC may be this is not the last version!
                    if (status == SyncStatus.UNSYNC){
                        /* Get last version and compare paths:
                         *  Same path: folder is UNSYNC
                         *  Different path: UNSYNC file is not in this folder
                         */
                        CloneFile lastVersion = cf2.getLastVersion();
                        if (!lastVersion.getPath().equals(cf2.getPath())){
                            status = oldStatus;
                            continue;
                        }
                    }
                    break;
                }
            }

            return status;
        }

        private void processGetEmblemsCommand(PrintWriter out, String command, Map<String, List<String>> args) throws CommandException {
            if (!args.containsKey("path")) {
                throw new CommandException("Invalid Arguments: Argument 'path' missing.");
            }

            File file = new File(args.get("path").get(0));
            logger.debug("Command " + command + ": path= '" + file.getAbsolutePath() + "'");

            if (!file.exists()) {
                throw new CommandException("Invalid Argument: Given path '" + file.getAbsolutePath() + "' does not exist.");
            }

            boolean embl = "get_emblems".equals(command);
            String resultKey = (embl) ? "emblems" : "status";
            String resultValue = "";

            // Find current version in DB
            CloneFile cf = db.getFileOrFolder(file);

            if (cf != null) {
                logger.debug("Command " + command + ": DB entry for " + file + ":    " + cf + " (sync: " + cf.getSyncStatus());

                SyncStatus status = getSyncStatusChildren(cf);
                queryCache.put(file, new Date());

                switch (status) {
                    case UNSYNC:
                    case CONFLICT:
                        resultValue = (embl) ? "unsyncable" : "unsyncable";
                        break;
                    case SYNCING:
                    case LOCAL:
                    case REMOTE:
                        resultValue = (embl) ? "syncing" : "syncing";
                        break;
                    case UPTODATE:
                        resultValue = (embl) ? "uptodate" : "up to date";
                        break;
                    default:
                        resultValue = "";
                }
            }

            // Return result
            logger.debug("Command " + command + " RESULT: " + resultKey + "=" + resultValue + " for path= " + file.getAbsolutePath());

            out.print("ok\n");
            out.print(resultKey + "	" + resultValue + "\n");
            out.print("done\n");
            out.flush();
        }

        private void processContextOptionsCommand(PrintWriter out, String command, Map<String, List<String>> args) throws CommandException {
            if (!args.containsKey("paths")) {
                throw new CommandException("Invalid Arguments: Argument 'path' missing.");
            }

            String path = args.get("paths").get(0);
            logger.debug("Command " + command + ": icon_overlay_context_options: paths= '" + path + "'");

            // Find file in DB
            List<String> options = new ArrayList<String>();
            CloneFile currentVersion = db.getFileOrFolder(new File(path));

            if (currentVersion != null) {
                int i = 0;
                List<CloneFile> previousVersions = currentVersion.getPreviousVersions();

                for (CloneFile pv : previousVersions) {
                    /*if (pv.getStatus() == Status.MERGED) {
                     continue;
                     }*/

                    // Not more than 10
                    if (i > 10) {
                        break;
                    }

                    i++;
                    options.add("Restore '" + pv.getName() + "' ("
                            + DateUtil.toNiceFormat(pv.getLastModified()) + ", "
                            + FileUtil.formatSize(pv.getSize())
                            + ")~Restores the file from the remote storage.~restore:" + pv.getId() + ":" + pv.getVersion());
                }
            }

            // Concatenate
            String optionsStr = StringUtil.join(options, "	");
            logger.debug("Command " + command + ": Result is: options: " + optionsStr);

            // Return
            out.print("ok\n");
            out.print("options	" + optionsStr + "\n");
            out.print("done\n");
            out.flush();
        }

        private void processGetFolderTagCommand(PrintWriter out, String command, Map<String, List<String>> args) throws CommandException {
            if (!args.containsKey("path")) {
                throw new CommandException("Invalid Arguments: Argument 'path' missing.");
            }

            String path = args.get("path").get(0);
            logger.debug("Command " + command + ": get_folder_tag: path= " + path);

            // TODO check status
            out.print("ok\n");
            out.print("tag	\n");
            out.print("done\n");
            out.flush();
        }

        private void processGetEmblemsPathsCommand(PrintWriter out, String command, Map<String, List<String>> args) {
            /*if (!args.containsKey("path"))
             throw new CommandException("Invalid Arguments: Argument 'path' missing.");

             String path = args.get("path").get(0);
             logger.debug("CommandServer (worker "+Thread.currentThread().getName()+"): get_emblem_paths: path= '"+path+"'");
             */

            // TODO check status

            out.print("ok\n");
            out.print("path	" + config.getResDir().getAbsolutePath() + File.separator + "emblems\n");
            out.print("done\n");
            out.flush();
        }

        private void processContextActionCommand(PrintWriter out, String command, Map<String, List<String>> args) throws CommandException {
            if (!args.containsKey("verb")) {
                throw new CommandException("Invalid Arguments: Argument 'verb' missing.");
            }

            if (!args.containsKey("paths")) {
                throw new CommandException("Invalid Arguments: Argument 'paths' missing.");
            }

            String verb = args.get("verb").get(0);
            //String path = args.get("paths").get(0);

            // Do it!
            if (verb.startsWith("restore")) {
                logger.debug("Command " + command + ": " + verb + ".");
                String[] splited = verb.split(":"); // verb[0] idFile[1] version[2]

                String path = args.get("paths").get(0);
                CloneFile currentVersion = db.getFileOrFolder(new File(path));

                CloneFile restoringVersion = null;
                int i = 0;

                for (CloneFile pv : currentVersion.getPreviousVersions()) {
                    /*if (pv.getStatus() == Status.MERGED) {
                     continue;
                     }*/

                    // Not more than 10
                    if (i > 10) {
                        break;
                    }

                    i++;
                    if (splited[2].compareTo(String.valueOf(pv.getVersion())) == 0) {
                        restoringVersion = (CloneFile) pv.clone();
                        break;
                    }

                }

                if (restoringVersion != null) {
                    Folder root = restoringVersion.getRoot();
                    restoringVersion.setVersion(currentVersion.getVersion() + 1);
                    restoringVersion.setStatus(Status.CHANGED);

                    try {
                        root.getProfile().getRemoteWatcher().restoreVersion(restoringVersion);
                    } catch (CouldNotApplyUpdateException e) {
                        logger.warn("Warning: could not download/assemble " + restoringVersion, e);
                    }
                }

            } else {
                logger.debug("Command " + command + ": Unknown verb " + verb + ". IGNORING.");
            }

            out.print("ok\n");
            out.print("\n");
            out.print("done\n");
            out.flush();
        }
    }
}
