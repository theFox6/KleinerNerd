package com.theFox6.kleinerNerd.matchers;

import java.util.regex.Matcher;

import net.dv8tion.jda.api.entities.Message;

public class ChannelStartMatch extends CommandMatch {

	private final String chanID;

	public ChannelStartMatch(MatchType match, String chanID, String args) {
		super(match,args);
		this.chanID = chanID;
	}

	public ChannelStartMatch(MatchType match, String args) {
		super(match,args);
		chanID = null;
	}
	
	/**
	 * Try to find a given command that takes a channel mention as first argument.
	 * @param raw the raw message that might contain the command
	 * @param command the command sequence that should be looked for (will be removed)
	 * @return a match that shows whether the command was found and what arguments were found
	 */
	public static ChannelStartMatch matchCommand(String raw, String command) {
		if (raw.startsWith(command)) {
    		String args = raw.substring((command).length());
    		Matcher chanMatch = Message.MentionType.CHANNEL.getPattern().matcher(args);

    		if (chanMatch.lookingAt()) {
	    		int chanEnd = chanMatch.end(1);
	    		String chanID = args.substring(2,chanEnd);
	    		if (chanEnd == args.length()) {
	    			return new ChannelStartMatch(MatchType.MISSING_PARAMS,chanID,"");
	    		}
	    		String content = args.substring(chanEnd + 2);
	    		return new ChannelStartMatch(MatchType.FULL_MATCH,chanID,content);
    		} else {
    			return new ChannelStartMatch(MatchType.MISSING_PARAMS,args);
    		}
    	} else {
    		return new ChannelStartMatch(MatchType.NO_MATCH,raw);
    	}
	}

	public String getChannelID() {
		return chanID;
	}

}
