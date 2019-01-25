package com.example.test64bit;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * @author Lody
 */
public class App extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.e("TTTTTT", "app => attachBaseContext");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("TTTTTT", "app => onCreate");
    }
}
