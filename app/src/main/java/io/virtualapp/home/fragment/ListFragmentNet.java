package io.virtualapp.home.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VFragment;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.ListAppContract;
import io.virtualapp.home.ListAppPresenterImpl;
import io.virtualapp.home.adapters.CloneAppListAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.models.AppInfo;
import io.virtualapp.net.apkDownloadInfo;
import io.virtualapp.net.apkInfoDetail;
import io.virtualapp.widgets.DragSelectRecyclerView;
import retrofit2.Call;
import retrofit2.Response;


public class ListFragmentNet extends VFragment<ListAppContract.ListAppPresenter> implements ListAppContract.ListAppView{
    private DragSelectRecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private CloneAppListAdapter mAdapter;
    public  List<AppInfo> getInfoList = new ArrayList<>();
    private  ListAppPresenterImpl    listAppPresenter;
    public static ListFragmentNet newInstance() {
        ListFragmentNet fragment = new ListFragmentNet();
        Log.e("liuheng2","selectFrom--");
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_app, null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mRecyclerView = view.findViewById(R.id.select_app_recycler_view);
        mProgressBar = view.findViewById(R.id.select_app_progress_bar);
        Button mInstallButton = view.findViewById(R.id.select_app_install_btn);
        mInstallButton.setVisibility(View.GONE);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, OrientationHelper.VERTICAL));
        mRecyclerView.addItemDecoration(new ItemOffsetDecoration(VUiKit.dpToPx(getContext(), 2)));
        mAdapter = new CloneAppListAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(new CloneAppListAdapter.ItemEventListener() {
            @Override
            public void onItemClick(AppInfo info, int position) {
                int count = mAdapter.getSelectedCount();
                if (!mAdapter.isIndexSelected(position)) {
                    if (count >= 9) {
                        Toast.makeText(getContext(), R.string.install_too_much_once_time, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                mAdapter.toggleSelected(position);

            }

            @Override
            public boolean isSelectable(int position) {
                return mAdapter.isIndexSelected(position) || mAdapter.getSelectedCount() < 9;
            }

            @Override
            public void onButtonRightClick(AppInfo appData, int position) {
                //todo 开始下载到本地目录
            }
        });
        mAdapter.setSelectionListener(count -> {

        });
//        mInstallButton.setOnClickListener(v -> {
//            Integer[] selectedIndices = mAdapter.getSelectedIndices();
//            ArrayList<AppInfoLite> dataList = new ArrayList<AppInfoLite>(selectedIndices.length);
//            for (int index : selectedIndices) {
//                AppInfo info = mAdapter.getItem(index);
//                Log.e("liuheng2","-"+info.packageName);  //
//                Log.e("liuheng2","-"+info.path);         //
//                Log.e("liuheng2","-"+info.cloneCount);   //
//                Log.e("liuheng2","-"+info.cloneMode);    //
//                Log.e("liuheng2","-"+info.icon);         //
//                Log.e("liuheng2","-"+info.name);         //
//                Log.e("liuheng2","-"+info.requestedPermissions.length); //权限的类型数组
//                Log.e("liuheng2","-"+info.targetSdkVersion); //   谷歌空间包名---com.excean.gspace
//                dataList.add(new AppInfoLite(info));
//            }
//            Intent data = new Intent();
//            data.putParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST, dataList);
//            getActivity().setResult(Activity.RESULT_OK, data);
//            getActivity().finish();
//        });
        listAppPresenter = new ListAppPresenterImpl(getActivity(), this, null,1);
        handleAppinfo();
        listAppPresenter.start();

    }

    @Override
    public void startLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void loadFinish(List<AppInfo> infoList) {
        if(!isAttach()){
            return;
        }
//        Log.e("liu1","infoList"+getInfoList.toString());
//        mAdapter.setList(getInfoList);
//        mRecyclerView.setDragSelectActive(false, 0);
//        mAdapter.setSelected(0, false);
//        mProgressBar.setVisibility(View.GONE);
//        mRecyclerView.setVisibility(View.VISIBLE);

    }

    @Override
    public void setPresenter(ListAppContract.ListAppPresenter presenter) {
        this.mPresenter = presenter;
    }

    //处理接口回调过来的数据
    public void handleAppinfo(){
        listAppPresenter.setCallBack(new appInfoNetInfoCallback() {
            @Override
            public void onSucssce(Response<apkDownloadInfo> response) {

                getInfoList.clear();
                if(response.body().getMessage().equals("6001")){
                    List<apkInfoDetail>    apkDownloadInfos =   response.body().getList();
                    for (apkInfoDetail detail:apkDownloadInfos ) {
                        AppInfo  appInfo = new AppInfo();
                        appInfo.packageName = detail.getPackage();
                        appInfo.name = detail.getApkname();
                        appInfo.path = detail.getDownload_url();
                        appInfo.cloneMode = false;
                        appInfo.cloneCount=0;
                        getInfoList.add(appInfo);
                    }
                    mAdapter.setList(getInfoList);
                    mRecyclerView.setDragSelectActive(false, 0);
                    mAdapter.setSelected(0, false);
                    mProgressBar.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onError(Call<apkDownloadInfo> call, Throwable t) {

            }
        });
    }
}
