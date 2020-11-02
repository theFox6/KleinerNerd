package com.theFox6.kleinerNerd.matchers;

import java.util.regex.Pattern;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.ChannelType;

public class ChannelLocationMatch extends CommandMatch {
	
	//TODO: use MessageChannelLocation

	public static final Pattern commandArgsSeperator = Pattern.compile("\\s");
	public static final Pattern snowflakeIdPattern = Pattern.compile("\\d+");
	private final ChannelType expectedPlace;
	private final String guildID;
	private final String chanID;

	public ChannelLocationMatch(MatchType match, ChannelType expectedPlace, String guildId, String chanId, String args) {
		super(match,args);
		this.expectedPlace = expectedPlace;
		this.guildID = guildId;
		this.chanID = chanId;
	}
	
	public ChannelLocationMatch(MatchType match, ChannelType expectedPlace, String chanId, String args) {
		this(match, expectedPlace, null, chanId, args);
	}
	
	public ChannelLocationMatch(ChannelType expectedPlace, String chanId, String args) {
		this(MatchType.MISSING_OPTIONALS, expectedPlace, null, chanId, args);
	}

	public ChannelLocationMatch(ChannelType expectedPlace, String args) {
		this(MatchType.NO_MATCH, expectedPlace, null, null, args);
	}

	public ChannelLocationMatch(String guildId, String chanId, String args) {
		this(MatchType.MISSING_OPTIONALS, null, guildId, chanId, args);
	}

	public ChannelLocationMatch(String chanId, String args) {
		this(MatchType.MISSING_OPTIONALS, null, null, chanId, args);
	}
	
	public ChannelLocationMatch(String args) {
		this(MatchType.NO_MATCH, null, null, null, args);
	}
	
	public ChannelType getExpectedPlace() {
		return expectedPlace;
	}

	public String getChannelId() {
		return chanID;
	}

	public String getGuildId() {
		return guildID;
	}

	public boolean isInGuild() {
		if (guildID == null)
			return false;
		return !guildID.isEmpty();
	}

	/**
	 * Match the next arguments given as channel location.
	 * @param args the command arguments
	 * @return a match that shows whether the channel location was provided and what arguments were found
	 */
	public static ChannelLocationMatch matchLocation(String args) {
		String[] firstArgs = commandArgsSeperator.split(args,2);
		if (firstArgs.length < 1)
			return new ChannelLocationMatch(args);
		if (firstArgs.length > 2)
			throw new MatchOverflowException("Expected two matches got " + firstArgs.length);
		if (firstArgs[0].startsWith("<")) {
			ChannelLocationMatch mentionTarget = matchMention(firstArgs[0]);
			if (mentionTarget.match == MatchType.NO_MATCH)
				return mentionTarget;
			else if (mentionTarget.match == MatchType.FULL_MATCH) {
				if (firstArgs.length > 1) {
					return new ChannelLocationMatch(MatchType.MISSING_OPTIONALS,mentionTarget.expectedPlace,mentionTarget.guildID,mentionTarget.chanID,firstArgs[1]);
				} else {
					return new ChannelLocationMatch(MatchType.MISSING_OPTIONALS,mentionTarget.expectedPlace,mentionTarget.guildID,mentionTarget.chanID,null);
				}
			} else if (mentionTarget.match == MatchType.MISSING_OPTIONALS) {
				if (firstArgs.length > 1) {
					return new ChannelLocationMatch(MatchType.MISSING_OPTIONALS,mentionTarget.expectedPlace,mentionTarget.guildID,mentionTarget.chanID,firstArgs[1]);
				} else {
					return new ChannelLocationMatch(MatchType.MISSING_OPTIONALS,mentionTarget.expectedPlace,mentionTarget.guildID,mentionTarget.chanID,null);
				}
			} else {
				throw new IllegalArgumentException("Did not expect matchMention() to report " + mentionTarget.match.name());
			}
		} else if (snowflakeIdPattern.asPredicate().test(firstArgs[0])) {
			if (firstArgs.length < 2) {
				return new ChannelLocationMatch(firstArgs[0], null);
			}
			return matchLocationBySnowflakeAnd(firstArgs[0], firstArgs[1]);
		} else {
			if (firstArgs.length < 2) {
				return new ChannelLocationMatch(args);
			}
			ChannelType expectedPlace = ChannelType.valueOf(firstArgs[0]);
			if (expectedPlace == null) {
				return new ChannelLocationMatch(args);
			}
			if (expectedPlace == ChannelType.PRIVATE) {
				return matchPrivateChannel(firstArgs[1]);
			} else if (expectedPlace == ChannelType.TEXT) {
				return matchTextChannel(firstArgs[1]);
			} else {
				QueuedLog.warning("ChannelLocationMatcher cannot match ChannelType " + expectedPlace.name());
				return new ChannelLocationMatch(expectedPlace, args);
			}
		}
		//throw new RuntimeException("Fell through all if statements with args " + args);
	}

	private static ChannelLocationMatch matchLocationBySnowflakeAnd(String snowflake, String args) {
		String[] nextArgs = commandArgsSeperator.split(args,2);
		if (nextArgs.length < 1) {
			return new ChannelLocationMatch(snowflake, args);
		} else if (nextArgs.length > 2) {
			throw new MatchOverflowException("Expected two matches got " + nextArgs.length);
		}
		if (nextArgs[0].startsWith("<")) {
			ChannelLocationMatch mentionTarget = matchMention(nextArgs[0]);
			if (mentionTarget.match == MatchType.NO_MATCH)
				return new ChannelLocationMatch(snowflake, args);
			else if (mentionTarget.match == MatchType.FULL_MATCH) {
				if (mentionTarget.expectedPlace == ChannelType.PRIVATE) {
					QueuedLog.debug("mentioned person right after giving a snowflakeId");
					return new ChannelLocationMatch(snowflake, args);
				}
				if (nextArgs.length > 1) {
					return new ChannelLocationMatch(snowflake, nextArgs[0], nextArgs[1]);
				} else {
					return new ChannelLocationMatch(snowflake, nextArgs[0], null);
				}
			} else {
				throw new IllegalArgumentException("Did not expect matchMention() to report " + mentionTarget.match.name());
			}
		} else if (snowflakeIdPattern.asPredicate().test(nextArgs[0])) {
			if (nextArgs.length > 1) {
				return new ChannelLocationMatch(snowflake, nextArgs[0], nextArgs[1]);
			} else {
				return new ChannelLocationMatch(snowflake, nextArgs[0], null);
			}
		} else
			return new ChannelLocationMatch(snowflake, args);
	}
	
	private static ChannelLocationMatch matchPrivateChannel(String args) {
		String[] nextArgs = commandArgsSeperator.split(args,2);
		if (nextArgs.length < 1) {
			return new ChannelLocationMatch(ChannelType.PRIVATE, args);
		} else if (nextArgs.length > 2) {
			throw new MatchOverflowException("Expected two matches got " + nextArgs.length);
		}
		if (nextArgs[0].startsWith("<")) {
			ChannelLocationMatch mentionTarget = matchMention(nextArgs[0]);
			if (mentionTarget.match == MatchType.NO_MATCH)
				return new ChannelLocationMatch(ChannelType.PRIVATE, args);
			else if (mentionTarget.match == MatchType.FULL_MATCH) {
				MatchType matchType;
				if (mentionTarget.expectedPlace == ChannelType.PRIVATE) {
					matchType = MatchType.FULL_MATCH;
				} else {
					QueuedLog.debug("mentioned a channel after declaring location PRIVATE");
					matchType = MatchType.NO_MATCH;
				}
				if (nextArgs.length > 1) {
					return new ChannelLocationMatch(matchType, ChannelType.PRIVATE, nextArgs[0], nextArgs[1]);
				} else {
					return new ChannelLocationMatch(matchType, ChannelType.PRIVATE, nextArgs[0], null);
				}
			} else {
				throw new IllegalArgumentException("Did not expect matchMention() to report " + mentionTarget.match.name());
			}
		} else if (snowflakeIdPattern.asPredicate().test(nextArgs[0])) {
			if (nextArgs.length > 1) {
				return new ChannelLocationMatch(MatchType.FULL_MATCH, ChannelType.PRIVATE, nextArgs[0], nextArgs[1]);
			} else {
				return new ChannelLocationMatch(MatchType.FULL_MATCH, ChannelType.PRIVATE, nextArgs[0], null);
			}
		} else
			return new ChannelLocationMatch(ChannelType.PRIVATE, args);
	}

	private static ChannelLocationMatch matchTextChannel(String args) {
		String[] nextArgs = commandArgsSeperator.split(args,2);
		if (nextArgs.length < 1)
			return new ChannelLocationMatch(ChannelType.TEXT, args);
		if (nextArgs.length > 2) {
			throw new MatchOverflowException("Expected two matches got " + nextArgs.length);
		}
		if (nextArgs[0].startsWith("<")) {
			ChannelLocationMatch mentionTarget = matchMention(nextArgs[0]);
			if (mentionTarget.match == MatchType.NO_MATCH)
				return new ChannelLocationMatch(ChannelType.TEXT, args);
			else if (mentionTarget.match == MatchType.FULL_MATCH) {
				MatchType matchType;
				if (mentionTarget.expectedPlace == ChannelType.TEXT) {
					matchType = MatchType.FULL_MATCH;
				} else {
					QueuedLog.debug("mentioned a person after declaring location TEXT");
					matchType = MatchType.NO_MATCH;
				}
				if (nextArgs.length > 1) {
					return new ChannelLocationMatch(matchType, ChannelType.TEXT, nextArgs[0], nextArgs[1]);
				} else {
					return new ChannelLocationMatch(matchType, ChannelType.TEXT, nextArgs[0], null);
				}
			} else {
				throw new IllegalArgumentException("Did not expect matchMention() to report " + mentionTarget.match.name());
			}
		} else if (snowflakeIdPattern.asPredicate().test(nextArgs[0])) {
			if (nextArgs.length < 2) {
				return new ChannelLocationMatch(ChannelType.TEXT, nextArgs[0], null);
			}
			return matchTextChannelBySnowflakeAnd(nextArgs[0], nextArgs[1]);
		} else
			return new ChannelLocationMatch(ChannelType.TEXT, args);
	}

	private static ChannelLocationMatch matchTextChannelBySnowflakeAnd(String snowflake, String args) {
		String[] nextArgs = commandArgsSeperator.split(args,2);
		if (nextArgs.length < 1) {
			return new ChannelLocationMatch(ChannelType.TEXT, snowflake, args);
		} else if (nextArgs.length > 2) {
			throw new MatchOverflowException("Expected two matches got " + nextArgs.length);
		}
		if (nextArgs[0].startsWith("<")) {
			ChannelLocationMatch mentionTarget = matchMention(nextArgs[0]);
			if (mentionTarget.match == MatchType.NO_MATCH)
				return new ChannelLocationMatch(snowflake, args);
			else if (mentionTarget.match == MatchType.FULL_MATCH) {
				if (mentionTarget.expectedPlace == ChannelType.PRIVATE) {
					return new ChannelLocationMatch(ChannelType.TEXT, snowflake, args);
				}
				if (nextArgs.length > 1) {
					return new ChannelLocationMatch(MatchType.FULL_MATCH, ChannelType.TEXT, snowflake, nextArgs[0], nextArgs[1]);
				} else {
					return new ChannelLocationMatch(MatchType.FULL_MATCH, ChannelType.TEXT, snowflake, nextArgs[0], null);
				}
			} else {
				throw new IllegalArgumentException("Did not expect matchMention() to report " + mentionTarget.match.name());
			}
		} else if (snowflakeIdPattern.asPredicate().test(nextArgs[0])) {
			if (nextArgs.length > 1) {
				return new ChannelLocationMatch(MatchType.FULL_MATCH, ChannelType.TEXT, snowflake, nextArgs[0], nextArgs[1]);
			} else {
				return new ChannelLocationMatch(MatchType.MISSING_OPTIONALS, ChannelType.TEXT, snowflake, nextArgs[0], null);
			}
		} else
			return new ChannelLocationMatch(ChannelType.TEXT, snowflake, args);
	}

	/**
	 * Treat the argument as a mention and match its location.
	 * @param raw the raw mention
	 * @return a match indicating the target channel
	 */
	public static ChannelLocationMatch matchMention(String raw) {
		if (raw.startsWith("<")) {
			String mention = raw.substring(1);
			if (mention.startsWith("@")) {
				mention = mention.substring(1);
				if (mention.startsWith("!"))
					mention = mention.substring(1);
				if (!mention.endsWith(">")) {
					QueuedLog.debug("badly terminated mention " + raw);
					return new ChannelLocationMatch(raw);
				}
				mention = mention.substring(0,mention.length() - 1);
				return new ChannelLocationMatch(MatchType.FULL_MATCH,ChannelType.PRIVATE,mention, raw);
			} else if (mention.startsWith("#")) {
				mention = mention.substring(1);
				if (!mention.endsWith(">")) {
					QueuedLog.debug("badly terminated mention " + raw);
					return new ChannelLocationMatch(raw);
				}
				mention = mention.substring(0,mention.length() - 1);
				return new ChannelLocationMatch(mention, raw);
			} else {
				QueuedLog.debug(raw + " doesn't seem to be a mention");
				return new ChannelLocationMatch(raw);
			}
		} else {
			return new ChannelLocationMatch(raw);
		}
	}
}
