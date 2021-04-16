package com.theFox6.kleinerNerd.reactionRoles;

import com.theFox6.kleinerNerd.data.MessageLocation;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public class RRConfigurator {
    public static enum RRConfState {
        Channel,
        Message,
        Role,
        Emote,
        Configured,
        Error;
    }

    private RRConfState state;
    private String channel;
    private MessageLocation msg;
    private String roleId;
    private String emoteId;

    public RRConfigurator(TextChannel chan) {
        state = RRConfState.Channel;
        chan.sendMessage("Welcher Kanal?").queue();
    }

    public boolean handle(String raw, TextChannel chan, Message msg) {
        switch (state) {
            case Channel:
                List<TextChannel> chans = msg.getMentionedChannels();
                if (chans.isEmpty()) {
                    //check whether name or id was given
                   channel = raw;
                } else {
                    channel = chans.get(0).getId();
                }
                state = RRConfState.Message;
                chan.sendMessage("Welche Nachricht (Nachrichten-ID)?").queue();
                break;
            case Message:
                Guild guild = msg.getGuild();
                guild.getTextChannelById(channel).retrieveMessageById(Long.parseLong(raw)).queue((m) -> {
                    this.msg = new MessageLocation(m);
                    state = RRConfState.Role;
                    chan.sendMessage("Welche Rolle?").queue();
                }, (err) -> {
                    QueuedLog.warning("Couldn't retrieve message",err);
                    chan.sendMessage("Konnte nachricht nicht finden, breche ab.").queue();
                    state = RRConfState.Error;
                });
                break;
            case Role:
                //check whether name or mention or id
                roleId = msg.getGuild().getRoleById(raw).getId();
                state = RRConfState.Emote;
                chan.sendMessage("Welches Emote?").queue();
                break;
            case Emote:
                List<Emote> emotes = msg.getEmotes();
                Emote emote = emotes.get(0);
                emoteId = emote.getName() + ":" + emote.getId();
                ReactionRoleStorage.getOrCreateConfig(this.msg).addReactionRole(emoteId,roleId);
                state = RRConfState.Configured;
                chan.sendMessage("Konfiguration beendet.").queue();
                return true;
            default:
                QueuedLog.error("unhandled configuration state: " + state.name());
        }
        return false;
    }
}
