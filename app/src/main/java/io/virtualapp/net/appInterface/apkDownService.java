package io.virtualapp.net.appInterface;

import io.virtualapp.net.apkDownloadInfo;
import retrofit2.Call;
import retrofit2.http.GET;

public interface apkDownService {
    @GET("/apkParseInfo")
    Call<apkDownloadInfo> getAplListInfo();
}
