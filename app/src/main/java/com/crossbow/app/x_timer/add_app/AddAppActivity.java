package com.crossbow.app.x_timer.add_app;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crossbow.app.x_timer.MainActivity;
import com.crossbow.app.x_timer.R;
import com.crossbow.app.x_timer.service.AppUsage;
import com.crossbow.app.x_timer.service.TickTrackerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by wanglx on 2016/1/1.
 */
public class AddAppActivity extends AppCompatActivity {

    private final String TAG = "AddAppActivity";

    private List<AppInfo> appList;
    private AppInfoAdapter appInfoAdapter;
    private ArrayList<String> selected;
    private TextView showInfo;
    private ListView listView;

    // 本地存储
    private SharedPreferences pref;

    // 联系service
    private TickTrackerService.UsageBinder usageBinder;
    private ServiceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_app_main);

        initConnection();
        initToolbar();
        initStatusBar();
        initCustomAppList();

        handleButton();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");

        super.onResume();

        if (isWorking()) {
            bindTickService();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");

        super.onPause();

        if (isWorking()) unbindTickService();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        super.onBackPressed();
        finish();
    }

    // handle toolbar
    private void initToolbar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.add_app_toolbar);
        setSupportActionBar(toolbar);

        // back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // handle the status bar
    private void initStatusBar() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
    }

    // list all custom app info
    public void initCustomAppList() {
        appList = new ArrayList<>();
        List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);

        // 所有应用信息
        for (int i = 0;i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            // 不要系统应用
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;

            String appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            String packageName = packageInfo.packageName;
            Drawable appIcon = packageInfo.applicationInfo.loadIcon(getPackageManager());
            AppInfo tmpInfo =new AppInfo(packageName, appName, appIcon);
            appList.add(tmpInfo);
        }
    }

    // handle the adapter
    private void initAdapter() {
        appInfoAdapter = new AppInfoAdapter(this, R.layout.add_app_item, appList);
        // 已选
        selected = (ArrayList<String>)getSelectedApps().clone();
        appInfoAdapter.setDefault(selected);

        showInfo = (TextView) findViewById(R.id.add_app_text);
        showInfo.setText("已选" + selected.size() + "个应用");

        listView = (ListView)findViewById(R.id.app_list);
        listView.setAdapter(appInfoAdapter);
    }

    // init the connection
    private void initConnection() {
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "onBinded: ");

                usageBinder = (TickTrackerService.UsageBinder) service;

                // 异步绑定
                initAdapter();
                handleListView();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "service error");

                usageBinder = null;
            }
        };
    }

    // add listener to list view
    private void handleListView() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox check = (CheckBox) view.findViewById(R.id.app_check_box);
                RelativeLayout background = (RelativeLayout) view.findViewById(R.id.app_background);
                if (check.isChecked()) {
                    check.setChecked(false);
                    selected.remove(appList.get(position).getPackageName());
                    showInfo.setText("已选" + selected.size() + "个应用");
                    background.setBackgroundColor(ContextCompat.getColor(AddAppActivity.this, R.color.listItemUnselected));
                } else {
                    check.setChecked(true);
                    selected.add(appList.get(position).getPackageName());
                    showInfo.setText("已选" + selected.size() + "个应用");
                    background.setBackgroundColor(ContextCompat.getColor(AddAppActivity.this, R.color.listItemSelected));
                }
            }
        });
    }

    // add listener to button
    private void handleButton() {
        // 确定 按钮
        Button ok = (Button)findViewById(R.id.add_app_ok);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isWorking() || usageBinder == null) {
                    Toast.makeText(AddAppActivity.this, "失败，请先开启服务",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                for (String appName: selected) {
                    if (!usageBinder.isInWatchingList(appName)) {
                        usageBinder.addAppToWatchingList(appName, shouldShowNotification());
                    }
                }

                for (String name: getSelectedApps()) {
                    if (!selected.contains(name)) {
                        usageBinder.removeAppFromWatchingLise(name, shouldShowNotification());
                    }
                }

                Toast.makeText(AddAppActivity.this, "成功", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // 取消 按钮
        Button cancel = (Button)findViewById(R.id.add_app_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // bind the service
    public void bindTickService() {
        Log.d(TAG, "onBinding:");

        Intent intent = new Intent(this, TickTrackerService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    // unbind the service
    public void unbindTickService() {
        unbindService(connection);

        Log.d(TAG, "onUnbinded:");
    }

    // check if the service is working
    public boolean isWorking() {
        ActivityManager myAM = (ActivityManager)getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(100);

        if (myList.size() <= 0) {
            return false;
        }

        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals("com.crossbow.app.x_timer.service.TickTrackerService")) {
                return true;
            }
        }
        return false;
    }

    // find all apps in the watching list
    private ArrayList<String> getSelectedApps() {
        Map<String, AppUsage> watchingList = usageBinder.getWatchingList();
        ArrayList<String> list = new ArrayList<>();
        for (Map.Entry<String, AppUsage> app : watchingList.entrySet()) {
            list.add(app.getKey());
        }

        return list;
    }

    // check if should start a notification
    public boolean shouldShowNotification() {
        if (pref == null) pref = getSharedPreferences("settings", Context.MODE_PRIVATE);

        return pref.getBoolean("show", true);
    }
}