package com.theFox6.kleinerNerd;

import com.theFox6.kleinerNerd.achievements.EmoteAchievementListener;
import com.theFox6.kleinerNerd.categories.CategoryCreationListener;
import com.theFox6.kleinerNerd.categories.CategoryStorage;
import com.theFox6.kleinerNerd.conversation.rulebased.RPGConvoListener;
import com.theFox6.kleinerNerd.echo.ManualMessagingListener;
import com.theFox6.kleinerNerd.listeners.*;
import com.theFox6.kleinerNerd.reactionRoles.ReactionRoleListener;
import com.theFox6.kleinerNerd.reactionRoles.ReactionRoleStorage;
import com.theFox6.kleinerNerd.storage.ConfigFiles;
import com.theFox6.kleinerNerd.storage.CounterStorage;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import com.theFox6.kleinerNerd.system.InstanceManager;
import com.theFox6.kleinerNerd.system.InstanceState;
import foxLog.LogLevel;
import foxLog.MainHelper;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class KleinerNerd {
	public static final String dataFolder = ".KleinerNerd/";
	public static final File logFile = new File("log.txt");
	//this could later become guild and dm-channel individual
	public static String prefix = ".";

	public static JDA buildBot(AnnotatedEventManager eventManager) {
		JDA jda;
		try {
            jda = JDABuilder.createDefault(ConfigFiles.getToken())
                    .setEventManager(eventManager)
                    .build();
        } catch (LoginException e) {
            QueuedLog.error("Login failed (probably because of a bad token)",e);
            return null;
        } catch (ErrorResponseException e) {
        	QueuedLog.error("Error response while building bot");
        	QueuedLog.error("response meaning: " + e.getMeaning());
        	Exception err = e.getResponse().getException();
        	if (err != null)
        		QueuedLog.error(err.getMessage());
        	return null;
        }
        try {
        	jda.awaitReady();
        } catch (InterruptedException e) {
        	QueuedLog.warning("interrupted while waiting to get ready",e);
            return jda;
        }
        QueuedLog.action("Bot started");
        return jda;
	}

	public static void main(String[] args) {
		if (MainHelper.hasOption(args, "C", "no-colorize"))
			QueuedLog.setOutputColorized(false);
		else /*if (MainHelper.hasOption(args, "c", "colorize"))*/
			QueuedLog.setOutputColorized(true);
		if (MainHelper.hasOption(args, "f", "fullog")) {
			QueuedLog.setLevel(LogLevel.ALL);
			QueuedLog.debug("full logging enabled");
		} else {
			QueuedLog.setLevel(LogLevel.DEBUG);
		}

		InstanceManager.ensureLastShutdown();
		startup();
	}

	private static void startup() {
		InstanceManager.setState(InstanceState.STARTING);
		// set the log file
		try {
			QueuedLog.setLogFile(logFile);
		} catch (FileNotFoundException e) {
			QueuedLog.error("Error while trying to set logfile",e);
		}
		GuildStorage.load();
		ReactionRoleStorage.load();
		CategoryStorage.load();
		CounterStorage.loadAll();
		Runtime.getRuntime().addShutdownHook(new Thread("KleinerNerd-shutdownHook") {
			@Override
			public void run() {
				QueuedLog.action("runtime shutting down");
				checkDataFolder();
				GuildStorage.save();
				ReactionRoleStorage.save();
				CategoryStorage.save();
				CounterStorage.saveAll();
				QueuedLog.printWarningCount();
				QueuedLog.printErrorCount();
				QueuedLog.flush();
				QueuedLog.close();
				InstanceManager.setState(InstanceState.SHUTDOWN);
			}
		});

		JDA jda = buildBot(setUpEventManager());
		if (jda == null) {
			QueuedLog.warning("bot unbuilt, trying to exit");
			InstanceManager.setState(InstanceState.DYING);
			System.exit(1);
		}
		ModLog.sendToOwners(jda,"I started up.");
		InstanceManager.setState(InstanceState.RUNNING);
	}

	private static AnnotatedEventManager setUpEventManager() {
		AnnotatedEventManager eventManager = new AnnotatedEventManager();
		eventManager.register(new LoggingListener());

		eventManager.register(new HelpListener());
		eventManager.register(new SystemCommandListener());
		eventManager.register(new ConfigurationListener());
		eventManager.register(new PollListener());
		eventManager.register(new ManualMessagingListener());
		eventManager.register(new ReactionRoleListener());
		eventManager.register(new CategoryCreationListener());

		eventManager.register(new ConvoSnippetListener());
		try {
			eventManager.register(new SuicideListener());
		} catch (IOException e) {
			QueuedLog.error("IOException while trying to load SuicideListener", e);
		}
		eventManager.register(new EmoteAchievementListener());

		eventManager.register(new StalkerCommandListener());
		eventManager.register(new VoiceLoggingListener());

		eventManager.register(new TestListener());
		eventManager.register(new RPGConvoListener());
		return eventManager;
	}

	public static void checkDataFolder() {
		File folder = new File(dataFolder);
		if (folder.exists()) {
			if (!folder.isDirectory()) {
				QueuedLog.warning("The data folder is not a directory! Check: " + folder.getAbsolutePath());
			}
		} else {
			if (!folder.mkdir())
				QueuedLog.warning("Data folder was not created.");
		}
	}
}
