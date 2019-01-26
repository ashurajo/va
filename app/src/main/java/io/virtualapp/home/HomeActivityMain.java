package io.virtualapp.home;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
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

import org.jdeferred.DoneCallback;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.virtualapp.App;
import io.virtualapp.BuildConfig;
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

public class HomeActivityMain extends VActivity implements  HomeContract.HomeView,LocalVpnService.onStatusChangedListener{

    private static final String TAG = HomeActivityMain.class.getSimpleName();
    private HomeContract.HomePresenter mPresenter;
    private ListAppContract.ListAppPresenter mresenter2;
    private TwoGearsView mLoadingView;
    private RecyclerView mLauncherView;
    private View mMenuView;
    private PopupMenu mPopupMenu;
    private View mBottomArea;
    private TextView mEnterSettingTextView;
    private View mDeleteAppBox;
    private TextView mDeleteAppTextView;
    private LaunchpadAdapter mLaunchpadAdapter;
    private Handler mUiHandler;
    //遮罩  YouTube
    private RelativeLayout relative_framen;
//  public static void goHome(Context context) {
//        Intent intent = new Intent(context, HomeActivityMain.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.putExtra("from","splash");
//        context.startActivity(intent);
//    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_main);
//        Looper.myQueue().addIdleHandler(() -> {
//            return false; //false 表示只监听一次IDLE事件,之后就不会再执行这个函数了.
//        });
        onInit();
    }
    /**
     *
     * VAConfig.gradle 中的ApplicationID,icon,appname等修改后，全局自动同步
     * 用BuildConfig.APPLICATION_ID ，来设置sp的唯一性，
     * fileprovider。。使用id来区别唯一性，下载到本地的apk也是使用APPLICATION_ID + ".apk"来命名的
     * 流程
     * --后台上传apk,脚本给修改相关信息，执行此代码开-下载apk到本地，用applicationID命名
     * --导入到va中
     * --提取apk相关信息
     * --添加Google服务
     * --添加VPN服务
     * --自动化启动
     * --进入apk
     * --退出流程，设置tag，区分退出的各种情况
     * --ok
     * todo 1.手机适配，设计vpn，权限，下载文件等; 2.apk导入流程优化; 3.多个依托界面操作的逻辑修改; 4.多开机制;5.谷歌服务不同机型处理
     *
     */
    private void onInit(){
        mUiHandler = new Handler(Looper.getMainLooper());
        bindViews();
        initLaunchpad();
        new HomePresenterImpl(this);
        mPresenter.check64bitEnginePermission();
        mPresenter.start();

        //开启谷歌服务
        askInstallGms();

        //do-start
        LocalVpnService.addOnStatusChangedListener(this);
        new AppProxyManager(this);
        Intent intent = LocalVpnService.prepare(this);
        if (intent == null) {
            startVPNService();
        } else {
            startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
        }

        // 自动设置好代理url，无需手动设置
        setProxyUrl("ss://chacha20:$^x4sK*^RQ@38.83.107.10:8882");
        //do-end

        //将assets中的apk复制到手机内存，并导入va中，过程会有点慢
        App.toMain = true ;
        if(!StringUtils.getAppCopystatue(this,BuildConfig.APPLICATION_ID)){
        File file = Environment.getExternalStorageDirectory();
        copyAssetsFile(this,"AddVpn.apk",""+file.getPath());
        }

        // 指定app代理--start
        Observable<List<io.virtualapp.shadowsocks.core.AppInfo>> observable = Observable.create(new ObservableOnSubscribe<List<io.virtualapp.shadowsocks.core.AppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<io.virtualapp.shadowsocks.core.AppInfo>> appInfo) {
                queryAppInfo();
                appInfo.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        Observer<List<io.virtualapp.shadowsocks.core.AppInfo>> observer = new Observer<List<io.virtualapp.shadowsocks.core.AppInfo>>() {
            @Override
            public void onSubscribe(Disposable d) {}
            @Override
            public void onNext(List<io.virtualapp.shadowsocks.core.AppInfo> aLong) {}
            @Override
            public void onError(Throwable e) {}
            @Override
            public void onComplete() {
            }
        };
        observable.subscribe(observer);
        //指定app代理--end

    }

    private  void autoInstallApp(){
        //导入本地apk成功后自动打开

            new Handler().postDelayed(() -> {

                if(mLaunchpadAdapter!=null&& mLaunchpadAdapter.getList().size()>0){

                    List<AppData>  appDatas =  mLaunchpadAdapter.getList();
                    for (AppData appData:appDatas) {
                        String apName =   appData.getName().replace(" ","");
                        if(!apName.equals("GooglePlay商店")&&!apName.equals("Google服务框架")&&!apName.equals("GooglePlay服务")){
                            //创建桌面快捷方式
                            //mPresenter.createShortcut(appData);
                            //打开app
                            mPresenter.launchApp(appData);
                            App.toMain = false;
                            finish();
                        }
                    }

                }
            },500);

    }
    /**
     * 关于自动开启vpn服务
     */
    private static final String CONFIG_URL_KEY = "CONFIG_URL_KEY";
    private static final int START_VPN_SERVICE_REQUEST_CODE = 1985;
    private static String GL_HISTORY_LOGS;
    void setProxyUrl(String ProxyUrl) {
        SharedPreferences preferences = getSharedPreferences("shadowsocksProxyUrl", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CONFIG_URL_KEY, ProxyUrl);
        editor.apply();
    }

    String readProxyUrl() {
        SharedPreferences preferences = getSharedPreferences("shadowsocksProxyUrl", MODE_PRIVATE);
        return preferences.getString(CONFIG_URL_KEY, "");
    }

    void startVPNService() {
        String ProxyUrl = readProxyUrl();
        GL_HISTORY_LOGS = null;
        LocalVpnService.ProxyUrl = ProxyUrl;
        startService(new Intent(this, LocalVpnService.class));
    }

    private  EditText et_seacher;
    private void bindViews() {
        mLoadingView = findViewById(R.id.pb_loading_app);
        mLauncherView = findViewById(R.id.home_launcher);
        mMenuView = findViewById(R.id.home_menu);
        mBottomArea = findViewById(R.id.bottom_area);
        mEnterSettingTextView = findViewById(R.id.enter_app_setting_text);
        mDeleteAppBox = findViewById(R.id.delete_app_area);
        mDeleteAppTextView = findViewById(R.id.delete_app_text);
        //遮罩
        relative_framen = findViewById(R.id.relative_framen);
        relative_framen.setVisibility(View.VISIBLE);
        //搜索
        et_seacher = findViewById(R.id.et_seacher);

        mMenuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddAppButtonClick();
            }
        });
    }

    private void initLaunchpad() {
        mLauncherView.setHasFixedSize(true);
        StaggeredGridLayoutManager   layoutManager = new StaggeredGridLayoutManager(1, OrientationHelper.VERTICAL);
        mLauncherView.setLayoutManager(layoutManager);
        mLaunchpadAdapter = new LaunchpadAdapter(this);
        SmartRecyclerAdapter   wrap = new SmartRecyclerAdapter(mLaunchpadAdapter);
        View footer = new View(this);
        footer.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VUiKit.dpToPx(this, 60)));
        wrap.setFooterView(footer);
        mLauncherView.setAdapter(wrap);
        mLauncherView.addItemDecoration(new ItemOffsetDecoration(this, R.dimen.desktop_divider));
//        ItemTouchHelper touchHelper = new ItemTouchHelper(new LauncherTouchCallback());
//        touchHelper.attachToRecyclerView(mLauncherView);


        mLaunchpadAdapter.setAppClickListener(new LaunchpadAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(int pos, AppData data) {
                if (!data.isLoading()) {
                    if (data instanceof AddAppButton) {
                        //选择要添加的app或apk
                        HomeActivityMain.this.onAddAppButtonClick();
                    }
                    //打开内部app
                    mLaunchpadAdapter.notifyItemChanged(pos);
                    mPresenter.launchApp(data);
                    finish();
                }
            }

            @Override
            public void onButtonClick(int position, AppData model) {
                //删除app
                deleteApp(position);
            }
        });

    }
    //点击添加app按钮，跳转到apk列表界面选择
    private void onAddAppButtonClick() {
        ListAppActivity.gotoListApp(this);
    }
    @Override
    public void showPermissionDialog() {
        Intent intent = OemPermissionHelper.getPermissionActivityIntent(this);
        new AlertDialog.Builder(this)
                .setTitle("Notice")
                .setMessage("You must to grant permission to allowed launch 64bit Engine.")
                .setCancelable(false)
                .setNegativeButton("GO", (dialog, which) -> {
                    try {
                        startActivity(intent);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }).show();
    }

    //HomeView-@override-start

    @Override
    public void showBottomAction() {

    }

    @Override
    public void hideBottomAction() {

    }

    @Override
    public void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
        mLoadingView.startAnim();
    }

    @Override
    public void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
        mLoadingView.stopAnim();
    }

    @Override
    public void loadFinish(List<AppData> list) {
        //去掉添加按钮
        //list.add(new AddAppButton(this));
        // Log.e("liuhengpu","2-open-"+list.size());

        mLaunchpadAdapter.setList(list);
        autoInstallApp();
        hideLoading();

    }

    @Override
    public void loadError(Throwable err) {

    }

    @Override
    public void showGuide() {

    }

    @Override
    public void addAppToLauncher(AppData model) {
        List<AppData> dataList = mLaunchpadAdapter.getList();
        boolean replaced = false;
        for (int i = 0; i < dataList.size(); i++) {
            AppData data = dataList.get(i);
            if (data instanceof EmptyAppData) {
                mLaunchpadAdapter.replace(i, model);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            mLaunchpadAdapter.add(model);
//          mLauncherView.smoothScrollToPosition(mLaunchpadAdapter.getItemCount() - 1);
            mLauncherView.smoothScrollToPosition(0);
        }

//        Log.e("liuhengpu","dataList"+dataList.size());
        if(dataList.size()>0){
            autoInstallApp();
        }
    }

    @Override
    public void removeAppToLauncher(AppData model) {
        mLaunchpadAdapter.remove(model);
    }

    @Override
    public void refreshLauncherItem(AppData model) {
        mLaunchpadAdapter.refresh(model);
    }

    @Override
    public void askInstallGms() {
        if (!GmsSupport.isOutsideGoogleFrameworkExist()) {
            return;
        }
        if (GmsSupport.isInstalledGoogleService()) {
            return;
        }
        defer().when(() -> {
            GmsSupport.installGApps(0);
        }).done((res) -> {
            mPresenter.dataChanged();
        });
    }

    @Override
    public void setPresenter(HomeContract.HomePresenter presenter) {
        mPresenter = presenter;
    }

    //HomeView-@override-end

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VCommends.REQUEST_SELECT_APP) {
            if (resultCode == RESULT_OK && data != null) {
                List<AppInfoLite> appList = data.getParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST);
                if (appList != null) {
                    for (AppInfoLite info : appList) {
                        mPresenter.addApp(info);
                    }
                }
            }
        } else if (requestCode == VCommends.REQUEST_PERMISSION) {
            if (resultCode == RESULT_OK) {
                String packageName = data.getStringExtra("pkg");
                int userId = data.getIntExtra("user_id", -1);
                VActivityManager.get().launchApp(userId, packageName);
                App.toMain = false;
            }
        }
        //开启了VPN
        else if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPNService();
            } else {
                onLogReceived("canceled.");
            }
            return;
        }
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        onLogReceived(status);
    }

    @Override
    public void onLogReceived(String logString) {

    }
    /**
     * 复制文件到内存
     * @param context
     * @param apkpath 下载路径
     * @param path  保存的目录路径
     * 针对7.0权限做处理
     * @return
     */
    public  void copyAssetsFile(Context context, String apkpath, String path) {

//        new Thread(() -> {
            try {
                InputStream mInputStream = context.getAssets().open(apkpath);
                //apk 下载的url
                //String url = BaseUrl.baseUrl + apkpath;
                //  HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                //  conn.connect();
                //  InputStream mInputStream = conn.getInputStream();
                File mFile = new File(path + File.separator + BuildConfig.APPLICATION_ID+".apk");
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
                        //包名.fileprovider适配7.0以上的，和配置文件保持一致
                        uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID +".fileprovider", mFile);
                    }else{
                        uri = Uri.fromFile(mFile);
                    }
                }catch (ActivityNotFoundException anfe){
                    Log.e(TAG,anfe.getMessage());
                }
                MediaScannerConnection.scanFile(context, new String[]{mFile.getAbsolutePath()}, null, null);
                Log.e(TAG,"copy-end：" + uri);
                StringUtils.setAppCopystatue(HomeActivityMain.this,true,BuildConfig.APPLICATION_ID);
                //apk下载完成，获取包名信息
                getPackageNameSd(mFile.getPath());
                //Log.e("liuhengpu","apkpackname--"+apkpackname);
                 startInstallApp();
                 //自动开启apk
                // return uri;
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, apkpath + "not exists" + "or write err");
                // return null;
            } catch (Exception e) {
                // return null;
            }
//        }).start();

    }

    public String getPackageNameSd(String apkPath){
        PackageManager pm = getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (info != null){
         //既然获取了ApplicationInfo,那么和应用相关的一些信息就都可以获取了,具体可以获取什么大家可以看看ApplicationInfo这个类
            ApplicationInfo appInfo = info.applicationInfo;
            InstallAppinfo.packageName =appInfo.packageName;
           // Log.e("liuhengpu","appInfo--"+InstallAppinfo.packageName);
            return appInfo.packageName;
        }
        return "";
    }
    /**
     * 手动添加的信息（ListAppFragment中科获取相关信息），
     * 后期可以优化，动态获取，需修改va中apk选择流程多个类别
     */
    private  static AppInfo InstallAppinfo =new AppInfo();
    static {
        InstallAppinfo.packageName = "";
        InstallAppinfo.path = "/storage/emulated/0/./"+BuildConfig.APPLICATION_ID+".apk";
        InstallAppinfo.cloneCount = 0;
        InstallAppinfo.cloneMode = false;
        InstallAppinfo.name = BuildConfig.APPLICATION_ID;
        InstallAppinfo.targetSdkVersion=24;
    }
    //导入apk到va
    public void startInstallApp(){
        AppInfoLite  infoLite =  new AppInfoLite(InstallAppinfo);
        mPresenter.addApp(infoLite);
    }

    //删除app
    private void deleteApp(int position) {
        AppData data = mLaunchpadAdapter.getList().get(position);
        new AlertDialog.Builder(this)
                .setTitle(R.string.tip_delete)
                .setMessage(getString(R.string.text_delete_app, data.getName()))
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    mPresenter.deleteApp(data);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
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

                if(response.body().getMessage().equals("6001")){
                   // Log.e("liu1","response-"+response.body().getList().toString());
                    if(!StringUtils.getAppCopystatue(getApplicationContext(),BuildConfig.APPLICATION_ID)){
                        File file = Environment.getExternalStorageDirectory();
                          // copyAssetsFile(getApplicationContext(),apkDownloadPath,file.getPath(),apkname);
                         //  Log.e("liu1","2--"+apkDownloadPath+"--"+apkname);
                    }
                }else {

                }
            }
            @Override
            public void onFailure(Call<apkDownloadInfo> call, Throwable t) {
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        //getAppInfoFromNet();
    }
    //遍历apk信息
    public void queryAppInfo() {
        PackageManager pm = this.getPackageManager(); // 获得PackageManager对象
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));
        if (AppProxyManager.Instance.mlistAppInfo != null) {
            AppProxyManager.Instance.mlistAppInfo.clear();
            for (ResolveInfo reInfo : resolveInfos) {
                String pkgName = reInfo.activityInfo.packageName; // 获得应用程序的包名
                String appLabel = (String) reInfo.loadLabel(pm); // 获得应用程序的Label
                Drawable icon = reInfo.loadIcon(pm); // 获得应用程序图标
                io.virtualapp.shadowsocks.core.AppInfo appInfo = new io.virtualapp.shadowsocks.core.AppInfo();
                appInfo.setAppLabel(appLabel);
                appInfo.setPkgName(pkgName);
                appInfo.setAppIcon(icon);
                if (!appInfo.getPkgName().equals("com.vm.shadowsocks"))//App本身会强制加入代理列表，这个是集成vpn之前源码的包名
                    AppProxyManager.Instance.mlistAppInfo.add(appInfo);
                if(!appInfo.getPkgName().equals(BuildConfig.APPLICATION_ID)) {
                    AppProxyManager.Instance.mlistAppInfo.add(appInfo);
                }

            }
            //第一次没有给va开代理的情况判断
            if(!AppProxyManager.Instance.isAppProxy(BuildConfig.APPLICATION_ID)){
                AppProxyManager.Instance.addProxyApp(BuildConfig.APPLICATION_ID);
            }

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //home键点击退出后，返回界面跳转参数处理
        App.toMain = true ;
        autoInstallApp();
    }
}
