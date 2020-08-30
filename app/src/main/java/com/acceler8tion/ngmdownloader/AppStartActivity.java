package com.acceler8tion.ngmdownloader;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class AppStartActivity extends AppCompatActivity {

    private Handler permissionCheckHandler = new Handler();
    private Handler afterWorkHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_start);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            permissionCheckHandler
                    .postDelayed(requestAllPermission, 3000);
        } else {
            afterWorkHandler
                    .postDelayed(goMain, 2000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 200) {
            //first try
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                afterWorkHandler
                        .post(goMain);
            } else {
                Toast.makeText(AppStartActivity.this, "권한 취소시 앱을 실행 할 수 없습니다.\n\n재시작 시, 파일쓰기 권한을 허용시켜주세요.", Toast.LENGTH_SHORT).show();
                afterWorkHandler
                        .postDelayed(shutDown, 1000);
            }
        } else {
            //second try
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                afterWorkHandler
                        .post(goMain);
            } else {
                Toast.makeText(AppStartActivity.this, "설정 - 애플리케이션 에서 NGM Downloader 앱의 파일쓰기권한을 허용해주세요.", Toast.LENGTH_SHORT).show();
                afterWorkHandler
                        .postDelayed(shutDown, 1000);

            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected Runnable requestAllPermission = new Runnable() {
        @Override
        public void run() {

            String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            Intent intent = new Intent(AppStartActivity.this, MainActivity.class);
            int writeState = PermissionChecker.checkSelfPermission(AppStartActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if( writeState != PermissionChecker.PERMISSION_GRANTED) {
                if(shouldShowRequestPermissionRationale(perms[0])) {
                    Toast.makeText(AppStartActivity.this, "이후 권한을 거절 할 시, 설정에서 직접 수락해야 합니다.", Toast.LENGTH_SHORT).show();
                    requestPermissions(perms, 202);
                } else {
                    requestPermissions(perms, 200);
                }
            } else {
                afterWorkHandler
                        .post(goMain);
            }
        }
    };

    protected Runnable shutDown = new Runnable() {
        @Override
        public void run() {
            moveTaskToBack(true);
            finishAndRemoveTask();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    };

    protected Runnable goMain = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(AppStartActivity.this, MainActivity.class);
            startActivity(intent);
        }
    };
}