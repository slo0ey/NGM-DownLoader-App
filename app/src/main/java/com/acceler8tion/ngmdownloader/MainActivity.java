package com.acceler8tion.ngmdownloader;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.acceler8tion.ngmdownloader.request.DownloadFailedException;
import com.acceler8tion.ngmdownloader.request.NGRequest;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final EditText input = findViewById(R.id.id_here);
    private final Button search = findViewById(R.id.search);
    private final ImageButton settings = findViewById(R.id.settings);
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String id = input.getText().toString();
                int status = songIdStatus(id);
                if(status == 1) {
                    tts.speak(getString(R.string.idstatus1), QUEUE_FLUSH, null, null);
                } else if(status == 2) {
                    tts.speak(getString(R.string.idstatus2), QUEUE_FLUSH, null, null);
                } else {
                    //TODO: later...
                }
            }
        });
    }

    protected int songIdStatus(String id) {
        if(id.isEmpty()) return 1;
        try {
            Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return 2;
        }
        return 0;
    }

    protected Thread download(final int songId) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                int status;
                try {
                    NGRequest ngr = NGRequest.build(songId);
                    byte[] songData = ngr.download();
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        //TODO: handle this
                    } else {
                        //TODO: later...
                    }
                } catch (DownloadFailedException e) {
                    e.printStackTrace();
                    status = -2;
                }
            }
        });
    }
}