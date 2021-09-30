package com.theFox6.kleinerNerd.achievements;

import com.theFox6.kleinerNerd.data.SendableImage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmoteAchievementListener {
    public EmoteAchievementListener() {
        // New file format and parser incoming?

        String ehrenReact = " Personen haben auf Nachrichten von dir mit Ehre reagiert.";
        Map<Integer,Achievement> ehre = new LinkedHashMap<>();
        ehre.put(5, new Achievement("eine gute Tat", "5" + ehrenReact,
                new SendableImage("/achievements/thumbsup.png","thumbsup.png")));
        ehre.put(10, new Achievement("Helferlein", "10" + ehrenReact,
                new SendableImage("/achievements/bulb.png","bulb.png")));
        ehre.put(25, new Achievement("Ehrenmann/frau", "Du bist ein Ehrenmann/eine Ehrenfrau!\n" +
                "25" + ehrenReact, new SendableImage("/achievements/Ehre.png","Ehre.png")));
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
    public void onReactionAdded(GuildMessageReactionAddEvent ev) {
        if (ev.getReactionEmote().getName().equalsIgnoreCase("ehre")) {
            ev.getChannel().retrieveMessageById(ev.getMessageId()).queue((o) -> {
                User author = o.getAuthor();
                if (ev.getUserId().equals(author.getId()))
                    return;
                QueuedLog.verbose("Ehre " + author.getId());
                AchievementCounter.increment("Ehre", author);
            }, (err) -> QueuedLog.error("could not retrieve message, that was reacted to",err));
        }
    }

    @SubscribeEvent
    public void onReactionRemoved(GuildMessageReactionRemoveEvent ev) {
        if (ev.getReactionEmote().getName().equalsIgnoreCase("ehre")) {
            ev.getChannel().retrieveMessageById(ev.getMessageId()).queue((o) -> {
                User author = o.getAuthor();
                if (ev.getUserId().equals(author.getId()))
                    return;

                AchievementCounter.decrement("Ehre",o.getAuthor());
            }, (err) -> QueuedLog.error("could not retrieve message, that was reacted to",err));
        }
    }
}
