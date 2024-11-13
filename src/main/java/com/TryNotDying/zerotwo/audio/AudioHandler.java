package com.TryNotDying.zerotwo.audio;

import com.TryNotDying.zerotwo.playlist.PlaylistLoader.Playlist;
import com.TryNotDying.zerotwo.queue.AbstractQueue;
import com.TryNotDying.zerotwo.settings.QueueType;
import com.TryNotDying.zerotwo.utils.TimeUtil;
import com.TryNotDying.zerotwo.settings.RepeatMode;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.TryNotDying.zerotwo.settings.Settings;
import com.TryNotDying.zerotwo.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import java.nio.ByteBuffer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {
    public final static String PLAY_EMOJI = "\u25B6"; // ▶
    public final static String PAUSE_EMOJI = "\u23F8"; // ⏸
    public final static String STOP_EMOJI = "\u23F9"; // ⏹

    private final static Logger LOGGER = LoggerFactory.getLogger(AudioHandler.class);

    private final List<AudioTrack> defaultQueue = new LinkedList<>();
    private final Set<String> votes = new HashSet<>();

    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;

    private AudioFrame lastFrame;
    private AbstractQueue<QueuedTrack> queue;

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player) {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();

        this.setQueueType(manager.getBot().getSettingsManager().getSettings(guildId).getQueueType());
    }

    public void setQueueType(QueueType type) {
        queue = type.createInstance(queue);
    }

    public int addTrackToFront(QueuedTrack qtrack) {
        if (audioPlayer.getPlayingTrack() == null) {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        } else {
            queue.addAt(0, qtrack);
            return 0;
        }
    }

    public int addTrack(QueuedTrack qtrack) {
        if (audioPlayer.getPlayingTrack() == null) {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        } else
            return queue.add(qtrack);
    }

    public AbstractQueue<QueuedTrack> getQueue() {
        return queue;
    }

    public void stopAndClear() {
        queue.clear();
        defaultQueue.clear();
        audioPlayer.stopTrack();
    }

    public boolean isMusicPlaying(JDA jda) {
        return guild(jda).getSelfMember().getVoiceState().inVoiceChannel() && audioPlayer.getPlayingTrack() != null;
    }

    public Set<String> getVotes() {
        return votes;
    }

    public AudioPlayer getPlayer() {
        return audioPlayer;
    }

    public RequestMetadata getRequestMetadata() {
        if (audioPlayer.getPlayingTrack() == null)
            return RequestMetadata.EMPTY;
        RequestMetadata rm = audioPlayer.getPlayingTrack().getUserData(RequestMetadata.class);
        return rm == null ? RequestMetadata.EMPTY : rm;
    }

    public boolean playFromDefault() {
        if (!defaultQueue.isEmpty()) {
            audioPlayer.playTrack(defaultQueue.remove(0));
            return true;
        }
        Settings settings = manager.getBot().getSettingsManager().getSettings(guildId);
        if (settings == null || settings.getDefaultPlaylist() == null)
            return false;

        Playlist pl = manager.getBot().getPlaylistLoader().getPlaylist(settings.getDefaultPlaylist());
        if (pl == null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(manager, (at) -> {
            if (audioPlayer.getPlayingTrack() == null)
                audioPlayer.playTrack(at);
            else
                defaultQueue.add(at);
        }, () -> {
            if (pl.getTracks().isEmpty() && !manager.getBot().getConfig().getStay())
                manager.getBot().closeAudioConnection(guildId);
        });
        return true;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        RepeatMode repeatMode = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();
        if (endReason == AudioTrackEndReason.FINISHED && repeatMode != RepeatMode.OFF) {
            QueuedTrack clone = new QueuedTrack(track.makeClone(), track.getUserData(RequestMetadata.class));
            if (repeatMode == RepeatMode.ALL)
                queue.add(clone);
            else
                queue.addAt(0, clone);
        }

        if (queue.isEmpty()) {
            if (!playFromDefault()) {
                manager.getBot().getNowplayingHandler().onTrackUpdate(null, null, null);
                if (!manager.getBot().getConfig().getStay())
                    manager.getBot().closeAudioConnection(guildId);
                player.setPaused(false);
            }
        } else {
            QueuedTrack qt = queue.pull();
            player.playTrack(qt.getTrack());
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (exception.getMessage().equals("Sign in to confirm you're not a bot")
                || exception.getMessage().equals("Please sign in")
                || exception.getMessage().equals("This video requires login."))
            LOGGER.error(
                    "Track {} has failed to play: {}. "
                            + "You will need to sign in to Google to play YouTube tracks. "
                            + "More info: https://zerotwo.com/youtube-oauth2",
                    track.getIdentifier(),
                    exception.getMessage()
            );
        else
            LOGGER.error("Track {} has failed to play", track.getIdentifier(), exception);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        votes.clear();
        RequestMetadata rm = track.getUserData(RequestMetadata.class);
        String title = (rm != null && rm.title != null) ? rm.title : track.getInfo().title;
        String imageUrl = (rm != null && rm.imageUrl != null) ? rm.imageUrl : null;
        manager.getBot().getNowplayingHandler().onTrackUpdate(track, title, imageUrl);
    }


    public Message getNowPlaying(JDA jda) {
        if (isMusicPlaying(jda)) {
            Guild guild = guild(jda);
            AudioTrack track = audioPlayer.getPlayingTrack();
            MessageBuilder mb = new MessageBuilder();
            mb.append(FormatUtil.filter(manager.getBot().getConfig().getSuccess() + " **Now Playing in " + guild.getSelfMember().getVoiceState().getChannel().getAsMention() + "...**"));
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(guild.getSelfMember().getColor());
            RequestMetadata rm = getRequestMetadata();
            if (rm.getOwner() != 0L) {
                User u = guild.getJDA().getUserById(rm.user.id);
                if (u == null)
                    eb.setAuthor(FormatUtil.formatUsername(rm.user), null, rm.user.avatar);
                else
                    eb.setAuthor(FormatUtil.formatUsername(u), null, u.getEffectiveAvatarUrl());
            }

            String title = getTitle(track);
            String imageUrl = getImageUrl(track);

            try {
                eb.setTitle(title, track.getInfo().uri);
            } catch (Exception e) {
                eb.setTitle(title);
            }

            if (track instanceof YoutubeAudioTrack && manager.getBot().getConfig().useNPImages()) {
                eb.setThumbnail("https://img.youtube.com/vi/" + track.getIdentifier() + "/mqdefault.jpg");
            } else if (imageUrl != null) {
                eb.setThumbnail(imageUrl);
            } else {
                eb.setThumbnail("https://suno.com/placeholder-1.jpg");
            }

            double progress = (double) audioPlayer.getPlayingTrack().getPosition() / track.getDuration();
            eb.setDescription(getStatusEmoji()
                    + " " + FormatUtil.progressBar(progress)
                    + " `[" + TimeUtil.formatTime(track.getPosition()) + "/" + TimeUtil.formatTime(track.getDuration()) + "]` "
                    + FormatUtil.volumeIcon(audioPlayer.getVolume()));

            return mb.setEmbeds(eb.build()).build();
        } else {
            return null;
        }
    }

    public Message getNoMusicPlaying(JDA jda) {
        Guild guild = guild(jda);
        return new MessageBuilder()
                .setContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess() + " **Now Playing...**"))
                .setEmbeds(new EmbedBuilder()
                        .setTitle("No music playing")
                        .setDescription(STOP_EMOJI + " " + FormatUtil.progressBar(-1) + " " + FormatUtil.volumeIcon(audioPlayer.getVolume()))
                        .setColor(guild.getSelfMember().getColor())
                        .build()).build();
    }

    public String getStatusEmoji() {
        return audioPlayer.isPaused() ? PAUSE_EMOJI : PLAY_EMOJI;
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        return true;
    }


    private Guild guild(JDA jda) {
        return jda.getGuildById(guildId);
    }

    private String getTitle(AudioTrack track) {
        RequestMetadata rm = track.getUserData(RequestMetadata.class);
        return (rm != null && rm.title != null) ? rm.title : track.getInfo().title;
    }

    private String getImageUrl(AudioTrack track) {
        RequestMetadata rm = track.getUserData(RequestMetadata.class);
        return (rm != null && rm.imageUrl != null) ? rm.imageUrl : null;
    }
}