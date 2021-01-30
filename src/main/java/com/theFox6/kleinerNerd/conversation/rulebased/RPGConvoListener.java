package com.theFox6.kleinerNerd.conversation.rulebased;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.KleinerNerd;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class RPGConvoListener {
	private static final List<String> rpgRumors = Arrays.asList(
			"Some elves were seen wandering towards Grugnil.",
			"Belsar claims to have slain an Ork.",
			"Wandering Orks have attacked a caravan of Goats.");
	
	public static final Function<Message,ExpectedStatement.Action> onInvalid = (msg) -> {
		msg.getChannel().sendMessage("Please choose a valid option").queue();
		return ExpectedStatement.Action.STAY;
	};
	
	private static final ExpectedStatement rpgConvo =
			new ExpectedStatement(KleinerNerd.prefix + "rpgconvo", (msg) -> {
					msg.getChannel().sendMessage("Welcome to the city of Dusvengard.\n"
						+ "I am the city guard Garnil. How can I help you?\n\n"
						+ "0 - Bye. (leave)\n"
						+ "1 - What is new?\n"
						+ "2 - What do you know about Orks?").queue();
					return ExpectedStatement.Action.STAY;
				}
			)
			.addNext(
					new ExpectedStatement("0", (msg) -> {
						msg.getChannel().sendMessage("Bye!").queue();
						return ExpectedStatement.Action.QUIT;
					}))
			.addNext(
					new ExpectedStatement("1", (msg) -> {
						msg.getChannel().sendMessage(KNHelpers.randomElement(rpgRumors) 
								+ "\n0 - OK, bye. (leave)\n"
								+ "1 - What else is new?\n"
								+ "2 - What do you know about Orks?").queue();
						return ExpectedStatement.Action.RETURN;
					}))
			.addNext(
					new ExpectedStatement("2", (msg) -> {
						msg.getChannel().sendMessage("Those beasts keep attacking caravans.\n\n"
								+ "0 - How terrible!\n"
								+ "1 - Oh no!\n"
								+ "2 - What to do?").queue();
						return ExpectedStatement.Action.STAY;
					}).setUnexpected(onInvalid)
					.addNext(
							new ExpectedStatement("[0-2]", true, (msg) -> {
								msg.getChannel().sendMessage("It would be appreciated if you helped.\n\n"
										+ "0 - Of course I'll help!\n"
										+ "1 - That sounds a bit too dangerous for me.").queue();
								return ExpectedStatement.Action.STAY;
							}).setUnexpected(onInvalid)
							.addNext(
									new ExpectedStatement("0", (msg) -> {
										msg.getChannel().sendMessage("Good luck. Come back when you're done.").queue();
										return ExpectedStatement.Action.QUIT;
									}))
							.addNext(
									new ExpectedStatement("1", (msg) -> {
										msg.getChannel().sendMessage("Whatever. Come back when you changed your mind.").queue();
										return ExpectedStatement.Action.QUIT;
									}))
					)
				);
	
	private Map<String,ExpectedStatement> startedConvos = new LinkedHashMap<>();
	
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	String chanId = msg.getChannel().getId();
    	if (msg.getAuthor().equals(event.getJDA().getSelfUser()))
    		return;
    	if (rpgConvo.rawMatches(raw)) {
    		rpgConvo.advancedTo(msg);
    		startedConvos.put(chanId, rpgConvo);
    	} else if (startedConvos.containsKey(chanId)) {
    		ExpectedStatement s = startedConvos.get(chanId);
    		if (s != null) {
    			startedConvos.put(chanId, s.advance(msg));
    		}
    	}
	}
}
