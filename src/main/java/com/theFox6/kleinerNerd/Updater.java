package com.theFox6.kleinerNerd;

import com.theFox6.kleinerNerd.system.InstanceManager;
import com.theFox6.kleinerNerd.system.InstanceState;
import foxLog.LogLevel;
import foxLog.MainHelper;
import foxLog.queued.QueuedLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Updater {
    public static final File logFile = new File("updater-log.txt");

    //TODO: make it run in jar
    //perhaps make it run on windows

    public static void main(String[] args) {
        // set the log file
        try {
            QueuedLog.setLogFile(logFile);
        } catch (FileNotFoundException e) {
            QueuedLog.error("Error while trying to set logfile",e);
        }
        //set colorization
        if (MainHelper.hasOption(args, "C", "no-colorize"))
            QueuedLog.setOutputColorized(false);
        else /*if (MainHelper.hasOption(args, "c", "colorize"))*/
            QueuedLog.setOutputColorized(true);
        // set log level
        if (MainHelper.hasOption(args, "f", "fullog")) {
            QueuedLog.setLevel(LogLevel.ALL);
            QueuedLog.debug("full logging enabled");
        } else {
            QueuedLog.setLevel(LogLevel.DEBUG);
        }
        // wait for last to finish
        InstanceManager.ensureLastShutdown();
        InstanceManager.setState(InstanceState.UPDATING);
        //update
        if (update()) {
            QueuedLog.action("update successful");
            InstanceManager.setState(InstanceState.UPDATED);
        } else {
            QueuedLog.warning("trouble while updating");
            InstanceManager.setState(InstanceState.UPDATE_FAILED);
        }
        //close logs
        QueuedLog.printWarningCount();
        QueuedLog.printErrorCount();
        QueuedLog.flush();
        QueuedLog.close();
    }

    private static boolean update() {
        boolean errorFree = true;
        QueuedLog.action("pulling latest commits");
        Process git = null;
        try {
            git = new ProcessBuilder("git","pull").start();
        } catch (IOException e) {
            QueuedLog.error("Could not execute git pull",e);
            errorFree = false;
        }
        if (git == null)
            errorFree = false;
        else {
            int exit;
            try {
                exit = git.waitFor();
            } catch (InterruptedException e) {
                QueuedLog.error("Interrupted pulling new commits", e);
                return false;
            }
            if (exit != 0) {
                QueuedLog.error("git exited with status code " + exit);
                errorFree = false;
            }
        }
        QueuedLog.action("restarting main process");
        try {
            new ProcessBuilder("bash","KleinerNerd.sh").start();
        } catch (IOException e) {
            QueuedLog.error("Launching the start script failed",e);
            return false;
        }
        return errorFree;
    }
}
