package io.virtualapp.net;

public class apkInfoDetail {
    String Icon;
    String apkname;
    String Package;
    String VersionName;
    String VersionCode;
    String download_url;

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public String getIcon() {
        return Icon;
    }

    public void setIcon(String icon) {
        Icon = icon;
    }

    public String getApkname() {
        return apkname;
    }

    public void setApkname(String apkname) {
        this.apkname = apkname;
    }

    public String getPackage() {
        return Package;
    }

    public void setPackage(String aPackage) {
        Package = aPackage;
    }

    public String getVersionName() {
        return VersionName;
    }

    public void setVersionName(String versionName) {
        VersionName = versionName;
    }

    public String getVersionCode() {
        return VersionCode;
    }

    public void setVersionCode(String versionCode) {
        VersionCode = versionCode;
    }

    @Override
    public String toString() {
        return "apkInfoDetail{" +
                "Icon='" + Icon + '\'' +
                ", appname='" + apkname + '\'' +
                ", Package='" + Package + '\'' +
                ", VersionName='" + VersionName + '\'' +
                ", VersionCode='" + VersionCode + '\'' +
                ", download_url='" + download_url + '\'' +
                '}';
    }
}
