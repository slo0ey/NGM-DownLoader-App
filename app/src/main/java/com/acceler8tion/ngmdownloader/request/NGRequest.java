package com.acceler8tion.ngmdownloader.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Objects;

public class NGRequest implements Runnable {

    private static final String OLD_URL = "https://www.newgrounds.com/audio/download/";
    private static final String CURRENT_URL = "https://audio-download.ngfiles.com/";
    private static final String INFO_URL = "https://www.newgrounds.com/audio/listen/";
    private final int songId;
    private String downloadUrl;
    private String name;

    private Document doc;
    private int ERR_CODE = -1;

    public NGRequest(int songId) {
        this.songId = songId;
    }

    public void build() throws NGMNoSuchMusicException, NGMConnectFailedException {

        Thread thread = new Thread(this);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            ERR_CODE = 3;
        }

        if(ERR_CODE == 1) throw new NGMNoSuchMusicException();
        else if(ERR_CODE == 2 || ERR_CODE == 3) throw new NGMConnectFailedException();

        analyze();
    }

    private void analyze() {
        String songName = doc.title();
        String title = songName;
        title = title.replace(" ", "-");
        title = title.replace("&", "amp");
        title = title.replace("\"", "quot");
        title = title.replace("<", "lt");
        title = title.replace(">", "gt");
        if (title.length() > 27) {
            title = title.substring(0, 27);
        }
        title = title.replace("[^a-zA-z0-9-]", "");

        if(songId <= 469776) {
            downloadUrl = OLD_URL + songId;
            name = title;
        } else {
            downloadUrl = String.format(CURRENT_URL + "%s000/%s_%s.mp3", ("" + songId).substring(0, 3), songId, title);
            name = songName;
        }
    }

    public int getSongId() {
        return songId;
    }

    public String getName() {
        return name;
    }

    public String getDownloadUrl() {
        return downloadUrl;
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

    @Override
    public void run() {
        try {
            doc = Jsoup.connect(INFO_URL + songId)
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .get();
        } catch (HttpStatusException e) {
            ERR_CODE = 1;
        } catch (IOException e) {
            ERR_CODE = 2;
        }
    }
}
