package com.TryNotDying.zerotwo.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.zerotwo.Bot;
import com.TryNotDying.zerotwo.audio.AudioHandler;
import com.TryNotDying.zerotwo.audio.RequestMetadata;
import com.TryNotDying.zerotwo.commands.DJCommand;
import com.TryNotDying.zerotwo.commands.MusicCommand;
import com.TryNotDying.zerotwo.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeekCmd extends MusicCommand
{
    private final static Logger LOG = LoggerFactory.getLogger("Seeking");
    
    public SeekCmd(Bot bot)
    {
        super(bot);
        this.name = "seek";
        this.help = "seeks the current song";
        this.arguments = "[+ | -] <HH:MM:SS | MM:SS | SS>|<0h0m0s | 0m0s | 0s>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event)
    {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        AudioTrack playingTrack = handler.getPlayer().getPlayingTrack();
        if (!playingTrack.isSeekable())
        {
            event.replyError("This track is not seekable.");
            return;
        }


        if (!DJCommand.checkDJPermission(event) && playingTrack.getUserData(RequestMetadata.class).getOwner() != event.getAuthor().getIdLong())
        {
            event.replyError("You cannot seek **" + playingTrack.getInfo().title + "** because you didn't add it!");
            return;
        }

        String args = event.getArgs();
        TimeUtil.SeekTime seekTime = TimeUtil.parseTime(args);
        if (seekTime == null)
        {
            event.replyError("Invalid seek! Expected format: " + arguments + "\nExamples: `1:02:23` `+1:10` `-90`, `1h10m`, `+90s`");
            return;
        }

        long currentPosition = playingTrack.getPosition();
        long trackDuration = playingTrack.getDuration();

        long seekMilliseconds = seekTime.relative ? currentPosition + seekTime.milliseconds : seekTime.milliseconds;
        if (seekMilliseconds > trackDuration)
        {
            event.replyError("Cannot seek to `" + TimeUtil.formatTime(seekMilliseconds) + "` because the current track is `" + TimeUtil.formatTime(trackDuration) + "` long!");
            return;
        }
        
        try
        {
            playingTrack.setPosition(seekMilliseconds);
        }
        catch (Exception e)
        {
            event.replyError("An error occurred while trying to seek: " + e.getMessage());
            LOG.warn("Failed to seek track " + playingTrack.getIdentifier(), e);
            return;
        }
        event.replySuccess("Successfully seeked to `" + TimeUtil.formatTime(playingTrack.getPosition()) + "/" + TimeUtil.formatTime(playingTrack.getDuration()) + "`!");
    }
}
