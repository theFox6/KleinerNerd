package com.theFox6.kleinerNerd;

import java.io.File;
import java.io.FileNotFoundException;
import javax.security.auth.login.LoginException;

import com.theFox6.kleinerNerd.consumable.AnnotatedConvertingEventManager;
import com.theFox6.kleinerNerd.consumable.ConsumableGuildMessageReceivedEvent;
import com.theFox6.kleinerNerd.echo.ManualMessagingListener;
import com.theFox6.kleinerNerd.listeners.ConfigurationListener;
import com.theFox6.kleinerNerd.listeners.VoiceLoggingListener;
import com.theFox6.kleinerNerd.listeners.HelpListener;
import com.theFox6.kleinerNerd.listeners.LoggingListener;
import com.theFox6.kleinerNerd.listeners.SimpleCommandListener;
import com.theFox6.kleinerNerd.listeners.StalkerCommandListener;
import com.theFox6.kleinerNerd.listeners.SuicideListener;
import com.theFox6.kleinerNerd.listeners.TestListener;
import com.theFox6.kleinerNerd.reactionRoles.ReactionRoleListener;
import com.theFox6.kleinerNerd.reactionRoles.ReactionRoleStorage;
import com.theFox6.kleinerNerd.storage.ConfigFiles;
import com.theFox6.kleinerNerd.storage.CounterStorage;
import com.theFox6.kleinerNerd.storage.GuildStorage;

import foxLog.LogLevel;
import foxLog.MainHelper;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class KleinerNerd {
	public static final String dataFolder = ".KleinerNerd/";
	//this could later become guild and dm-channel individual
	public static String prefix = ".";
	private static AnnotatedConvertingEventManager eventManager;

	public static JDA buildBot() {
		JDA jda;
		try {
            jda = JDABuilder.createDefault(ConfigFiles.getToken())
                    .setEventManager(eventManager)
                    .build();
        } catch (LoginException e) {
            QueuedLog.error("Login failed (probably because of  a bad token)",e);
            return null;
        } catch (ErrorResponseException e) {
        	QueuedLog.error("Error response while building bot");
        	QueuedLog.error("response meaning: " + e.getMeaning());
        	QueuedLog.error(e.getResponse().getException().getMessage());
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
		else if (MainHelper.hasOption(args, "c", "colorize")) {
			QueuedLog.setOutputColorized(true);
		}
		if (MainHelper.hasOption(args, "f", "fullog")) {
			QueuedLog.setLevel(LogLevel.ALL);
			QueuedLog.debug("full logging enabled");
		} else {
			QueuedLog.setLevel(LogLevel.INFO);
		}
		// set the log file
		try {
			QueuedLog.setLogFile(new File("log.txt"));
		} catch (FileNotFoundException e) {
			QueuedLog.error("Error while trying to set logfile",e);
		}
		GuildStorage.load();
		ReactionRoleStorage.load();
		CounterStorage.load();
		
		eventManager = new AnnotatedConvertingEventManager();
		convertEvent(GuildMessageReceivedEvent.class, ConsumableGuildMessageReceivedEvent.class);
		
		eventManager.register(new LoggingListener());
		
		eventManager.register(new HelpListener());
		eventManager.register(new SimpleCommandListener());
		eventManager.register(new StalkerCommandListener());
		eventManager.register(new ConfigurationListener());
		eventManager.register(new ManualMessagingListener());
		eventManager.register(new ReactionRoleListener());
		
		eventManager.register(new SuicideListener());
		
		eventManager.register(new VoiceLoggingListener());
		eventManager.register(new TestListener());
		JDA jda = buildBot();
		if (jda == null) {
			QueuedLog.warning("bot unbuilt, trying to exit");
			System.exit(1);
		}
		ModLog.sendToOwner(jda,"I started up.");
		Runtime.getRuntime().addShutdownHook(new Thread("KleinerNerd-shutdownHook") {
			@Override
			public void run() {
				QueuedLog.action("runtime shutting down");
				checkDataFolder();
				GuildStorage.save();
				ReactionRoleStorage.save();
				CounterStorage.save();
				QueuedLog.printWarningCount();
				QueuedLog.printErrorCount();
				QueuedLog.flush();
				QueuedLog.close();
			}
		});
	}

	private static void convertEvent(Class<?> eventBefore, Class<? extends GenericEvent> eventAfter) {
		try {
			eventManager.convertEvent(eventBefore, eventAfter);
		} catch (NoSuchMethodException e) {
			QueuedLog.error(eventBefore.getName() + " needs a constructor that accepts only a " + eventAfter.getName(),e);
		} catch (SecurityException e) {
			QueuedLog.error("Could not add event for conversion.",e);
		}
	}

	public static void checkDataFolder() {
		File folder = new File(dataFolder);
		if (folder.exists()) {
			if (!folder.isDirectory()) {
				QueuedLog.warning("The data folder is not a directory! Check: " + folder.getAbsolutePath());
			}
		} else {
			folder.mkdir();
		}
	}
}
