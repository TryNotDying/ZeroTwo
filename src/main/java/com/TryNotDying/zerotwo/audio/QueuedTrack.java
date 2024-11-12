package com.TryNotDying.zerotwo.audio;

import com.TryNotDying.zerotwo.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.TryNotDying.zerotwo.queue.Queueable;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author TryNotDying < >
 */
public class QueuedTrack implements Queueable
{
    private final AudioTrack track;
    private final RequestMetadata requestMetadata;

    public QueuedTrack(AudioTrack track, RequestMetadata rm)
    {
        this.track = track;
        this.track.setUserData(rm == null ? RequestMetadata.EMPTY : rm);

        this.requestMetadata = rm;
        if (this.track.isSeekable() && rm != null)
            track.setPosition(rm.requestInfo.startTimestamp);
    }
    
    @Override
    public long getIdentifier() 
    {
        return requestMetadata.getOwner();
    }
    
    public AudioTrack getTrack()
    {
        return track;
    }

    public RequestMetadata getRequestMetadata()
    {
        return requestMetadata;
    }

    @Override
    public String toString() 
    {
        String entry = "`[" + TimeUtil.formatTime(track.getDuration()) + "]` ";
        AudioTrackInfo trackInfo = track.getInfo();
        entry = entry + (trackInfo.uri.startsWith("http") ? "[**" + trackInfo.title + "**]("+trackInfo.uri+")" : "**" + trackInfo.title + "**");
        return entry + " - <@" + track.getUserData(RequestMetadata.class).getOwner() + ">";
    }
}
