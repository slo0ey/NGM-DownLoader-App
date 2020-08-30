package com.acceler8tion.ngmdownloader;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.acceler8tion.ngmdownloader.request.NGRequest;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final EditText input = findViewById(R.id.id_here);
    private final Button search = findViewById(R.id.search);
    private final ImageButton settings = findViewById(R.id.settings);
    private final ImageView github = findViewById(R.id.github);
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
                    tts.speak(getString(R.string.id_status_1), QUEUE_FLUSH, null, null);
                } else if(status == 2) {
                    tts.speak(getString(R.string.id_status_2), QUEUE_FLUSH, null, null);
                } else {
                    download(Integer.parseInt(id)).start();
                }
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tts.speak("아직 안만듬", QUEUE_FLUSH, null, null);
            }
        });

        github.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://github.com/GyuminKim29/"));
                startActivity(intent);
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

    @SuppressWarnings("ConstantConditions")
    protected Thread download(final int songId) {
        return new Thread(new Runnable() {

            private AppCompatActivity context = MainActivity.this;
            private int status;
            private NGRequest ngr;
            private String path;
            private StackTraceElement stack;

            @Override
            public void run() {

                try {
                    ngr = NGRequest.build(songId);
                    String songData = ngr.download();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        //TODO: handle this
                    } else {
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                        String subPath = pref.getString("sub", "/Music");
                        path = Environment.getExternalStorageDirectory()+subPath+"/"+songId+".mp3";
                        save(path, songData);
                        status = 1;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    stack = e.getStackTrace()[0];
                    status = -1;
                }
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(status == 1) {
                            Toast.makeText(context, String.format(context.getString(R.string.download_success), ngr.getName(), ""+songId, path), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, String.format(context.getString(R.string.download_failed), stack.toString()), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    protected void save(String path, String data) throws IOException {
        FileWriter fw = new FileWriter(path);
        fw.write(data);
        fw.flush();
    }
}