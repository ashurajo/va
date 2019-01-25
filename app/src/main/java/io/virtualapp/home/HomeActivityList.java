package io.virtualapp.home;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.oem.OemPermissionHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.nestedadapter.SmartRecyclerAdapter;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.adapters.LaunchpadAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.models.AddAppButton;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfo;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.EmptyAppData;
import io.virtualapp.net.BaseUrl;
import io.virtualapp.net.apkDownloadInfo;
import io.virtualapp.net.appInterface.apkDownService;
import io.virtualapp.shadowsocks.core.AppProxyManager;
import io.virtualapp.shadowsocks.core.LocalVpnService;
import io.virtualapp.utils.StringUtils;
import io.virtualapp.widgets.TwoGearsView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeActivityList extends VActivity {

    private static final String TAG = HomeActivityList.class.getSimpleName();
    /**
     * 复制文件到内存
     * @param context
     * @param fileName 复制的文件名
     * @param path  保存的目录路径
     * 针对7.0权限做处理,zanshi
     * @return
     */
    public  void copyAssetsFile(Context context, String fileName, String path) {
        try {
            InputStream mInputStream = context.getAssets().open(fileName);
            File file = new File(path);
            if (!file.exists()) {
                file.mkdir();
            }
            File mFile = new File(path + File.separator + "YouTube.apk");
            if(!mFile.exists())
                mFile.createNewFile();
            Log.e(TAG,"start-copy");
            FileOutputStream mFileOutputStream = new FileOutputStream(mFile);
            byte[] mbyte = new byte[1024*2];
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
            Log.e(TAG,"copy-end：" + uri);
            StringUtils.setAppCopystatue(this,true,"test");
            //startInstallApp();
            // return uri;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, fileName + "not exists" + "or write err");
            // return null;
        } catch (Exception e) {
            // return null;
        }
    }

    //进行网络请求
    //message=6001是正常 6002看返回的code信息
    private void getAppInfoFromNet() {
        String baseUrl = BaseUrl.baseUrl;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apkDownService movieService = retrofit.create(apkDownService.class);
        Call<apkDownloadInfo> call = movieService.getAplListInfo();
        call.enqueue(new Callback<apkDownloadInfo>() {
            @Override
            public void onResponse(Call<apkDownloadInfo> call, Response<apkDownloadInfo> response) {
               // resultTV.setText(response.body().toString());

                if(response.body().getMessage().equals("6001")){
                    Log.e("liu1","response-"+response.body().getList().toString());
                }

            }

            @Override
            public void onFailure(Call<apkDownloadInfo> call, Throwable t) {

            }

        });
    }

    @Override
    protected void onResume() {

        getAppInfoFromNet();
        super.onResume();

    }
}
