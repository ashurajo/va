package io.virtualapp.net;

import java.util.List;

public class apkDownloadInfo{
    /**
     * {
     {
     "message":"6001",
     "code":"success",
     "list":[
     {
     "Icon":"null",
     "download_url":"/sites/default/files/2019-01/YouTube.apk",
     "apkname":"YouTubeapk",
     "VersionCode":"1245563340",
     "appname":"com.google.android.apps.youtube.app.YouTubeApplication",
     "Package":"com.google.android.youtube",
     "VersionName":"12.45.56"
     }
     ]
     }
     */
    String message ;
    String code;
    List<apkInfoDetail> list ;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<apkInfoDetail> getList() {
        return list;
    }

    public void setList(List<apkInfoDetail> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return "apkDownloadInfo{" +
                "message='" + message + '\'' +
                ", code='" + code + '\'' +
                ", list=" + list +
                '}';
    }
}
