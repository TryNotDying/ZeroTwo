package com.TryNotDying.ZeroTwo.commands.music;

import com.TryNotDying.ZeroTwo.audio.RequestMetadata;
import com.TryNotDying.ZeroTwo.utils.TimeUtil;
import com.TryNotDying.ZeroTwo.utils.SunoURLextractor;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.TryNotDying.ZeroTwo.Bot;
import com.TryNotDying.ZeroTwo.audio.AudioHandler;
import com.TryNotDying.ZeroTwo.audio.QueuedTrack;
import com.TryNotDying.ZeroTwo.commands.DJCommand;
import com.TryNotDying.ZeroTwo.commands.MusicCommand;
import com.TryNotDying.ZeroTwo.playlist.PlaylistLoader.Playlist;
import com.TryNotDying.ZeroTwo.utils.FormatUtil;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

public class PlayCmd extends MusicCommand {
    private final static String LOAD = "\uD83D\uDCE5"; // 📥
    private final static String CANCEL = "\uD83D\uDEAB"; // 🚫

    private final String loadingEmoji;

    public PlayCmd(Bot bot) {
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "play";
        this.arguments = "<title|URL|subcommand>";
        this.help = "plays the provided song";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
        this.children = new Command[]{new PlaylistCmd(bot)};
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (event.getArgs().isEmpty() && event.getMessage().getAttachments().isEmpty()) {
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (handler.getPlayer().getPlayingTrack() != null && handler.getPlayer().isPaused()) {
                if (DJCommand.checkDJPermission(event)) {
                    handler.getPlayer().setPaused(false);
                    event.replySuccess("Resumed **" + handler.getPlayer().getPlayingTrack().getInfo().title + "**.");
                } else {
                    event.replyError("Only DJs can unpause the player!");
                }
                return;
            }
            StringBuilder builder = new StringBuilder(event.getClient().getWarning() + " Play Commands:\n");
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" <song title>` - plays the first result from Youtube");
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" <URL>` - plays the provided song, playlist, or stream");
            for (Command cmd : children) {
                builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName()).append(" ").append(cmd.getArguments()).append("` - ").append(cmd.getHelp());
            }
            event.reply(builder.toString());
            return;
        }
        String args = event.getArgs().startsWith("<") && event.getArgs().endsWith(">")
                ? event.getArgs().substring(1, event.getArgs().length() - 1)
                : event.getArgs().isEmpty() ? event.getMessage().getAttachments().get(0).getUrl() : event.getArgs();

        try {
            if (args.startsWith("https://suno.com/")) {
                handleSunoUrl(event, args);
            } else {
                handleYoutubeUrl(event, args);
            }
        } catch (IOException e) {
            event.replyError("Error loading track: " + e.getMessage());
        }
    }

    private void handleSunoUrl(CommandEvent event, String args) {
        try {
            List<Map<String, String>> sunoUrls = SunoURLextractor.extractSunoAudioUrls(args);
            if (!sunoUrls.isEmpty()) {
                for (Map<String, String> audioInfo : sunoUrls) {
                    String url = audioInfo.get("url");
                    String title = audioInfo.get("title");
                    String imageUrl = audioInfo.get("imageUrl");
                    event.reply(loadingEmoji + " Loading... `[" + (title != null ? title : "Unknown Title") + "]`",
                            m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), url, new ResultHandler(m, event, title, imageUrl, false)));
                }
            }
        } catch (IOException e) {
            event.replyError("Error fetching Suno playlist: " + e.getMessage());
        }
    }

    private void handleYoutubeUrl(CommandEvent event, String args) {
        event.reply(loadingEmoji + " Loading... `[" + args + "]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), args, new ResultHandler(m, event, null, null, false)));
    }

    private class ResultHandler implements AudioLoadResultHandler {

        private final Message m;
        private final CommandEvent event;
        private final String title;
        private final String imageUrl;
        private final boolean ytsearch;

        private ResultHandler(Message m, CommandEvent event, String title, String imageUrl, boolean ytsearch) {
            this.m = m;
            this.event = event;
            this.title = title;
            this.imageUrl = imageUrl;
            this.ytsearch = ytsearch;
        }

        private void loadSingle(AudioTrack track, AudioPlaylist playlist) {
            if (bot.getConfig().isTooLong(track)) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " This track (**" + track.getInfo().title + "**) is longer than the allowed maximum: `"
                        + TimeUtil.formatTime(track.getDuration()) + "` > `" + TimeUtil.formatTime(bot.getConfig().getMaxSeconds() * 1000) + "`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            String songTitle = title != null ? title : track.getInfo().title;
            String songImageUrl = imageUrl != null ? imageUrl : null;
            int pos = handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event, songTitle, songImageUrl))) + 1;
            String displayTitle = title != null ? title : (track.getInfo().title != null ? track.getInfo().title : "Unknown Title");

            String addMsg = FormatUtil.filter(event.getClient().getSuccess() + " Added **" + displayTitle
                    + "** (`" + TimeUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "to begin playing" : " to the queue at position " + pos));
            if (playlist == null || !event.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_ADD_REACTION)) {
                m.editMessage(addMsg).queue();
            } else {
                new ButtonMenu.Builder()
                        .setText(addMsg + "\n" + event.getClient().getWarning() + " This track has a playlist of **" + playlist.getTracks().size() + "** tracks attached. Select " + LOAD + " to load playlist.")
                        .setChoices(LOAD, CANCEL)
                        .setEventWaiter(bot.getWaiter())
                        .setTimeout(30, TimeUnit.SECONDS)
                        .setAction(re -> {
                            if (re.getName().equals(LOAD)) {
                                m.editMessage(addMsg + "\n" + event.getClient().getSuccess() + " Loaded **" + loadPlaylist(playlist, track) + "** additional tracks!").queue();
                            } else {
                                m.editMessage(addMsg).queue();
                            }
                        })
                        .setFinalAction(m -> {
                            try {
                                m.clearReactions().queue();
                            } catch (PermissionException ignore) {
                            }
                        })
                        .build().display(m);
            }
        }

        private int loadPlaylist(AudioPlaylist playlist, AudioTrack exclude) {
            int count = 0;
            for (AudioTrack track : playlist.getTracks()) {
                if (!bot.getConfig().isTooLong(track) && !track.equals(exclude)) {
                    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                    String songTitle = title != null ? title : track.getInfo().title;
                    String songImageUrl = imageUrl != null ? imageUrl : null;
                    handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event, songTitle, songImageUrl)));
                    count++;
                }
            }
            return count;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadSingle(track, null);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            if (playlist.getTracks().size() == 1 || playlist.isSearchResult()) {
                AudioTrack single = playlist.getSelectedTrack() == null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single, null);
            } else if (playlist.getSelectedTrack() != null) {
                AudioTrack single = playlist.getSelectedTrack();
                loadSingle(single, playlist);
            } else {
                int count = loadPlaylist(playlist, null);
                if (playlist.getTracks().size() == 0) {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " The playlist " + (playlist.getName() == null ? "" : "(**" + playlist.getName()
                            + "**) ") + " could not be loaded or contained 0 entries")).queue();
                } else if (count == 0) {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " All entries in this playlist " + (playlist.getName() == null ? "" : "(**" + playlist.getName()
                            + "**) ") + "were longer than the allowed maximum (`" + bot.getConfig().getMaxTime() + "`)")).queue();
                } else {
                    m.editMessage(FormatUtil.filter(event.getClient().getSuccess() + " Found "
                            + (playlist.getName() == null ? "a playlist" : "playlist **" + playlist.getName() + "**") + " with `"
                            + playlist.getTracks().size() + "` entries; added to the queue!"
                            + (count < playlist.getTracks().size() ? "\n" + event.getClient().getWarning() + " Tracks longer than the allowed maximum (`"
                            + bot.getConfig().getMaxTime() + "`) have been omitted." : ""))).queue();
                }
            }
        }

        @Override
        public void noMatches() {
            if (ytsearch) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " No results found for `" + event.getArgs() + "`.")).queue();
            } else {
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:" + event.getArgs(), new ResultHandler(m, event, title, imageUrl, true));
            }
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == Severity.COMMON) {
                m.editMessage(event.getClient().getError() + " Error loading: " + throwable.getMessage()).queue();
            } else {
                m.editMessage(event.getClient().getError() + " Error loading track.").queue();
            }
        }
    }

    //Inner Class PlaylistCmd
    public class PlaylistCmd extends MusicCommand {

        public PlaylistCmd(Bot bot) {
            super(bot);
            this.name = "playlist";
            this.aliases = new String[]{"pl"};
            this.arguments = "<name>";
            this.help = "plays the provided playlist";
            this.beListening = true;
            this.bePlaying = false;
        }

        @Override
        public void doCommand(CommandEvent event) {
            if (event.getArgs().isEmpty()) {
                event.reply(event.getClient().getError() + " Please include a playlist name.");
                return;
            }
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(event.getArgs());
            if (playlist == null) {
                event.replyError("I could not find `" + event.getArgs() + ".txt` in the Playlists folder.");
                return;
            }
            event.getChannel().sendMessage(loadingEmoji + " Loading playlist **" + event.getArgs() + "**... (" + playlist.getItems().size() + " items)").queue(m -> {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, RequestMetadata.fromResultHandler(at, event, at.getInfo().title, null))), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + " No tracks were loaded!"
                            : event.getClient().getSuccess() + " Loaded **" + playlist.getTracks().size() + "** tracks!");
                    if (!playlist.getErrors().isEmpty()) {
                        builder.append("\nThe following tracks failed to load:");
                    }
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000) {
                        str = str.substring(0, 1994) + " (...)";
                    }
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }
    }
}