package com.TryNotDying.zerotwo.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SunoURLextractor {

    public static List<String> extractSunoAudioUrls(String userProvidedInput) {
        String songbaseUrl = "https://suno.com/song/";
        String playlistBaseUrl = "https://suno.com/playlist/";
        List<String> audioUrls = new ArrayList<>();

        try {
            // Basic URL validation
            new URL(userProvidedInput);

            if (userProvidedInput.startsWith(songbaseUrl)) {
                String audioUrl = extractAudioFromSunoPage(userProvidedInput);
                if (audioUrl != null) {
                    audioUrls.add(audioUrl);
                } else {
                    System.err.println("No audio URL found for song: " + userProvidedInput);
                }
            } else if (userProvidedInput.startsWith(playlistBaseUrl)) {
                audioUrls.addAll(extractPlaylistAudioUrls(userProvidedInput));
                if (audioUrls.isEmpty()) {
                    System.err.println("No audio URLs found for playlist: " + userProvidedInput);
                }
            } else {
                System.err.println("URL is not a Suno song or playlist: " + userProvidedInput);
            }
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error extracting audio URL: " + e.getMessage());
        }
        return audioUrls;
    }

    private static String extractAudioFromSunoPage(String sunoURL) throws IOException {
        try {
            Document doc = Jsoup.connect(sunoURL).get();
            Elements metaTags = doc.select("meta[property=og:audio]");
            if (metaTags.isEmpty()) {
                return null;
            }
            String audioUrl = metaTags.first().attr("content"); //Corrected attribute name
            try {
                new URL(audioUrl); // Validate URL before returning
                return audioUrl;
            } catch (MalformedURLException e) {
                System.err.println("Invalid audio URL found on page: " + audioUrl);
                return null;
            }
        } catch (IOException e) {
            throw new IOException("Error fetching or parsing Suno URL: " + e.getMessage(), e); //Added the cause
        }
    }

    private static List<String> extractPlaylistAudioUrls(String playlistUrl) throws IOException {
        List<String> audioUrls = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(playlistUrl).get();
            Elements metaTags = doc.select("meta[property=og:audio]");
            for (Element meta : metaTags) {
                String audioURL = meta.attr("content"); //Corrected attribute name
                try {
                    new URL(audioURL);
                    audioUrls.add(audioURL);
                } catch (MalformedURLException e) {
                    System.err.println("Invalid audio URL in playlist: " + audioURL);
                    // Ignore malformed URLs
                }
            }
            return audioUrls;
        } catch (IOException e) {
            throw new IOException("Error fetching or parsing Suno playlist URL: " + e.getMessage(), e); //Added the cause

        }
    }
}