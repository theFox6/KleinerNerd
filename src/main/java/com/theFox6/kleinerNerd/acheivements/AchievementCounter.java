package com.theFox6.kleinerNerd.acheivements;

import com.theFox6.kleinerNerd.data.ResourceNotFoundException;
import com.theFox6.kleinerNerd.data.SendableImage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class AchievementCounter {
    public void sendAchievement(MessageChannel chan, User receiver, SendableImage image, String title, String description) {
        EmbedBuilder achievement = new EmbedBuilder();
        achievement.setAuthor(receiver.getName(), receiver.getEffectiveAvatarUrl());
        achievement.setTitle(title).setDescription(description);
        achievement.setImage(image.link());
        if (image.needsAttach()) {
            try {
                chan.sendFile(image.data(), image.name()).embed(achievement.build()).queue();
            } catch (ResourceNotFoundException e) {
                QueuedLog.error("achievement image not found", e);
                chan.sendMessage(achievement.build()).queue();
            }
        } else
            chan.sendMessage(achievement.build()).queue();
    }
}
