package io.virtualapp.home.fragment;

import io.virtualapp.net.apkDownloadInfo;
import retrofit2.Call;
import retrofit2.Response;

public interface appInfoNetInfoCallback {

    void onSucssce(Response<apkDownloadInfo> response);

    void onError(Call<apkDownloadInfo> call, Throwable t);
}
