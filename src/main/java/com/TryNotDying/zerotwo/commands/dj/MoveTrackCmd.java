package com.TryNotDying.zerotwo.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.zerotwo.Bot;
import com.TryNotDying.zerotwo.audio.AudioHandler;
import com.TryNotDying.zerotwo.audio.QueuedTrack;
import com.TryNotDying.zerotwo.audio.RequestMetadata;
import com.TryNotDying.zerotwo.commands.DJCommand;
import com.TryNotDying.zerotwo.queue.AbstractQueue;
import com.TryNotDying.zerotwo.utils.SunoURLextractor;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class MoveTrackCmd extends DJCommand {

    public MoveTrackCmd(Bot bot) {
        super(bot);
        this.name = "movetrack";
        this.help = "move a track in the current queue to a different position";
        this.arguments = "<from> <to>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        int from;
        int to;

        String[] parts = event.getArgs().split("\\s+", 2);
        if (parts.length < 2) {
            event.replyError("Please include two valid indexes.");
            return;
        }

        try {
            from = Integer.parseInt(parts[0]);
            to = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            event.replyError("Please provide two valid indexes.");
            return;
        }

        if (from == to) {
            event.replyError("Can't move a track to the same position.");
            return;
        }

        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        AbstractQueue<QueuedTrack> queue = handler.getQueue();
        if (isUnavailablePosition(queue, from)) {
            String reply = String.format("`%d` is not a valid position in the queue!", from);
            event.replyError(reply);
            return;
        }
        if (isUnavailablePosition(queue, to)) {
            String reply = String.format("`%d` is not a valid position in the queue!", to);
            event.replyError(reply);
            return;
        }

        QueuedTrack track = queue.moveItem(from - 1, to - 1);
        String trackTitle = getCorrectedTitle(track.getTrack());
        String reply = String.format("Moved **%s** from position `%d` to `%d`.", trackTitle, from, to);
        event.replySuccess(reply);
    }

    private static boolean isUnavailablePosition(AbstractQueue<QueuedTrack> queue, int position) {
        return (position < 1 || position > queue.size());
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