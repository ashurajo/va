package io.virtualapp.home;

import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.util.List;

import io.virtualapp.VCommends;
import io.virtualapp.home.fragment.appInfoNetInfoCallback;
import io.virtualapp.home.repo.AppDataSource;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.AppRepository;
import io.virtualapp.net.BaseUrl;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Lody shezhi
 */
public class ListAppPresenterImpl implements ListAppContract.ListAppPresenter {

	private Activity mActivity;
	private ListAppContract.ListAppView mView;
	private AppDataSource mRepository;

	private File from;
    private int type;
    private  appInfoNetInfoCallback callBack;
	public ListAppPresenterImpl(Activity activity, ListAppContract.ListAppView view, File fromWhere,int type) {
		mActivity = activity;
		mView = view;
		mRepository = new AppRepository(activity);
		mView.setPresenter(this);
		this.from = fromWhere;
		this.type = type;
	}
	public void setCallBack(appInfoNetInfoCallback callBack){
			this .callBack = callBack;
	}
	@Override
	public void start() {
		mView.setPresenter(this);
		mView.startLoading();
		if(type==0){
			if (from == null)
				mRepository.getInstalledApps(mActivity).done(mView::loadFinish);
			else
				mRepository.getStorageApps(mActivity, from).done(mView::loadFinish);
		}else {
			    mView.loadFinish(mRepository.getNetApps(mActivity,callBack));
		}

	}
}
