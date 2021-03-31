package com.theFox6.kleinerNerd.system;

import com.theFox6.kleinerNerd.KleinerNerd;
import foxLog.deamon.ConditionNotifier;
import foxLog.queued.QueuedLog;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Queue;

public class InstanceManager {
    private static File instanceFile = new File(KleinerNerd.dataFolder + "instance.properties");
    private static Properties instanceProperties;

    public static void setState(InstanceState state) {
        instanceProperties.setProperty("state", state.name());
        save();
    }

    public static void ensureLastShutdown() {
        if (!load()) {
            QueuedLog.error("Canceling startup because instance file was not loaded.");
            try {
                QueuedLog.flushInterruptibly();
            } catch (InterruptedException e) {
                QueuedLog.warning("interrupted log flushing", e);
            } catch (ConditionNotifier.NotNotifiableException e) {
                //this exception shouldn't even exist in the QueuedLog
                QueuedLog.error("Oi Fox, fix that log of yours!",e);
            }
            System.exit(1);
        }
        String stateString = instanceProperties.getProperty("state");
        InstanceState s;
        if (stateString == null) {
            s = InstanceState.UNKNOWN;
            instanceProperties.setProperty("state",s.name());
            save();
        } else {
            s = InstanceState.valueOf(stateString);
        }
        //TODO: just wait if he is shutting down, etc.
        if (s.running) {
            QueuedLog.warning("Der KleinerNerd ist nicht ordentlich heruntergefahren. " +
                    "Dies kann zu Datenverlust führen.");
            QueuedLog.debug(s.message);
            QueuedLog.message("Falls der KleineNerd nicht mehr läuft, " +
                    "kann in das instance file als Zustand \"OVERRIDE\" eingetragen werden.");
            try {
                QueuedLog.flushInterruptibly();
            } catch (InterruptedException e) {
                QueuedLog.warning("interrupted log flushing",e);
            } catch (ConditionNotifier.NotNotifiableException e) {
                //this exception shouldn't even exist in the QueuedLog
                QueuedLog.error("Oi Fox, fix that log of yours!",e);
            }
            //let's quit
            System.exit(1);
        } else {
            //perhaps check whether it's a normal state or not
            QueuedLog.debug(s.message);
        }
    }

    private static void save() {
        QueuedLog.action("saving instance properties");
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
