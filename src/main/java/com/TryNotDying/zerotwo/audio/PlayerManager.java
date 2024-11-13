package com.TryNotDying.zerotwo.audio;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.TryNotDying.zerotwo.Bot;
import com.TryNotDying.zerotwo.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

public class PlayerManager extends DefaultAudioPlayerManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(PlayerManager.class);
    private final Bot bot;

    public PlayerManager(Bot bot) {
        this.bot = bot;
    }

    public void init() {
        TransformativeAudioSourceManager.createTransforms(bot.getConfig().getTransforms()).forEach(t -> registerSourceManager(t));

        YoutubeAudioSourceManager yt = setupYoutubeAudioSourceManager();
        registerSourceManager(yt);

        registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        registerSourceManager(new BandcampAudioSourceManager());
        registerSourceManager(new VimeoAudioSourceManager());
        registerSourceManager(new TwitchStreamAudioSourceManager());
        registerSourceManager(new BeamAudioSourceManager());
        registerSourceManager(new GetyarnAudioSourceManager());
        registerSourceManager(new NicoAudioSourceManager());
        registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        AudioSourceManagers.registerLocalSource(this);

        DuncteBotSources.registerAll(this, "en-US");
    }

    private YoutubeAudioSourceManager setupYoutubeAudioSourceManager() {
        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(true);
        yt.setPlaylistPageCount(bot.getConfig().getMaxYTPlaylistPages());

        if (bot.getConfig().useYoutubeOauth2()) {
            String token = null;
            try {
                token = Files.readString(OtherUtil.getPath("youtubetoken.txt"));
            } catch (NoSuchFileException e) {
                // ignored
            } catch (IOException e) {
                LOGGER.warn("Failed to read YouTube OAuth2 token file: {}", e.getMessage());
                return yt;
            }
            LOGGER.debug("Read YouTube OAuth2 refresh token from youtubetoken.txt");
            try {
                yt.useOauth2(token, false);
            } catch (Exception e) {
                LOGGER.warn("Failed to authorise with YouTube. If this issue persists, delete the youtubetoken.txt file to reauthorise.", e);
            }
        }
        return yt;
    }

    public Bot getBot() {
        return bot;
    }

    public boolean hasHandler(Guild guild) {
        return guild.getAudioManager().getSendingHandler() != null;
    }

    public AudioHandler setUpHandler(Guild guild) {
        AudioHandler handler;
        if (guild.getAudioManager().getSendingHandler() == null) {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
        } else {
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        }
        return handler;
    }
}