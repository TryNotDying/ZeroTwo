package com.TryNotDying.zerotwo.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.zerotwo.Bot;
import com.TryNotDying.zerotwo.audio.AudioHandler;
import com.TryNotDying.zerotwo.audio.RequestMetadata;
import com.TryNotDying.zerotwo.commands.DJCommand; //Import
import com.TryNotDying.zerotwo.commands.MusicCommand;
import com.TryNotDying.zerotwo.utils.FormatUtil;
import com.TryNotDying.zerotwo.utils.SunoURLextractor; //Import
import com.sedmelluq.discord.lavaplayer.track.AudioTrack; //Import
import org.jsoup.Jsoup; //Import
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException; //Import


public class SkipCmd extends MusicCommand {
    public SkipCmd(Bot bot) {
        super(bot);
        this.name = "skip";
        this.help = "votes to skip the current song";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (!handler.isMusicPlaying(event.getJDA())) {
            event.reply(event.getClient().getWarning() + " There is no music playing!");
            return;
        }
        if (!DJCommand.checkDJPermission(event)) {
            event.replyWarning("Only DJs can skip tracks!");
            return;
        }
        AudioTrack track = handler.getPlayer().getPlayingTrack();
        String title = getCorrectedTitle(track);
        handler.getPlayer().stopTrack();
        event.replySuccess("Skipped **" + title + "**.");
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
}