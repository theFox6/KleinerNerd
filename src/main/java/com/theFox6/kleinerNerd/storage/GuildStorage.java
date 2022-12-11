package com.theFox6.kleinerNerd.storage;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.commands.ModRoleChangeListener;
import com.theFox6.kleinerNerd.data.GuildSettings;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GuildStorage {
	private static final Path gsf = KleinerNerd.dataFolder.resolve("guilds.json");
	private static final Path bsf = KleinerNerd.dataFolder.resolve("guilds_broken.json");
	private static boolean broken = false;

	private static ConcurrentHashMap<String, GuildSettings> settings = null;
	private static final Set<ModRoleChangeListener> modroleListeners = ConcurrentHashMap.newKeySet();

	public static void load() {
		if (Files.exists(gsf)) {
			JavaType settingsType = TypeFactory.defaultInstance().constructParametricType(ConcurrentHashMap.class, String.class, GuildSettings.class);
			try {
				settings = new ObjectMapper().readValue(Files.newBufferedReader(gsf), settingsType);
			} catch (JsonParseException e) {
				broken = true;
				QueuedLog.error("parse error while trying to load guild settings", e);
				loadingError("JSON parse error while loading guild settings");
			} catch (JsonMappingException e) {
				broken = true;
				QueuedLog.error("mapping error while trying to load guild settings", e);
				loadingError("JSON mapping error while loading guild settings");
			} catch (IOException e) {
				broken = true;
				QueuedLog.error("io error while trying to load guild settings", e);
				loadingError("io error while loading guild settings");
			}
		}
		if (settings == null)
			settings = new ConcurrentHashMap<>();
	}
	
	public static void save() {
		if (broken) {
			if (Files.exists(bsf)) {
				QueuedLog.error("previous guild setting file was broken, not overriding");
				return;
			} else {
				try {
					Files.move(gsf, bsf);
				} catch (IOException e) {
					QueuedLog.error("failed to rename prevoius (broken) guild setting file, aborting save", e);
					return;
				}
				QueuedLog.warning("previous guild setting file was broken and renamed to " + bsf.toAbsolutePath());
			}
		}
		try {
			new ObjectMapper().writeValue(Files.newBufferedWriter(gsf), settings);
		} catch (JsonGenerationException e) {
			QueuedLog.error("generation error while trying to save guild settings", e);
			saveError("JSON generation error while writing guild settings");
		} catch (JsonMappingException e) {
			QueuedLog.error("mapping error while trying to save guild settings", e);
			saveError("JSON mapping error while writing guild settings");
		} catch (IOException e) {
			QueuedLog.error("io error while trying to save guild settings", e);
			saveError("io error while writing guild settings");
		}
	}
	
	private static GuildSettings getSettings(String guildId) {
		GuildSettings s = settings.get(guildId);
		if (s == null) {
			s = new GuildSettings();
			settings.put(guildId, s);
		}
		return s;
	}

	public static void addModroleListener(ModRoleChangeListener l) {
		modroleListeners.add(l);
	}

	public static Role getModRole(Guild guild) {
		String mrid = getSettings(guild.getId()).moderatorRole;
		if (mrid == null) {
			QueuedLog.info("modrole not set in guild \"" + guild.getName() + "\"");
			return null;
		}
		Role mr = guild.getRoleById(mrid);
		if (mr == null) {
			//TODO: throw role not found exception
			QueuedLog.error("couldn't find modrole with id " + mrid + " for guild \"" + guild.getName() + "\"");
			return null;
		}
		return mr;
	}

	public static void setModrole(Guild g, @Nullable Role modrole) {
		GuildSettings s = getSettings(g.getId());
		if (modrole == null)
			s.moderatorRole = null;
		else
			s.moderatorRole = modrole.getId();
		modroleListeners.forEach((l) -> l.onModRoleChange(g, modrole));
	}

	private static void loadingError(String msg) {
		// TODO Ask the owner whether to shutdown without quit hooks
	}

	private static void saveError(String msg) {
		// TODO ask owner whether to shutdown and risk data loss
		// 		this might not be easy when already within shutdown process
	}

	public static void setModLogChannel(Guild guild, @Nullable GuildMessageChannel chan) {
		//was this an extra safety precaution? perhaps remove it
		String gid = guild.getId();
		if (chan == null)
			getSettings(gid).modLogChannel = null;
		else {
			if (!gid.equals(chan.getGuild().getId()))
				throw new IllegalArgumentException("given channel is not within given guild");
			getSettings(gid).modLogChannel = chan.getId();
		}
	}

	public static TextChannel getModLogChannel(Guild g) {
		String mlcid = getSettings(g.getId()).modLogChannel;
		if (mlcid == null)
			return null;
		//FIXME: it might not be a text channel
		TextChannel tc = g.getTextChannelById(mlcid);
		if (tc == null) {
			QueuedLog.debug("could not get modlog channel channel from guild, trying to fetch it from jda");
			tc = g.getJDA().getTextChannelById(mlcid);
		}
		if (tc == null) {
			QueuedLog.warning("could not find modlog channel");
			return null;
		}
		return tc;
	}
}
