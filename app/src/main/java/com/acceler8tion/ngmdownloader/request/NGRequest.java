package com.acceler8tion.ngmdownloader.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Objects;

public class NGRequest {

    private static final String OLD_URL = "https://www.newgrounds.com/audio/download/";
    private static final String CURRENT_URL = "https://audio-download.ngfiles.com/";
    private static final String INFO_URL = "https://www.newgrounds.com/audio/listen/";
    private final int songId;
    private final String downloadUrl;
    private final String name;

    private NGRequest(int songId, String downloadUrl, String name) {
        this.songId = songId;
        this.downloadUrl = Objects.requireNonNull(downloadUrl, "failed-to-parse-url");
        this.name = name;
    }

    public static NGRequest build(int songId) {
        String[] info = parseInfo(songId);
        if(info == null) return null;
        if (songId <= 469776) {
            return new NGRequest(songId, OLD_URL + songId, info[1]);
        } else {
            return new NGRequest(songId, info[0], info[1]);
        }
    }

    public static String[] parseInfo(int songId) {
        try {
            Document con = Jsoup.connect(INFO_URL + songId)
                    .ignoreContentType(true)
                    .get();
            String title = con.title();
            final String name = title;
            title = title.replace(" ", "-");
            title = title.replace("&", "amp");
            title = title.replace("\"", "quot");
            title = title.replace("<", "lt");
            title = title.replace(">", "gt");
            if (title.length() > 27) {
                title = title.substring(0, 27);
            }
            title = title.replace("[^a-zA-z0-9-]", "");
            return new String[]{String.format(CURRENT_URL + "%s000/%s_%s.mp3", ("" + songId).substring(0, 3), songId, title), name};
        } catch (IOException e) {
            return null;
        }
    }

    public String download() throws DownloadFailedException {
        try {
            return Jsoup.connect(downloadUrl)
                    .method(Connection.Method.GET)
                    .execute().body();
        } catch (Exception e) {
            e.printStackTrace();
            throw new DownloadFailedException("Failed to download: " + e.getMessage());
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(!(obj instanceof NGRequest)){
            return false;
        }
        NGRequest ngr = (NGRequest) obj;
        return ngr.songId == songId && downloadUrl.equalsIgnoreCase(ngr.downloadUrl);
    }

    @NonNull
    @Override
    public String toString() {
        return "NGRequest["+
                "songId="+songId+
                ", downloadUrl="+downloadUrl
                +"]";
    }
}
