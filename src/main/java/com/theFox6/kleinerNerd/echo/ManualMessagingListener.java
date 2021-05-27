package com.theFox6.kleinerNerd.echo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.theFox6.kleinerNerd.ChannelRetrievalException;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.data.MessageLocation;
import com.theFox6.kleinerNerd.echo.faults.InvalidTargetPlaceException;
import com.theFox6.kleinerNerd.listeners.HelpListener;
import com.theFox6.kleinerNerd.matchers.ChannelLocationMatch;
import com.theFox6.kleinerNerd.storage.ConfigFiles;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class ManualMessagingListener {
	public static final String echoCommand = KleinerNerd.prefix + "echo ";
	
	private Map<MessageLocation,PreparedEcho> echoQueue = new ConcurrentHashMap<>();
	
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	if (raw.startsWith(echoCommand)) {
    		User author = msg.getAuthor();
    		if (!ConfigFiles.getOwners().contains(author.getId())) {
    			//chan.sendMessage("This command is restricted to be used by my owner only.").queue();
    			QueuedLog.action("cought "+ author.getDiscriminator() + " trying to use the echo command");
    			return;
    		}
    		
    		String args = raw.substring(echoCommand.length());
    		System.out.println(args);
    		ChannelLocationMatch loc = ChannelLocationMatch.matchLocation(args);
    		if (!loc.isValidMatch()) {
    			//TODO: send the reason why it is invalid
    			chan.sendMessage("Please provide a valid location. For help about the format use `" + HelpListener.helpCommand + " location format`.").queue();
    			return;
    		}
    		
    		JDA jda = event.getJDA();
    		PreparedEcho echoMessage = new PreparedEcho(jda,loc,author);
    		
    		if (echoMessage.isInvalid()) {
    			QueuedLog.warning("newly created PreparedEcho invalid without exception");
    			chan.sendMessage("I failed to prepare the message you wanted me to send.\n"
    					+ "This fault seems to originate from some kind of mess my developer made.").queue();
				return;
			}
    		
    		try {
				echoMessage.retrieveChannel();
			} catch (ChannelRetrievalException e) {
				QueuedLog.debug(e.getMessage());
				chan.sendMessage("I " + e.getMessage() + "!").queue();
				return;
			} catch (InvalidTargetPlaceException e) {
				QueuedLog.warning("invalid target place on retrieveChannel", e);
				chan.sendMessage("I couldn't retrieve the target place from the given arguments,\n"
						+ "because I " + e.getMessage() + ".\n"
						+ "This kind of fault means that either you or my developer messed up.\n"
						+ "Please provide more info about the target place next time.\n"
						+ "See `" + HelpListener.helpCommand + " location format` for help about providing a target location."
						).queue();
				return;
			}
    		
    		if (echoMessage.isInvalid()) {
    			QueuedLog.warning("retrieved PreparedEcho invalid without exception");
    			chan.sendMessage("The provided location seems to be invalid, "
    					+ "but somehow I can't tell why.\n"
    					+ "Something must have gone badly wrong.").queue();
    			return;
			}
    		
    		if (loc.isFullMatch()) {
    			echoMessage.send((sent) -> msg.addReaction("U+1F4E8").queue());
    		} else {
    			echoMessage.sendConfirmEmbed(chan,
    					(confirm) -> echoQueue.put(new MessageLocation(confirm),echoMessage));
    		}
    	}
    }
	
	@SubscribeEvent
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		String reacter = event.getUserId();
		if (reacter.equals(event.getJDA().getSelfUser().getId()))
			return;
		PreparedEcho echo = echoQueue.get(new MessageLocation(event));
		if (echo == null)
			return;
		if (!echo.getAuthorId().equals(reacter))
			QueuedLog.debug(reacter + " reacted to an echo confirmation for somebody else.");
		ReactionEmote react = event.getReactionEmote();
		if (!react.isEmoji()) {
			QueuedLog.debug(event.getUser().getAsTag() + " reacted to an echo confirmation with emote " + react.getName());
			return;
		}
		String codePoint = react.getAsCodepoints();
		MessageChannel chan = event.getChannel();
		String msgId = event.getMessageId();
		if (codePoint.equals("U+2611")) {
			QueuedLog.info("echo confirmed");
			echo.send((sent) -> {
				echoQueue.remove(msgId,echo);
				chan.addReactionById(msgId,"U+1f4e8").queue();
				chan.removeReactionById(msgId,"U+274c").queue();
			});
		} else if (codePoint.equals("U+274c")) {
			QueuedLog.info("echo rejected");
			echoQueue.remove(msgId,echo);
			chan.removeReactionById(msgId,"U+2611").queue();
		} else {
			QueuedLog.debug(event.getUser().getAsTag() + " reacted to an echo confirmation with emoji " + codePoint);
		}
	}
}
