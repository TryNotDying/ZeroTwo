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

    public static List<Map<String, String>> extractSunoAudioUrls(String userProvidedInput) throws IOException {
        String playlistBaseUrl = "https://suno.com/playlist/";
        List<Map<String, String>> audioUrls = new ArrayList<>();

        try {
            new URL(userProvidedInput);

            if (userProvidedInput.startsWith(playlistBaseUrl)) {
                audioUrls.addAll(extractPlaylistAudioUrls(userProvidedInput));
                if (audioUrls.isEmpty()) {
                    System.err.println("No audio URLs found for playlist: " + userProvidedInput);
                }
            } else {
                System.err.println("URL is not a Suno playlist: " + userProvidedInput);
            }
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL: " + e.getMessage());
            throw new IOException("Invalid URL", e);
        }
        return audioUrls;
    }

    private static List<Map<String, String>> extractPlaylistAudioUrls(String playlistUrl) throws IOException {
        List<Map<String, String>> audioUrls = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(playlistUrl).get();
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
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error fetching song info for URL: " + audioURL + ", reason: " + e.getMessage());
                }
            }
            return audioUrls;
        } catch (IOException e) {
            throw new IOException("Error fetching or parsing Suno playlist URL: " + e.getMessage(), e);
        }
    }


    private static Map<String, String> getAudioInfoFromUrl(String audioUrl) throws IOException {
        String reconstructedUrl = reconstructSunoUrl(audioUrl);
        if (reconstructedUrl == null) {
            return null; // Handle invalid URLs
        }
        try {
            Document doc = Jsoup.connect(reconstructedUrl).get();
            Elements titleTags = doc.select("meta[property=og:title]");
            if (titleTags.isEmpty()) {
                return null; // Handle missing title
            }
            String title = titleTags.first().attr("content");
            Map<String, String> audioInfo = new HashMap<>();
            audioInfo.put("url", audioUrl); // Keep original URL
            audioInfo.put("title", title);
            return audioInfo;
        } catch (IOException e) {
            System.err.println("Error fetching title for URL: " + reconstructedUrl + ", reason: " + e.getMessage());
            return null;
        }
    }

    private static String reconstructSunoUrl(String cdnUrl) {
        if (!cdnUrl.startsWith(SUNO_CDN_BASE_URL)) {
            return null; // Not a Suno CDN URL
        }
        String songId = cdnUrl.substring(SUNO_CDN_BASE_URL.length());
        songId = songId.substring(0, songId.lastIndexOf(".mp3")); // Remove .mp3 extension
        return SUNO_SONG_BASE_URL + songId;
    }
}