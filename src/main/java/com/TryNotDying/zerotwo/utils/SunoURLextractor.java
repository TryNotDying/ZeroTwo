package com.TryNotDying.ZeroTwo.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SunoURLextractor {

    public static String extractSunoAudioUrl(String userProvidedInput) {
        String baseUrl = "https://suno.com/song/";

        try {
            URL url = new URL(userProvidedInput); //Check if valid URL
            if(userProvidedInput.startsWith(baseUrl)) {
                return extractAudioFromSunoPage(userProvidedInput); //Extract if valid Suno URL
            }
        } catch (MalformedURLException e) {
            //Not a valid URL - return the input unchanged.
            return null; //Could also return an error message.
        } catch (IOException e) {
            System.err.println("Error extracting Suno audio URL: " + e.getMessage());
            return null; //Could also return an error message.

        }

        return null; //Return null if it's not a suno song URL.

    }

    private static String extractAudioFromSunoPage(String sunoURL) throws IOException {
        try {
            Document doc = Jsoup.connect(sunoURL).get();
            Elements metaTags = doc.select("meta[property=og:audio]");

            if (metaTags.isEmpty()) {
                return null; //Or throw an exception.
            }

            String audioUrl = metaTags.first().attr("content");

            try {
                new URL(audioUrl); // Check for valid URL format.
                return audioUrl;
            } catch (MalformedURLException e) {
                return null; //Or throw an exception.
            }
        } catch (IOException e) {
            throw new IOException("Error fetching or parsing Suno URL: " + e.getMessage());
        }
    }
}