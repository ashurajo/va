package io.virtualapp.home;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.ChooseTypeAndAccountActivity;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.oem.OemPermissionHelper;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.nestedadapter.SmartRecyclerAdapter;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.autoclick.AutoClickAccessibilityService;
import io.virtualapp.autoclick.BaseAccessibilityService;
import io.virtualapp.autoclick.helper.OpenAccessibilitySettingHelper;
import io.virtualapp.home.adapters.LaunchpadAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.device.DeviceSettingsActivity;
import io.virtualapp.home.location.LocationSettingsActivity;
import io.virtualapp.home.models.AddAppButton;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfo;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.EmptyAppData;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.shadowsocks.core.AppProxyManager;
import io.virtualapp.shadowsocks.core.LocalVpnService;
import io.virtualapp.shadowsocks.ui.MainActivity;
import io.virtualapp.utils.StringUtils;
import io.virtualapp.widgets.TwoGearsView;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_DRAG;
import static android.support.v7.widget.helper.ItemTouchHelper.DOWN;
import static android.support.v7.widget.helper.ItemTouchHelper.END;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static android.support.v7.widget.helper.ItemTouchHelper.START;
import static android.support.v7.widget.helper.ItemTouchHelper.UP;

/**
 * @author Liu
 */
public class HomeActivity extends VActivity implements HomeContract.HomeView ,LocalVpnService.onStatusChangedListener {

    private static final String TAG = HomeActivity.class.getSimpleName();
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
    public static void goHome(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }

    /**
     * 初始化说明
     * splashActivity中申请文件读写权限，进入home，获取vpn服务权限，
     * home初始化的过程中，将assets下的apk下载到本地，然后再导入va中，
     * 然后开启apk
     * todo 进入apk后退出的监听,多次返回退出
     * todo 单独代理的设置
     * todo 导入apk的流程优化
     * todo 屏蔽home页导入列表，以及数据显示的操作，抽取操作类的方法，此块关联较多
     *
     */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Looper.myQueue().addIdleHandler(() -> {
            Log.i("IdleHandler","queueIdle");
            onInit();
            return false; //false 表示只监听一次IDLE事件,之后就不会再执行这个函数了.
        });

    }

    //初始化封装,部分优化
    private void onInit(){
        mUiHandler = new Handler(Looper.getMainLooper());
        bindViews();
        initLaunchpad();
        initMenu();
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

       // Log.e("liuheng2","1"+getContext().getAssets().getLocales()[0].toString());
        //将assets中的apk复制到手机内存，并导入va中，过程会有点慢
        if(!StringUtils.getAppCopystatue(getContext(),"test")){
            File file = Environment.getExternalStorageDirectory();
            copyAssetsFile(getApplicationContext(),"YouTube.apk",""+file.getPath());
//           Log.e("liuheng2","2--");
        }

        //指定app代理--start
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

        //导入本地apk成功后自动打开
        //Log.e("liuheng2","3"+getAppCopystatue());
        if(StringUtils.getAppCopystatue(this,"test")){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mLaunchpadAdapter!=null&& mLaunchpadAdapter.getList().size()>1){
                        //Log.e("AutoClick","postDelayed");
                        List<AppData>  appDatas =  mLaunchpadAdapter.getList();
                        for (AppData appData:appDatas) {
                           // Log.e("liuhengpu",""+appData.getPackageName()+"-");
                            if(appData.getPackageName().equals("com.google.android.youtube")){
                                //创建桌面快捷方式
                                mPresenter.createShortcut(appData);
                                //打开app
                                mPresenter.launchApp(appData);
                                return;
                            }
                        }

                    }
                }
            },300);
        }

    }

    private File getSelectFrom() {
            String selectFrom = "/storage/emulated/0";
            if (selectFrom != null) {
                return new File(selectFrom);
            }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
       // File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "va");//参数2是文件名称
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private  static  boolean temp = false;
    private void initMenu() {
        mPopupMenu = new PopupMenu(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Light), mMenuView);
        Menu menu = mPopupMenu.getMenu();
        setIconEnable(menu, true);

        menu.add(R.string.menu_accounts).setIcon(R.drawable.ic_account).setOnMenuItemClickListener(item -> {
            List<VUserInfo> users = VUserManager.get().getUsers();
            List<String> names = new ArrayList<>(users.size());
            for (VUserInfo info : users) {
                names.add(info.name);
            }
            CharSequence[] items = new CharSequence[names.size()];
            for (int i = 0; i < names.size(); i++) {
                items[i] = names.get(i);
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.choose_user_title)
                    .setItems(items, (dialog, which) -> {
                        VUserInfo info = users.get(which);
                        Intent intent = new Intent(this, ChooseTypeAndAccountActivity.class);
                        intent.putExtra(ChooseTypeAndAccountActivity.KEY_USER_ID, info.id);
                        startActivity(intent);
                    }).show();
            return false;
        });
        menu.add(R.string.kill_all_app).setIcon(R.drawable.ic_speed_up).setOnMenuItemClickListener(item -> {
            VActivityManager.get().killAllApps();
            Toast.makeText(this, "Memory release complete!", Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.add(R.string.menu_gms).setIcon(R.drawable.ic_google).setOnMenuItemClickListener(item -> {
            askInstallGms();
            return true;
        });

        menu.add(R.string.menu_mock_phone).setIcon(R.drawable.ic_device).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, DeviceSettingsActivity.class));
            return true;
        });

        menu.add(R.string.virtual_location).setIcon(R.drawable.ic_location).setOnMenuItemClickListener(item -> {
            if (mPresenter.getAppCount() == 0) {
                Toast.makeText(this, R.string.tip_no_app, Toast.LENGTH_SHORT).show();
                return false;
            }
            startActivity(new Intent(this, LocationSettingsActivity.class));
            return true;
        });

        menu.add(getString(R.string.about)).setIcon(R.drawable.ic_about).setOnMenuItemClickListener(item -> {
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle(R.string.about_title);
//            builder.setMessage(R.string.about_msg);
//            builder.setPositiveButton(android.R.string.ok, (dlg, id) -> {
//                dlg.dismiss();
//            });
//            builder.show();
//          启动Clean Mvp
//          Toast.makeText(HomeActivity.this,"开始启动Clean Mvp",Toast.LENGTH_SHORT).show();

            if(!temp){
                relative_framen.setVisibility(View.GONE);
                temp = true ;
            }else {
                relative_framen.setVisibility(View.VISIBLE);
                temp = false;
            }

            return true;
        });

        //无障碍检查
        menu.add(getString(R.string.permission)).setIcon(R.drawable.ic_about).setOnMenuItemClickListener(item -> {

            if (!OpenAccessibilitySettingHelper.isAccessibilitySettingsOn(this,
                    AutoClickAccessibilityService.class.getName())) {// 判断服务是否开启
                OpenAccessibilitySettingHelper.jumpToSettingPage(this);// 跳转到开启页面
                android.util.Log.i("AutoClick", "-SplashActivity-start-");
            }
            return true;
        });
        menu.add(getString(R.string.main)).setIcon(R.drawable.ic_about).setOnMenuItemClickListener(item -> {

           Intent intent = new Intent(HomeActivity.this,MainActivity.class);
           startActivity(intent);
            return true;
        });
        mMenuView.setOnClickListener(v ->
                mPopupMenu.show());

    }

    private static void setIconEnable(Menu menu, boolean enable) {
        try {
            @SuppressLint("PrivateApi")
            Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);
            m.invoke(menu, enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindViews() {
        mLoadingView = findViewById(R.id.pb_loading_app);
        mLauncherView = findViewById(R.id.home_launcher);
        mMenuView = findViewById(R.id.home_menu);
        mBottomArea = findViewById(R.id.bottom_area);
        mEnterSettingTextView = findViewById(R.id.enter_app_setting_text);
        mDeleteAppBox = findViewById(R.id.delete_app_area);
        mDeleteAppTextView = findViewById(R.id.delete_app_text);
         // 遮罩va主界面
        relative_framen = findViewById(R.id.relative_framen);
        relative_framen.setVisibility(View.GONE);
    }

    private void initLaunchpad() {
        mLauncherView.setHasFixedSize(true);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL);
        mLauncherView.setLayoutManager(layoutManager);
        mLaunchpadAdapter = new LaunchpadAdapter(this);
        SmartRecyclerAdapter wrap = new SmartRecyclerAdapter(mLaunchpadAdapter);
        View footer = new View(this);
        footer.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VUiKit.dpToPx(this, 60)));
        wrap.setFooterView(footer);
        mLauncherView.setAdapter(wrap);
        mLauncherView.addItemDecoration(new ItemOffsetDecoration(this, R.dimen.desktop_divider));
//        ItemTouchHelper touchHelper = new ItemTouchHelper(new LauncherTouchCallback());
//        touchHelper.attachToRecyclerView(mLauncherView);


//        mLaunchpadAdapter.setAppClickListener((pos, data) -> {
//            if (!data.isLoading()) {
//                if (data instanceof AddAppButton) {
//                    //选择要添加的app或apk
//                    HomeActivity.this.onAddAppButtonClick();
//                    Log.e(TAG, "initLaunchpad:1 -" + data.getPackageName() + "--" + data.getName() + "--" + data.getUserId());
//                }
//                //打开内部app
//                mLaunchpadAdapter.notifyItemChanged(pos);
//                mPresenter.launchApp(data);
//                //add
//                String nowAppPackagesName = data.getPackageName();
//                Log.e(TAG, "initLaunchpad:2 -" + data.getPackageName() + "--" + data.getName() + "--" + data.getUserId());
//            }
//        });


    }

    //点击添加app按钮，跳转到apk列表界面选择
    private void onAddAppButtonClick() {
        ListAppActivity.gotoListApp(this);
    }

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

    //进入设置界面
    private void enterAppSetting(int position) {
        AppData model = mLaunchpadAdapter.getList().get(position);
        if (model instanceof PackageAppData || model instanceof MultiplePackageAppData) {
            mPresenter.enterAppSetting(model);
        }
    }

    @Override
    public void setPresenter(HomeContract.HomePresenter presenter) {
        mPresenter = presenter;
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

    @Override
    public void showBottomAction() {
        mBottomArea.setTranslationY(mBottomArea.getHeight());
        mBottomArea.setVisibility(View.VISIBLE);
        mBottomArea.animate().translationY(0).setDuration(500L).start();
    }

    @Override
    public void hideBottomAction() {
        mBottomArea.setTranslationY(0);
        ObjectAnimator transAnim = ObjectAnimator.ofFloat(mBottomArea, "translationY", 0, mBottomArea.getHeight());
        transAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }
            @Override
            public void onAnimationEnd(Animator animator) {
                mBottomArea.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationCancel(Animator animator) {
                mBottomArea.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        transAnim.setDuration(500L);
        transAnim.start();
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
        list.add(new AddAppButton(this));
        mLaunchpadAdapter.setList(list);

        if(StringUtils.getAppCopystatue(this,"test")){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mLaunchpadAdapter!=null&& mLaunchpadAdapter.getList().size()>1){

                        List<AppData>  appDatas =  mLaunchpadAdapter.getList();
                        for (AppData appData:appDatas) {
                            Log.e("liuhengpu",""+appData.getPackageName()+"-");
                            if(appData.getPackageName().equals("com.google.android.youtube")){
                                mPresenter.launchApp(appData);
                                return;
                            }
                        }
                    }
                }
            },200);
        }
        hideLoading();
    }

    @Override
    public void loadError(Throwable err) {
        err.printStackTrace();
        hideLoading();
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
            mLauncherView.smoothScrollToPosition(mLaunchpadAdapter.getItemCount() - 1);
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
            Log.e("liu2","====1");
            return;
        }
        if (GmsSupport.isInstalledGoogleService()) {
            Log.e("liu2","====2");
            return;
        }
        defer().when(() -> {
            GmsSupport.installGApps(0);
        }).done((res) -> {
            Log.e("liu2","====4");
            mPresenter.dataChanged();
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VCommends.REQUEST_SELECT_APP) {
            if (resultCode == RESULT_OK && data != null) {
                List<AppInfoLite> appList = data.getParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST);
                if (appList != null) {
                    for (AppInfoLite info : appList) {
                        mPresenter.addApp(info);
                        Log.e(TAG,""+info);
                    }
                }
            }
        } else if (requestCode == VCommends.REQUEST_PERMISSION) {
            if (resultCode == RESULT_OK) {
                String packageName = data.getStringExtra("pkg");
                int userId = data.getIntExtra("user_id", -1);
                VActivityManager.get().launchApp(userId, packageName);
            }
        }else if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPNService();
                Log.e("liuhengpu","1");
            } else {
                onLogReceived("canceled.");
                Log.e("liuhengpu","2");
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
private  void  setProxy(String proxy){

        if(StringUtils.isString(proxy)){
            //得到正确的代理地址，然后指定app设置唯一代理
            StringUtils.setAppCopystatue(this,true,"test");
            //根据包名判断是否开启指定代理（只针对该app的代理，区别于全局代理）
            String packagesName = getPackageName();
            Boolean isAppproxy = AppProxyManager.Instance.isAppProxy(packagesName);
            if(isAppproxy){
                AppProxyManager.Instance.addProxyApp(packagesName);
            }else {
                Toast.makeText(this,"请开启代理，否则无法使用",Toast.LENGTH_SHORT).show();
            }

        }else{
            File file = getSelectFrom();

            if(FileUtils.isExist(file.getPath())){
                //确定存在此路径的的处理
            }
        }
}

//    private class LauncherTouchCallback extends ItemTouchHelper.SimpleCallback {
//
//        int[] location = new int[2];
//        boolean upAtDeleteAppArea;
//        boolean upAtEnterSettingArea;
//        RecyclerView.ViewHolder dragHolder;
//
//        LauncherTouchCallback() {
//            super(UP | DOWN | LEFT | RIGHT | START | END, 0);
//        }
//
//        @Override
//        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
//            return 0;
//        }
//
//        @Override
//        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
//            try {
//                AppData data = mLaunchpadAdapter.getList().get(viewHolder.getAdapterPosition());
//                if (!data.canReorder()) {
//                    return makeMovementFlags(0, 0);
//                }
//            } catch (IndexOutOfBoundsException e) {
//                e.printStackTrace();
//            }
//            return super.getMovementFlags(recyclerView, viewHolder);
//        }
//
//        @Override
//        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
//            int pos = viewHolder.getAdapterPosition();
//            int targetPos = target.getAdapterPosition();
//            mLaunchpadAdapter.moveItem(pos, targetPos);
//            return true;
//        }
//
//        @Override
//        public boolean isLongPressDragEnabled() {
//            return true;
//        }
//
//        @Override
//        public boolean isItemViewSwipeEnabled() {
//            return false;
//        }
//
//        @Override
//        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
//            if (viewHolder instanceof LaunchpadAdapter.ViewHolder) {
//                if (actionState == ACTION_STATE_DRAG) {
//                    if (dragHolder != viewHolder) {
//                        dragHolder = viewHolder;
//                        viewHolder.itemView.setScaleX(1.2f);
//                        viewHolder.itemView.setScaleY(1.2f);
//                        if (mBottomArea.getVisibility() == View.GONE) {
//                            showBottomAction();
//                        }
//                    }
//                }
//            }
//            super.onSelectedChanged(viewHolder, actionState);
//        }
//
//        @Override
//        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current, RecyclerView.ViewHolder target) {
//            if (upAtEnterSettingArea || upAtDeleteAppArea) {
//                return false;
//            }
//            try {
//                AppData data = mLaunchpadAdapter.getList().get(target.getAdapterPosition());
//                return data.canReorder();
//            } catch (IndexOutOfBoundsException e) {
//                //ignore
//                Log.e(TAG,""+e.toString()+"");
//            }
//            return false;
//        }
//
//        @Override
//        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
//            if (viewHolder instanceof LaunchpadAdapter.ViewHolder) {
//                viewHolder.itemView.setScaleX(1f);
//                viewHolder.itemView.setScaleY(1f);
//            }
//            super.clearView(recyclerView, viewHolder);
//            if (dragHolder == viewHolder) {
//                if (mBottomArea.getVisibility() == View.VISIBLE) {
//                    mUiHandler.postDelayed(HomeActivity.this::hideBottomAction, 200L);
//                    if (upAtEnterSettingArea) {
////                        enterAppSetting(viewHolder.getAdapterPosition());
////                        设置快捷方式
//                        createShortcut(viewHolder.getAdapterPosition());
//                    } else if (upAtDeleteAppArea) {
//                        deleteApp(viewHolder.getAdapterPosition());
//                    }
//                }
//                dragHolder = null;
//            }
//        }
//
//        private void createShortcut(int position) {
//            AppData model = mLaunchpadAdapter.getList().get(position);
//            if (model instanceof PackageAppData || model instanceof MultiplePackageAppData) {
//                mPresenter.createShortcut(model);
//            }
//        }
//
//        @Override
//        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
//        }
//
//        @Override
//        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
//            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
//            if (actionState != ACTION_STATE_DRAG || !isCurrentlyActive) {
//                return;
//            }
//            View itemView = viewHolder.itemView;
//            itemView.getLocationInWindow(location);
//            int x = (int) (location[0] + dX);
//            int y = (int) (location[1] + dY);
//
//            mBottomArea.getLocationInWindow(location);
//            int baseLine = location[1] - mBottomArea.getHeight();
//            if (y >= baseLine) {
//                mDeleteAppBox.getLocationInWindow(location);
//                int deleteAppAreaStartX = location[0];
//                if (x < deleteAppAreaStartX) {
//                    upAtEnterSettingArea = true;
//                    upAtDeleteAppArea = false;
//                    mEnterSettingTextView.setTextColor(Color.parseColor("#0099cc"));
//                    mDeleteAppTextView.setTextColor(Color.BLACK);
//                } else {
//                    upAtDeleteAppArea = true;
//                    upAtEnterSettingArea = false;
//                    mDeleteAppTextView.setTextColor(Color.parseColor("#0099cc"));
//                    mEnterSettingTextView.setTextColor(Color.BLACK);
//                }
//            } else {
//                    upAtEnterSettingArea = false;
//                    upAtDeleteAppArea = false;
//                    mDeleteAppTextView.setTextColor(Color.BLACK);
//                    mEnterSettingTextView.setTextColor(Color.BLACK);
//            }
//        }
//    }


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


    /**
     * 复制文件到内存
     * @param context
     * @param fileName 复制的文件名
     * @param path  保存的目录路径
     * 针对7.0权限做了处理
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
            startInstallApp();
           // return uri;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, fileName + "not exists" + "or write err");
           // return null;
        } catch (Exception e) {
           // return null;
        }
    }

    /**
     * 手动添加的信息（ListAppFragment中科获取相关信息），
     * 后期可以优化，动态获取，需修改va中apk选择流程多个类别
     */
    private  static   AppInfo  InstallAppinfo =new AppInfo();
    static {
        InstallAppinfo.packageName = "com.google.android.youtube";
        InstallAppinfo.path = "/storage/emulated/0/./YouTube.apk";
        InstallAppinfo.cloneCount = 0;
        InstallAppinfo.cloneMode = false;
        InstallAppinfo.name = "YouTube";
        InstallAppinfo.targetSdkVersion=26;
    }
    public void startInstallApp(){
        AppInfoLite  infoLite =  new AppInfoLite(InstallAppinfo);
        mPresenter.addApp(infoLite);

    }

    private boolean isAppInstall(Context mContext, String packageName){
        PackageInfo mInfo;
        try {
            mInfo = mContext.getPackageManager().getPackageInfo(packageName, 0 );
        } catch (Exception e) {
            // TODO: handle exception
            mInfo = null;
            Log.i(TAG, "没有发现安装的包名");
        }
        if(mInfo == null){
            return false;
        }else {
            return true;
        }
    }
    //判断格式
    boolean isValidUrl(String url) {
        try {
            if (url == null || url.isEmpty())
                return false;

            if (url.startsWith("ss://")) {//file path
                return true;
            } else { //url
                Uri uri = Uri.parse(url);
                if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))
                    return false;
                return uri.getHost() != null;
            }
        } catch (Exception e) {
            return false;
        }
    }
    // add
    private PackageManager mPackageManager;
    private List<String> mPackages = new ArrayList<>();

    public void goAccess(View view) {
        BaseAccessibilityService.getInstance().goAccess();
    }

    public void goApp(View view) {
        Intent intent = mPackageManager.getLaunchIntentForPackage("com.hujiang.hjclass");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void cleanProcess(View view) {
        for (String mPackage : mPackages) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", mPackage, null);
            intent.setData(uri);
            startActivity(intent);
        }
    }

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
                if(!appInfo.getPkgName().equals("io.busniess.va")) {
                    AppProxyManager.Instance.mlistAppInfo.add(appInfo);
                }
            }
            //第一次没有给va开代理的情况判断
            if(!AppProxyManager.Instance.isAppProxy("io.busniess.va")){
                AppProxyManager.Instance.addProxyApp("io.busniess.va");
            }

        }
    }

    //todo 对于从va内部应用退出的app，需要区分下
    @Override
    public void onBackPressed() {
        Log.e("start","onBackPressed");
        System.exit(0);
    }

    private  void  setVpnService(){
        String[] requestedPermissions ={};
        VpnService   vpnService = new VpnService();
        Intent  intent  = new Intent();
        Bundle  bundle = new Bundle();
        AppInfo  appInfo = new AppInfo();
        appInfo.packageName= "";
        appInfo.path ="";
        appInfo.name ="";
        appInfo.cloneMode=false;
        appInfo.cloneCount = 1;
        appInfo.name ="";
        appInfo.targetSdkVersion = 26;
        appInfo.requestedPermissions = requestedPermissions ;
        AppInfoLite  appInfoLite =  new AppInfoLite(appInfo);
        bundle.putParcelable("VaIntent",appInfoLite);
        vpnService.onBind(intent);

    }

}
