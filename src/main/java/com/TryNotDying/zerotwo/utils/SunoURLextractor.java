package com.TryNotDying.zerotwo.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SunoURLextractor {

    private static final String SUNO_SONG_BASE_URL = "https://suno.com/song/";
    private static final String SUNO_CDN_BASE_URL = "https://cdn1.suno.ai/";
    private static final String DEFAULT_IMAGE_URL = "https://suno.com/placeholder-1.jpg";


    public static List<Map<String, String>> extractSunoAudioUrls(String userProvidedInput) throws IOException {
        String songBaseUrl = "https://suno.com/song/";
        String playlistBaseUrl = "https://suno.com/playlist/";
        List<Map<String, String>> audioUrls = new ArrayList<>();

        try {
            new URL(userProvidedInput);

            if (userProvidedInput.startsWith(songBaseUrl)) {
                audioUrls.add(extractSunoSongInfo(userProvidedInput));
            } else if (userProvidedInput.startsWith(playlistBaseUrl)) {
                audioUrls.addAll(extractSunoPlaylistInfo(userProvidedInput));
            } else {
                Map<String, String> youtubeInfo = new HashMap<>();
                youtubeInfo.put("url", userProvidedInput);
                youtubeInfo.put("title", null);
                youtubeInfo.put("imageUrl", null);
                audioUrls.add(youtubeInfo);
            }
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL: " + e.getMessage());
            throw new IOException("Invalid URL", e);
        }
        return audioUrls;
    }

    private static Map<String, String> extractSunoSongInfo(String songUrl) throws IOException {
        try {
            Document doc = Jsoup.connect(songUrl).get();
            Elements titleTags = doc.select("meta[property=og:title]");
            Elements imageTags = doc.select("meta[property=og:image]");
            Elements audioTags = doc.select("meta[property=og:audio]");

            if (titleTags.isEmpty() || audioTags.isEmpty()) {
                return null;
            }
            String title = titleTags.first().attr("content");
            String imageUrl = imageTags.isEmpty() ? DEFAULT_IMAGE_URL : imageTags.first().attr("content");
            String audioUrl = audioTags.first().attr("content");

            Map<String, String> audioInfo = new HashMap<>();
            audioInfo.put("url", audioUrl);
            audioInfo.put("title", title);
            audioInfo.put("imageUrl", imageUrl);
            return audioInfo;
        } catch (IOException e) {
            System.err.println("Error fetching song info for URL: " + songUrl + ", reason: " + e.getMessage());
            return null;
        }
    }

    private static List<Map<String, String>> extractSunoPlaylistInfo(String playlistUrl) throws IOException {
        List<Map<String, String>> audioUrls = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(playlistUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                    .maxBodySize(0)
                    .timeout(15000)
                    .get();
            Elements metaTags = doc.select("meta[property=og:audio]");
            if (metaTags.isEmpty()) {
                return audioUrls;
            }
            for (Element meta : metaTags) {
                String audioURL = meta.attr("content");
                try {
                    Map<String, String> audioInfo = getAudioInfoFromUrl(audioURL);
                    if (audioInfo != null) {
                        audioUrls.add(audioInfo);
                    }
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (IOException e) {
                    System.err.println("Error fetching song info for URL: " + audioURL + ", reason: " + e.getMessage());
                    //Skip this song and continue to the next.
                } catch (InterruptedException e) {
                    System.err.println("Delay interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
            return audioUrls;
        } catch (IOException e) {
            System.err.println("Error fetching or parsing Suno playlist URL: " + playlistUrl + ", reason: " + e.getMessage());
            throw new IOException("Error fetching Suno playlist", e);
        }
    }

    private static Map<String, String> getAudioInfoFromUrl(String audioUrl) throws IOException {
        String reconstructedUrl = reconstructSunoUrl(audioUrl);
        if (reconstructedUrl == null) {
            return null;
        }
        try {
            Document doc = Jsoup.connect(reconstructedUrl).get();
            Elements titleTags = doc.select("meta[property=og:title]");
            Elements imageTags = doc.select("meta[property=og:image]");

            if (titleTags.isEmpty()) {
                return null;
            }
            String title = titleTags.first().attr("content");
            String imageUrl = imageTags.isEmpty() ? DEFAULT_IMAGE_URL : imageTags.first().attr("content");

            Map<String, String> audioInfo = new HashMap<>();
            audioInfo.put("url", audioUrl);
            audioInfo.put("title", title);
            audioInfo.put("imageUrl", imageUrl);
            return audioInfo;
        } catch (IOException e) {
            System.err.println("Error fetching title/image for URL: " + reconstructedUrl + ", reason: " + e.getMessage());
            return null;
        }
    }

    public static String reconstructSunoUrl(String cdnUrl) {
        if (!cdnUrl.startsWith(SUNO_CDN_BASE_URL)) {
            return null;
        }
        String songId = cdnUrl.substring(SUNO_CDN_BASE_URL.length());
        songId = songId.substring(0, songId.lastIndexOf(".mp3"));
        return SUNO_SONG_BASE_URL + songId;
    }
}