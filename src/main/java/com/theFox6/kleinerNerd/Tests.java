package com.theFox6.kleinerNerd;

import java.io.File;
import java.net.URL;

import com.theFox6.kleinerNerd.matchers.ChannelStartMatch;

public class Tests {
	
	public static void testResourceToken() {
		URL token = KleinerNerd.class.getClassLoader().getResource("token.txt");
		if (token == null) {
			System.out.println("I haz no token.");
			return;
		}

		File file = new File(token.getFile());
		if (file.exists()) {
			System.out.println("I haz a token.");
		} else {
			System.out.println("I haz no-exist token.");
		}
	}
	
	public static void testMentionMatcher() {
		String raw = "ff.chanstart <#4589347895347875> test test and test";
		String chanstartCommand = KleinerNerd.prefix + "chanstart ";
		ChannelStartMatch chanMatch = ChannelStartMatch.matchCommand(raw,chanstartCommand);
		
		if (chanMatch.isFullMatch()) {
    		System.out.println("channel ID mentioned:\n"+chanMatch.getChannelID());
    		System.out.println("message after channel mention:\n" + chanMatch.getArgs());
		} else {
			System.out.println("no channel at start, got args:\n" + chanMatch.getArgs());
		}
	}
	
	public static void main(String[] args) {
		//System.out.println("Starting tests");
		testResourceToken();
		testMentionMatcher();
		String t = "test";
		System.out.println("testing".substring(t.length()));
	}

}
