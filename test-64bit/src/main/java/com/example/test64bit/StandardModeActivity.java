package com.example.test64bit;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileOutputStream;

import io.virtualapp.test64bit.R;

public class StandardModeActivity extends Activity {

    private Intent intent;
    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("TTTTTT", "onServiceConnected: " + name + ", " + service);
            Messenger messenger = new Messenger(service);
            try {
                Message msg = new Message();
                msg.what = 999;
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("TTTTTT", "onServiceDisconnected: " + name);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = new Intent(this, TestService.class);
        setContentView(R.layout.activity_main);
    }

    public void startService(View view) {
        startService(intent);
    }

    public void bindService(View view) {
        bindService(intent, conn, BIND_AUTO_CREATE);
    }

    public void stopService(View view) {
        stopService(intent);
    }

    public void unbindService(View view) {
        unbindService(conn);
    }
}
