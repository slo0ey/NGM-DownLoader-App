package com.acceler8tion.ngmdownloader;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.acceler8tion.ngmdownloader.request.NGMConnectFailedException;
import com.acceler8tion.ngmdownloader.request.NGMNoSuchMusicException;
import com.acceler8tion.ngmdownloader.request.NGRequest;

import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText input;
    private Button search;
    private ImageButton settings;
    private ImageView github;
    private TextToSpeech tts;

    private DownloadManager downloadManager;
    private String subPath;
    private int latestId;
    private String latestSongName;
    private String latestPath;
    private long latestDownloadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        input = findViewById(R.id.id_here);
        search = findViewById(R.id.search);
        settings = findViewById(R.id.settings);
        github = findViewById(R.id.github);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        subPath = pref.getString("sp", "/Music");
        latestId = pref.getInt("lid", -1);
        latestSongName = pref.getString("lname", "");
        latestPath = pref.getString("lpath", "");
        latestDownloadId = pref.getLong("ldownloadId", -1L);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        IntentFilter inf = new IntentFilter();
        inf.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        inf.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        registerReceiver(onDownloadComplete, inf);

        tts = new TextToSpeech(this, i -> {
            if(i != ERROR) {
                tts.setLanguage(Locale.KOREAN);
            }
        });

        search.setOnClickListener(view -> {
            final String id = input.getText().toString();
            int status = checkId(id);
            if(status == 1) {
                tts.speak(getString(R.string.id_status_1), QUEUE_FLUSH, null, null);
            } else if(status == 2) {
                tts.speak(getString(R.string.id_status_2), QUEUE_FLUSH, null, null);
            } else {
                if(isRunning(latestId)) tts.speak(getString(R.string.already_started), QUEUE_FLUSH, null, null);
                download(Integer.parseInt(id));
            }
        });

        settings.setOnClickListener(view -> tts.speak("아직 안만듬", QUEUE_FLUSH, null, null));

        github.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("https://github.com/GyuminKim29/"));
            startActivity(intent);
        });
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                if(latestId == Integer.parseInt(""+id)) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(id);
                    Cursor c = downloadManager.query(q);
                    if(!c.moveToFirst()){
                        return;
                    }

                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = c.getInt(columnIndex);
                    if(status == DownloadManager.STATUS_SUCCESSFUL) {
                        Toast.makeText(context, String.format("다운로드에 성공했습니다.\nTitle: %s\nId: %s\nFilePath: %s", latestSongName, latestId, latestPath), Toast.LENGTH_LONG).show();
                    } else if(status == DownloadManager.STATUS_FAILED) {
                        Toast.makeText(context, "다운로드에 실패했습니다...", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if(DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(intent.getAction())) {
                Toast.makeText(context, "다운로드 중 입니다.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void registerLatestMusicInfo(NGRequest ngr, String path, long latestDownloadId) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.edit()
                .putInt("lid", ngr.getSongId())
                .putString("lname", ngr.getName())
                .putString("lpath", path)
                .putLong("ldownloadId", latestDownloadId)
                .apply();
    }

    private int checkId(String id) {
        if(id.isEmpty()) return 1;
        try {
            Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return 2;
        }
        return 0;
    }

    private boolean isRunning(long downloadId) {
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(downloadId);
        Cursor c = downloadManager.query(q);
        if(!c.moveToFirst()){
            return false;
        }

        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int status = c.getInt(columnIndex);

        return !(status == DownloadManager.STATUS_FAILED || status == DownloadManager.STATUS_SUCCESSFUL);
    }

    private void download(int songId) {
        try {
            NGRequest ngr = NGRequest.build(songId);
            String url = ngr.getDownloadUrl();
            File file;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                file = new File(getExternalFilesDir(null), ngr.getName() + ".mp3");
            } else {
                file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music/" + ngr.getName() + ".mp3");
            }
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle("Downloading Music")
                    .setDescription("Downloading "+ngr.getName())
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                request.setRequiresCharging(false);
            }
            Toast.makeText(this, "다운로드를 시작합니다.", Toast.LENGTH_SHORT).show();

            long downloadId = downloadManager.enqueue(request);
            registerLatestMusicInfo(ngr, file.getAbsolutePath(), downloadId);
        } catch (NGMNoSuchMusicException e) {
            e.printStackTrace();
            tts.speak(getString(R.string.do_not_exist), QUEUE_FLUSH, null, null);
        } catch (NGMConnectFailedException e) {
            e.printStackTrace();
            tts.speak(getString(R.string.check_internet), QUEUE_FLUSH, null, null);
        }
    }
}