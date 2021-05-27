package com.theFox6.kleinerNerd.logging;

import com.theFox6.kleinerNerd.data.MessageChannelLocation;
import com.theFox6.kleinerNerd.data.MessageLocation;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LoggedVoiceEvent {
    private VoiceAction action;
    private String channelId;
    private Collection<MessageLocation> recentlySentMessages;
    private static final TemporalAmount expiryTime = Duration.ofSeconds(20);

    public LoggedVoiceEvent(VoiceAction action, VoiceChannel channel) {
        this.action = action;
        this.channelId = channel.getId();
        this.recentlySentMessages = new ConcurrentLinkedDeque<>();
    }

    public boolean wasChannel(VoiceChannel vc) {
        return channelId.equals(vc.getId());
    }

    public VoiceAction getAction() {
        return action;
    }

    public boolean isExpired() {
        return recentlySentMessages.isEmpty();
    }

    public void sendMessage(JDA jda, MessageChannelLocation loc, String msg) {
        loc.fetchChannel(jda,
            (chan) -> chan.sendMessage(msg).queue(
                (sm) -> recentlySentMessages.add(new MessageLocation(sm)),
                (e) -> QueuedLog.error("could not send voice logging message", e)
            ),
            (e) -> QueuedLog.error("could not fetch channel for voice logging", e)
        );
    }

    public void editOrSend(JDA jda, Iterable<MessageChannelLocation> targets, BiConsumer<MessageChannelLocation, Runnable> check, String editedMessage, String newMessage) {
        targets.forEach((loc) -> check.accept(loc, () -> {
            AtomicBoolean unprocessed = new AtomicBoolean(true);
            recentlySentMessages.stream().filter((ml) -> ml.location().equals(loc)).forEach((ml) -> {
                Consumer<? super Throwable> onFail = (e) -> {
                    QueuedLog.warning("Could not edit voice logging message", e);
                    recentlySentMessages.remove(ml);
                    sendMessage(jda, loc, newMessage);
                };
                ml.fetchMessage(jda, (msg) -> {
                    if (msg.getTimeCreated().isAfter(OffsetDateTime.now().minus(expiryTime))) {
                        msg.editMessage(editedMessage).queue((em) -> {}, onFail);
                    } else {
                        recentlySentMessages.remove(ml);
                        sendMessage(jda,loc,newMessage);
                    }
                }, onFail);
                unprocessed.set(false);
            });
            if (unprocessed.get()) {
                sendMessage(jda, loc, newMessage);
            }
        }));
    }

    public void updateAction(VoiceAction action) {
        this.action = action;
    }
}
