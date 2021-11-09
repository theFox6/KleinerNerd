package com.theFox6.kleinerNerd.achievements;

import com.theFox6.kleinerNerd.data.ResourceNotFoundException;
import com.theFox6.kleinerNerd.data.SendableImage;
import com.theFox6.kleinerNerd.storage.CounterStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementCounter {
    private static Map<String, Map<Integer, Achievement>> achievements = new ConcurrentHashMap<>();

    public static void registerAchievements(String name, Map<Integer, Achievement> thresholds) {
        achievements.put(name, thresholds);
    }

    public static void increment(String key, User user) {
        Map<Integer, Achievement> thresholds = achievements.get(key);
        int count = CounterStorage.getUserCounter("achievement_"+key, user.getId()).incrementAndGet();
        if (thresholds == null)
            QueuedLog.warning("No achievements for " + key + " registered.");
        else {
            Achievement a = thresholds.get(count);
            if (a != null)
                sendAchievement(user, a);
        }
    }

    public static void sendAchievement(User user, Achievement a) {
        QueuedLog.verbose(user.getName() + " reached achievement threshold for \"" + a.title + '"');
        //can't send achievements to yourself
        if (user.getJDA().getSelfUser().equals(user))
            return;

        user.openPrivateChannel().queue((pc) -> sendAchievement(pc, user, a.image, a.title, a.desc),
                (e) -> QueuedLog.error("Couldn't open PrivateChannel to send achievement to " + user.getName(),e));
    }

    public static void sendAchievement(MessageChannel channel, User receiver, SendableImage image, String title, String description) {
        EmbedBuilder achievement = new EmbedBuilder();
        achievement.setFooter(receiver.getName(), receiver.getEffectiveAvatarUrl());
        achievement.setTitle(title).setDescription(description).setTimestamp(Instant.now());
        if (image == null) {
            QueuedLog.debug("achievement "+title+" has no image");
        } else {
            //achievement.setAuthor("Errungenschaft",null, image.link());
            achievement.setThumbnail(image.link());
            if (image.needsAttach()) {
                try {
                    channel.sendFile(image.data(), image.name()).embed(achievement.build()).queue(
                            (m) -> QueuedLog.debug(receiver.getName() + " received the achievement \"" + title + '"'),
                            (e) -> QueuedLog.error("couldn't send achievement", e)
                    );
                    return;
                } catch (ResourceNotFoundException e) {
                    QueuedLog.error("achievement image not found", e);
                }
            }
        }
        channel.sendMessage(achievement.build()).queue(
                (m) -> QueuedLog.debug(receiver.getName() + " received the achievement \"" + title + '"'),
                (e) -> QueuedLog.error("couldn't send achievement", e)
        );
    }

    public static void decrement(String key, User user) {
        CounterStorage.getUserCounter("achievement_"+key, user.getId()).decrementAndGet();
    }
}
