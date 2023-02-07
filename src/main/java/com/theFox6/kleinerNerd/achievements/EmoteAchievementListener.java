package com.theFox6.kleinerNerd.achievements;

import com.theFox6.kleinerNerd.data.SendableImage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EmoteAchievementListener {
    public EmoteAchievementListener() {
        //TODO: get roles by counting on servers separately
        // New file format and parser incoming?

        String ehrenReact = " Personen haben auf Nachrichten von dir mit Ehre reagiert.";
        Map<Integer,Achievement> ehre = new LinkedHashMap<>();
        ehre.put(5, new Achievement("eine gute Tat", "5" + ehrenReact,
                new SendableImage("/achievements/thumbsup.png","thumbsup.png")));
        ehre.put(10, new Achievement("Helferlein", "10" + ehrenReact,
                new SendableImage("/achievements/bulb.png","bulb.png")));
        ehre.put(25, new Achievement("Ehrenmann/frau", "Du bist ein Ehrenmann/eine Ehrenfrau!\n" +
                "25" + ehrenReact, new SendableImage("/achievements/Ehre.png","Ehre.png"), "designierter Ehrenmann"));
        ehre.put(50, new Achievement("gute Arbeit", "Gute Arbeit!\n" +
                "50" + ehrenReact, new SendableImage("/achievements/clap.png","clap.png")));
        ehre.put(100, new Achievement("wahrer Unterstützer", "Du bist ein großartiger Unterstützer!\n" +
                "100" + ehrenReact, new SendableImage("/achievements/Reward-Badge.png", "Reward-Badge.png")));
        ehre.put(200, new Achievement("ein wahrer Gott", "Du bist wie ein Gott!\n" +
                "200" + ehrenReact, new SendableImage("/achievements/angel-icon.png","angel-icon.png")));
        ehre.put(500, new Achievement("Unterstützer ohne Grenzen", "Du bist nicht aufzuhalten!\n" +
                "500" + ehrenReact, new SendableImage("/achievements/Ehre.png","Ehre.png")));
        AchievementCounter.registerAchievements("Ehre", ehre);
    }

    @SubscribeEvent
    public void onReactionAdded(MessageReactionAddEvent ev) {
        if (ev.getEmoji().getName().equalsIgnoreCase("ehre")) {
            ev.getChannel().retrieveMessageById(ev.getMessageId()).queue((o) -> {
                User author = o.getAuthor();
                if (ev.getUserId().equals(author.getId()))
                    return;
                //QueuedLog.verbose("Ehre " + author.getId());
                getOrRetrieveMember(o, (m) -> AchievementCounter.increment("Ehre", author, m, o.getGuild()));
            }, (err) -> QueuedLog.error("could not retrieve message, that was reacted to",err));
        }
    }

    @SubscribeEvent
    public void onReactionRemoved(MessageReactionRemoveEvent ev) {
        if (ev.getEmoji().getName().equalsIgnoreCase("ehre")) {
            ev.getChannel().retrieveMessageById(ev.getMessageId()).queue((o) -> {
                User author = o.getAuthor();
                if (ev.getUserId().equals(author.getId()))
                    return;

                AchievementCounter.decrement("Ehre",o.getAuthor());
            }, (err) -> QueuedLog.error("could not retrieve message, that was reacted to",err));
        }
    }

    public static void getOrRetrieveMember(Message msg, Consumer<? super Member> success) {
        Member m = msg.getMember();
        if (m != null) {
            success.accept(m);
            return;
        }
        if (!msg.isFromGuild())
            return; //TODO: add a fail consumer, that receives an error here
        Guild g = msg.getGuild();
        User author = msg.getAuthor();
        m = g.getMember(author);
        if (m != null) {
            success.accept(m);
            return;
        }
        g.retrieveMember(author).queue(success, (e) -> QueuedLog.error("retrieving member failed", e));
    }
}
