package com.TryNotDying.zerotwo.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.zerotwo.Bot;
import com.TryNotDying.zerotwo.audio.AudioHandler;
import com.TryNotDying.zerotwo.audio.RequestMetadata;
import com.TryNotDying.zerotwo.commands.DJCommand;
import com.TryNotDying.zerotwo.commands.MusicCommand;
import com.TryNotDying.zerotwo.utils.FormatUtil;
import com.TryNotDying.zerotwo.utils.SunoURLextractor;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class ForceskipCmd extends DJCommand {
    public ForceskipCmd(Bot bot) {
        super(bot);
        this.name = "forceskip";
        this.help = "skips the current song";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;
    }

    private String getCorrectedTitle(AudioTrack track) {
        RequestMetadata rm = track.getUserData(RequestMetadata.class);
        if (rm != null && rm.title != null) {
            return rm.title;
        } else if (track.getInfo().uri.startsWith("https://cdn1.suno.ai/")) {
            try {
                String sunoUrl = SunoURLextractor.reconstructSunoUrl(track.getInfo().uri);
                if (sunoUrl != null) {
                    Document doc = Jsoup.connect(sunoUrl).get();
                    Elements titleTags = doc.select("meta[property=og:title]");
                    if (!titleTags.isEmpty()) {
                        return titleTags.first().attr("content");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error fetching Suno title: " + e.getMessage());
            }
        }
        return track.getInfo().title;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        RequestMetadata rm = handler.getRequestMetadata();
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        String title = getCorrectedTitle(track);

        event.reply(event.getClient().getSuccess() + " Skipped **" + (title != null ? title : track.getInfo().title)
                + "** " + (rm.getOwner() == 0L ? "(autoplay)" : "(requested by **" + FormatUtil.formatUsername(rm.user) + "**)") );
        handler.getPlayer().stopTrack();
    }
}