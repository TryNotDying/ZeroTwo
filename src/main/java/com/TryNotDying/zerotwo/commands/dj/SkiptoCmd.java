package com.TryNotDying.zerotwo.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.zerotwo.Bot;
import com.TryNotDying.zerotwo.audio.AudioHandler;
import com.TryNotDying.zerotwo.audio.RequestMetadata;
import com.TryNotDying.zerotwo.commands.DJCommand;
import com.TryNotDying.zerotwo.utils.FormatUtil;
import com.TryNotDying.zerotwo.utils.SunoURLextractor;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class SkiptoCmd extends DJCommand {
    public SkiptoCmd(Bot bot) {
        super(bot);
        this.name = "skipto";
        this.help = "skips to the specified song";
        this.arguments = "<position>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        int index = 0;
        try {
            index = Integer.parseInt(event.getArgs());
        } catch (NumberFormatException e) {
            event.reply(event.getClient().getError() + " `" + event.getArgs() + "` is not a valid integer!");
            return;
        }
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (index < 1 || index > handler.getQueue().size()) {
            event.reply(event.getClient().getError() + " Position must be a valid integer between 1 and " + handler.getQueue().size() + "!");
            return;
        }
        handler.getQueue().skip(index - 1);
        AudioTrack track = handler.getQueue().get(0).getTrack();
        String title = getCorrectedTitle(track);
        event.reply(event.getClient().getSuccess() + " Skipped to **" + (title != null ? title : track.getInfo().title) + "**");
        handler.getPlayer().stopTrack();
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
                //Consider adding a more robust way of handling the exception, such as returning a default value.
            }
        }
        return track.getInfo().title;
    }
}