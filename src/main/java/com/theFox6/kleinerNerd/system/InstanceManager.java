package com.theFox6.kleinerNerd.system;

import com.theFox6.kleinerNerd.KleinerNerd;
import foxLog.queued.QueuedLog;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class InstanceManager {
    private static final File instanceFile = new File(KleinerNerd.dataFolder + "instance.properties");
    private static final int maxTries = 12;
    private static Properties instanceProperties;

    public static void setState(InstanceState state) {
        instanceProperties.setProperty("state", state.name());
        save();
    }

    public static void ensureLastShutdown() {
        InstanceState s = loadState(true);
        if (!s.running) {
            //TODO: check whether it's a normal state or not
            QueuedLog.debug(s.message);
            return;
        }
        QueuedLog.message("Der KleineNerd läuft scheinbar noch.");
        QueuedLog.message("Falls der KleineNerd nicht mehr läuft, " +
                "kann in das instance file als Zustand \"OVERRIDE\" eingetragen werden.");
        InstanceState last = null;
        int attempts = 0;
        //perhaps add support for maxTries = -1
        while (s.running && attempts++ < maxTries) {
            if (last != s) {
                QueuedLog.debug(s.message);
                last = s;
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                QueuedLog.error("Interrupted waiting for other instance.",e);
                s = loadState(true);
                break;
            }
            s = loadState(true);
        }
        if (s.running) {
            QueuedLog.warning("Der KleinerNerd ist nicht ordentlich heruntergefahren. " +
                    "Dies kann zu Datenverlust führen.");
            try {
                QueuedLog.flushInterruptibly();
            } catch (InterruptedException e) {
                QueuedLog.warning("interrupted log flushing",e);
            }
            //let's quit
            System.exit(1);
        }
    }

    private static InstanceState loadState(boolean exitOnFail) {
        if (!load()) {
            if (exitOnFail) {
                    QueuedLog.error("Cancelling startup due to an error while loading instance file.");
                try {
                    QueuedLog.flushInterruptibly();
                } catch (InterruptedException e) {
                    QueuedLog.warning("interrupted log flushing", e);
                }
                System.exit(1);
                return null;
            } else {
                QueuedLog.debug("Instance file not loaded");
                return null;
            }
        }
        String stateString = instanceProperties.getProperty("state");
        if (stateString == null) {
            InstanceState s = InstanceState.UNKNOWN;
            instanceProperties.setProperty("state",s.name());
            save();
            return s;
        } else {
            return InstanceState.valueOf(stateString);
        }
    }

    private static void save() {
        QueuedLog.action("saving instance properties");
        KleinerNerd.checkDataFolder();
        try (FileWriter fw = new FileWriter(instanceFile)) {
            instanceProperties.store(fw, "KleinerNerd instance properties");
        } catch (IOException e) {
            QueuedLog.error("could not write instance file", e);
        }
    }

    private static boolean load() {
        if (instanceProperties == null) {
            QueuedLog.action("loading instance properties");
            instanceProperties = new Properties();
        } else {
            QueuedLog.verbose("reloading instance properties");
        }
        if (instanceFile.exists()) {
            try (FileReader fr = new FileReader(instanceFile)) {
                instanceProperties.load(fr);
            } catch (IOException e) {
                QueuedLog.error("failed to load instance file", e);
                return false;
            }
            if (instanceProperties.isEmpty())
                QueuedLog.warning("instance file is empty");
        } else {
            QueuedLog.debug("instance file does not exist yet");
        }
        return true;
    }
}
