package io.virtualapp.splash;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;

import org.jdeferred.Promise;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.HomeActivity;
import io.virtualapp.home.HomeActivityMain;
import jonathanfinerty.once.Once;

public class SplashActivity extends VActivity {

    private static String TAG = "SplashActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    //        @SuppressWarnings("unused")
            boolean enterGuide = !Once.beenDone(Once.THIS_APP_INSTALL, VCommends.TAG_NEW_VERSION);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_splash);

        //获取授权
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && VirtualCore.get().getTargetSdkVersion() >= android.os.Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }else{
                toMain();
            }
        }else {
                toMain();
        }

    }

    //进入主界面
    private void toMain(){
        VUiKit.defer().when(() -> {
            long time = System.currentTimeMillis();
            doActionInThread();
            time = System.currentTimeMillis() - time;
            long delta = 500L - time;
            if (delta > 0) {
                VUiKit.sleep(delta);
            }
        }).done((res) -> {
            HomeActivityMain.goHome(this);
            finish();
            overridePendingTransition(0, 0);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
             //                if(!StringUtils.getAppCopystatue(this)){
             //                    File file = Environment.getExternalStorageDirectory();
             //                    copyAssetsFile(getApplicationContext(),"YouTube.apk",""+file.getPath());
             //                    //Log.e("liuheng2","2"+getAppCopystatue());
             //                }
             // Permission Granted准许
                Toast.makeText(this,"已获得授权！",Toast.LENGTH_SHORT).show();
                VUiKit.defer().when(() -> {
                    long time = System.currentTimeMillis();
                    doActionInThread();
                    time = System.currentTimeMillis() - time;
                    long delta = 500L - time;
                    if (delta > 0) {
                        VUiKit.sleep(delta);
                    }
                }).done((res) -> {
                    HomeActivityMain.goHome(this);
                    finish();

                    overridePendingTransition(0, 0);
                });
            }else {
                Toast.makeText(this,"未获得授权！无法启动！",Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void doActionInThread() {
        if (!VirtualCore.get().isEngineLaunched()) {
            VirtualCore.get().waitForEngine();
        }
    }

    @Override
    public void onBackPressed() {

    }


    public  Uri copyAssetsFile(Context context, String fileName, String path) {
        try {
            InputStream mInputStream = context.getAssets().open(fileName);
            File file = new File(path);
            if (!file.exists()) {
                file.mkdir();
            }
            File mFile = new File(path + File.separator + "YouTube.apk");
            if(!mFile.exists())
                mFile.createNewFile();
            Log.e(TAG,"开始拷贝");
            FileOutputStream mFileOutputStream = new FileOutputStream(mFile);
            byte[] mbyte = new byte[1024];
            int i = 0;
            while((i = mInputStream.read(mbyte)) > 0){
                mFileOutputStream.write(mbyte, 0, i);
            }
            mInputStream.close();
            mFileOutputStream.close();
            Uri uri = null;
            try{
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    //包名.fileprovider
                    uri = FileProvider.getUriForFile(context, "com.example.app.fileprovider", mFile);
                }else{
                    uri = Uri.fromFile(mFile);
                }
            }catch (ActivityNotFoundException anfe){
                Log.e(TAG,anfe.getMessage());
            }
            MediaScannerConnection.scanFile(context, new String[]{mFile.getAbsolutePath()}, null, null);
            Log.e(TAG,"拷贝完毕：" + uri);
           // setAppCopystatue(true);
//          startInstallApp();
             return uri;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, fileName + "not exists" + "or write err");
             return null;
        } catch (Exception e) {
             return null;
        }
    }

}
