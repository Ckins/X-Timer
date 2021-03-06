package com.crossbow.app.x_timer.utils;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.crossbow.app.x_timer.service.AppUsage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by heath on 15-12-29.
 */
public class FileUtils {
    // 记录上下文
    private Context mContext;

    private List<PackageInfo> packages;

    public FileUtils(Context context) {
        this.mContext = context;
    }

    // 获取保存的监听列表
    public ArrayList<String> getAppList() {
        File file = new File("/data/data/com.crossbow.app.x_timer/files/appList");
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try {
            Gson gson = new Gson();
            return gson.fromJson(getContent(file), new
                    TypeToken<ArrayList<String> >() {}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 保存监听列表
    public void storeAppList(ArrayList<String> appList) {
        try {
            getOutputStream("appList").write(new Gson().toJson(appList).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 保存app使用状况
    public void storeAppInfo(AppUsage appUsage) {
        try {
            getOutputStream("flag_"+appUsage.getPackageName())
                    .write(new Gson().toJson(appUsage).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 读取app使用状况
    public AppUsage loadAppInfo(String pkgName) {
        //文件不存在，直接返回一个新的AppUsage
        File file = new File("/data/data/com.crossbow.app.x_timer/files/flag_" + pkgName);
        if (!file.exists()) {
            return new AppUsage(pkgName, findAppName(pkgName));
        }

        //文件存在则读取文件，转换成AppUsage
        try {
            return parseJSONWithGSON(getContent(file));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 通过GSON读取json文件
    private AppUsage parseJSONWithGSON(String json) {
        Gson gson = new Gson();
        AppUsage appUsage = gson.fromJson(json, AppUsage.class);
        return appUsage;
    }

    // 获取file的OutputStream
    private FileOutputStream getOutputStream(String fileName) {
        try {
            FileOutputStream outputStream = mContext.openFileOutput(fileName, Context.MODE_PRIVATE);

            return outputStream;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 获取file内容，返回String
    private String getContent(File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder content = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 获取所有存储的app信息，不管在不在list里
    public List<AppUsage> getAllStoredApp() {
        List<AppUsage> list = new ArrayList<>();
        File root = new File("/data/data/com.crossbow.app.x_timer/files");
        File[] files = root.listFiles();

        if (files == null || files.length == 0) return list;

        for (File file : files) {
            if (file.getName().startsWith("flag_")) {
                AppUsage usage = loadAppInfo(file.getName().substring(5, file.getName().length()));
                list.add(usage);
            }
        }

        return list;
    }

    // 删除指定应用信息
    public boolean deleteCertainAppInfo(String pkgName) {
        File root = new File("/data/data/com.crossbow.app.x_timer/files");
        File[] files = root.listFiles();

        if (files == null || files.length == 0) return false;

        for (File file : files) {
            if (file.getName().equals(pkgName)) {
                file.delete();
                return true;
            }
        }

        return false;
    }

    // 删除所有应用信息
    public boolean deleteAllAppInfo() {
        File root = new File("/data/data/com.crossbow.app.x_timer/files");
        File[] files = root.listFiles();

        if (files == null || files.length == 0) return false;

        for (File file : files) {
            if (file.getName().startsWith("flag_")) file.delete();
        }

        return true;
    }

    // 删除所有files下文件，debug用
    public boolean deleteALLFiles() {
        File root = new File("/data/data/com.crossbow.app.x_timer/files");
        File[] files = root.listFiles();

        if (files == null || files.length == 0) return false;

        for (File file : files) {
            file.delete();
        }

        return true;
    }

    // 列出files下所有文件，debug用
    public boolean showALLFiles() {
        File root = new File("/data/data/com.crossbow.app.x_timer/files");
        File[] files = root.listFiles();

        if (files == null || files.length == 0) return false;

        for (File file : files) {
            System.out.println(file.getName());
        }

        return true;
    }

    // 删除所监听列表
    public boolean deleteAppList() {
        File file = new File("/data/data/com.crossbow.app.x_timer/files/appList");
        if (!file.exists()) {
            return false;
        }

        file.delete();
        return true;
    }

    // get app real name
    private String findAppName(String pkgName) {
        if (packages == null) packages = mContext.getPackageManager().getInstalledPackages(0);

        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            String packageName = packageInfo.packageName;
            if (packageName.equals(pkgName)) {
                String appName = packageInfo.applicationInfo
                        .loadLabel(mContext.getPackageManager()).toString();

                return appName;
            }
        }
        return "找不到名字";
    }
}
