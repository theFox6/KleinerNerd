package com.theFox6.kleinerNerd;

import com.theFox6.kleinerNerd.achievements.EmoteAchievementListener;
import com.theFox6.kleinerNerd.commands.CommandListener;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.ModRoleChangeListener;
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimerTask;

public class KleinerNerd {
	public static final Path dataFolder = FileSystems.getDefault().getPath(".KleinerNerd/");
	public static final Path backupFolder = FileSystems.getDefault().getPath("backup");
	public static final File logFile = new File("log.txt");
	//this could later become guild and dm-channel individual
	public static String prefix = ".";
	private static CommandManager commandManager;

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
		//TODO: -t --checks (only run checks then quit, do not log on)
		//TODO: rewrite command line option parsing (see wrapper)

		/*(MainHelper.hasOption(args, "c", "colorize")*/
		QueuedLog.setOutputColorized(!MainHelper.hasOption(args, "C", "no-colorize"));

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
		boolean updated = InstanceManager.isUpdating();
		//TODO: check if the last state was shutdown
		InstanceManager.setState(InstanceState.STARTING);
		// set the log file
		try {
			QueuedLog.setLogFile(logFile);
		} catch (FileNotFoundException e) {
			QueuedLog.error("Error while trying to set logfile",e);
		}
		if (!backupData()) {
			QueuedLog.error("data not backed up, exiting");
			System.exit(1);
		}
		GuildStorage.load();
		ReactionRoleStorage.load();
		CounterStorage.loadAll();
		Runtime.getRuntime().addShutdownHook(new Thread("KleinerNerd-shutdownHook") {
			@Override
			public void run() {
				QueuedLog.action("runtime shutting down");
				saveAll();
				QueuedLog.printWarningCount();
				QueuedLog.printErrorCount();
				QueuedLog.flush();
				QueuedLog.close();
				if (InstanceManager.isUpdating())
					InstanceManager.setState(InstanceState.UPDATE);
				else if (InstanceManager.isShuttingDown())
					InstanceManager.setState(InstanceState.SHUTDOWN);
				else
					InstanceManager.setState(InstanceState.QUIT);
			}
		});
		final int day = 1000 * 60 * 60 * 24;
		KNHelpers.timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				saveAll();
			}
		}, day, day);

		AnnotatedEventManager manager = setUpEventManager();
		JDA jda = buildBot(manager);
		if (jda == null) {
			QueuedLog.warning("bot unbuilt, trying to exit");
			InstanceManager.setState(InstanceState.DYING);
			System.exit(1);
		}
		setupCommands(jda, manager.getRegisteredListeners());
		//TODO: add option for shutdown vs other
		if (updated)
			ModLog.sendToOwners(jda,"Ich wurde aktualisiert.");
		else
			ModLog.sendToOwners(jda,"Ich bin gestartet.");
		InstanceManager.setState(InstanceState.RUNNING);
	}

	private static void saveAll() {
		checkDataFolder();
		GuildStorage.save();
		ReactionRoleStorage.save();
		CounterStorage.saveAll();
	}

	private static boolean backupData() {
		String backupName = "data_" + DateTimeFormatter.BASIC_ISO_DATE.withZone(ZoneId.of("UTC")).format(Instant.now());
		Path currentBackup = backupFolder.resolve(backupName);
		if (Files.exists(currentBackup)) {
			for (int i = 1; Files.exists(currentBackup); i++)
			currentBackup = backupFolder.resolve(backupName + '_' + i);
		}
		try {
			KNHelpers.copyFolder(dataFolder, currentBackup);
		} catch (IOException e) {
			QueuedLog.error("failed to backup data", e);
			return false;
		}
		return true;
	}

	private static void setupCommands(JDA jda, List<Object> listeners) {
		commandManager.upsertCommands(jda);
		listeners.forEach((l) -> {
			if (l instanceof ModRoleChangeListener) {
				GuildStorage.addModroleListener(((ModRoleChangeListener) l));
			}
			if (l instanceof CommandListener) {
				((CommandListener) l).setupCommands(jda);
			}
		});
	}

	private static AnnotatedEventManager setUpEventManager() {
		AnnotatedEventManager eventManager = new AnnotatedEventManager();
		eventManager.register(new LoggingListener());

		commandManager = new CommandManager();
		eventManager.register(commandManager);

		eventManager.register(new SystemCommandListener().setupCommands(commandManager));
		new ConfigurationListener().setupCommands(commandManager);
		eventManager.register(new ReactionRoleListener().setupCommands(commandManager));

		eventManager.register(new ConvoSnippetListener());
		try {
			eventManager.register(new SuicideListener());
		} catch (IOException e) {
			QueuedLog.error("IOException while trying to load SuicideListener", e);
		}
		eventManager.register(new EmoteAchievementListener());

		new PollListener().setupCommands(commandManager);
		new StalkerCommandListener().setupCommands(commandManager);


		eventManager.register(new VoiceLoggingListener());
		return eventManager;
	}

	public static void checkDataFolder() {
		if (Files.exists(dataFolder)) {
			if (!Files.isDirectory(dataFolder)) {
				QueuedLog.warning("The data folder is not a directory! Check: " + dataFolder.toAbsolutePath());
			}
		} else {
			try {
				Files.createDirectories(dataFolder);
			} catch (IOException e) {
				QueuedLog.error("Data folder was not created.", e);
				e.printStackTrace();
			}
		}
	}
}
