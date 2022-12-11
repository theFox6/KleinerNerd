package com.theFox6.kleinerNerd.achievements;

import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.data.SendableImage;
import com.theFox6.kleinerNerd.storage.CounterStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AchievementCounter {
    private static final Map<String, Map<Integer, Achievement>> achievements = new ConcurrentHashMap<>();

    public static void registerAchievements(String name, Map<Integer, Achievement> thresholds) {
        achievements.put(name, thresholds);
    }

    public static void increment(String key, User user, Member member, Guild guild) {
        Map<Integer, Achievement> thresholds = achievements.get(key);
        int count = CounterStorage.getUserCounter("achievement_"+key, user.getId()).incrementAndGet();
        if (thresholds == null)
            QueuedLog.warning("No achievements for " + key + " registered.");
        else {
            Achievement a = thresholds.get(count);
            if (a != null)
                sendAchievement(user, a, member, guild);
        }
    }

    public static void sendAchievement(User user, Achievement a, Member member, Guild guild) {
        QueuedLog.verbose(user.getName() + " reached achievement threshold for \"" + a.title + '"');
        //can't send achievements to yourself
        if (user.getJDA().getSelfUser().equals(user))
            return;

        if (member != null) {
            List<Role> achievementRoles = guild.getRolesByName(a.roleName, true);
            if (achievementRoles.size() > 1)
                ModLog.sendToGuild(guild, "Mehr als eine " + a.roleName + " Rolle!");
            if (!achievementRoles.isEmpty())
                guild.addRoleToMember(member, achievementRoles.get(0)).reason("Achievement").queue(
                        (s) -> QueuedLog.action(member.getEffectiveName() + " earned " + a.roleName + " role."),
                        (e) -> QueuedLog.error("Couldn't assign achievement role " + a.roleName + " to " + user.getName(),e)
                );
        }
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
                channel.sendFiles(image.upload()).setEmbeds(achievement.build()).queue(
                        (m) -> QueuedLog.debug(receiver.getName() + " received the achievement \"" + title + '"'),
                        (e) -> QueuedLog.error("couldn't send achievement", e)
                );
                return;
            }
        }
        channel.sendMessageEmbeds(achievement.build()).queue(
                (m) -> QueuedLog.debug(receiver.getName() + " received the achievement \"" + title + '"'),
                (e) -> QueuedLog.error("couldn't send achievement", e)
        );
    }

    public static void decrement(String key, User user) {
        CounterStorage.getUserCounter("achievement_"+key, user.getId()).decrementAndGet();
    }
}
