package com.crossbow.app.x_timer.fragment.statistic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.crossbow.app.x_timer.MainActivity;
import com.crossbow.app.x_timer.R;
import com.crossbow.app.x_timer.detail.app_detail.AppDetailActivity;
import com.crossbow.app.x_timer.service.AppUsage;
import com.crossbow.app.x_timer.service.TickTrackerService;
import com.crossbow.app.x_timer.utils.FileUtils;
import com.devspark.progressfragment.ProgressFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by CuiH on 2015/12/29.
 */
public class StatisticFragment extends ProgressFragment {

    private final String TAG = "StatisticFragment";

    private MainActivity mainActivity;

    // 异步用
    private View realView;
    private boolean firstTime;
    private boolean isVisible;
    private boolean forceRefresh;

    private List<AppUsage> appUsageList;


    public StatisticFragment() { }

    @SuppressLint("ValidFragment")
    public StatisticFragment(MainActivity activity) {
        mainActivity = activity;
        firstTime = true;
        forceRefresh = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView: ");

        realView = inflater.inflate(R.layout.statistic_fragment, container, false);

        return inflater.inflate(R.layout.fragment_loading, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated: ");

        super.onActivityCreated(savedInstanceState);
        setContentView(realView);
        setContentShown(false);

        if (isVisible) {
            if (firstTime) {
                new MyAsyncTask().execute();

                firstTime = false;
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        Log.d(TAG, "setUserVisibleHint: ");

        if (isVisibleToUser) {
            isVisible = true;

            if (forceRefresh) {
                new MyAsyncTask().execute();

                forceRefresh = false;
            }
        } else {
            isVisible = false;
        }

        super.setUserVisibleHint(isVisibleToUser);
    }

    // 异步更新UI
    private class MyAsyncTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            initDayLengthUsed();
            getStoredAppInfo();
            initMostCountUsedApp();
            initMostDayUsedApp();
            initButton();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            setContentShown(true);
        }
    }

    public void setFirstTime(boolean flag) {
        firstTime = flag;
    }

    public void setForceRefresh(boolean flag) {
        forceRefresh = flag;
    }

    // 计算共使用了X-Timer多少天
    private void initDayLengthUsed() {
        TextView textView = (TextView)realView.findViewById(R.id.statistic_day_length_all);

        SharedPreferences pref = mainActivity
                .getSharedPreferences("settings", Context.MODE_PRIVATE);

        Date today = new Date();

        String startString = pref.getString("firstDate", AppUsage.getDateInString(today));
        Date startDate = new Date();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            startDate = sdf.parse(startString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        int day1 = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTime(today);
        int day2 = calendar.get(Calendar.DAY_OF_YEAR);

        textView.setText(""+(day2-day1));
    }

    // 获取存储的所有app信息
    private void getStoredAppInfo() {
        FileUtils fileUtils = new FileUtils(mainActivity);
        appUsageList = fileUtils.getAllStoredApp();
    }

    // 查找使用次数最多的应用
    private void initMostCountUsedApp() {
        TextView name = (TextView)realView.findViewById(R.id.statistic_most_count_app);
        TextView count = (TextView)realView.findViewById(R.id.statistic_most_count_app_count);

        if (appUsageList.isEmpty()) {
            name.setText("暂无数据");
            count.setText("暂无数据");

            return;
        }

        AppUsage mostUsed = null;
        long mostUsedCount = -1;
        for (AppUsage appUsage: appUsageList) {
            long nowCount = 0;
            for (Map.Entry<String, AppUsage.History> historyEntry:
                    appUsage.getUsingHistory().entrySet()) {
                nowCount += historyEntry.getValue().getUsedCount();
            }

            if (nowCount > mostUsedCount) {
                mostUsedCount = nowCount;
                mostUsed = appUsage;
            }
        }

        if (mostUsed != null) {
            name.setText(mostUsed.getRealName());
            count.setText(""+mostUsedCount);
        } else {
            name.setText("暂无数据");
            count.setText("暂无数据");
        }
    }

    private void initMostDayUsedApp() {
        TextView name = (TextView)realView.findViewById(R.id.statistic_most_day_app);
        TextView days = (TextView)realView.findViewById(R.id.statistic_most_day_app_count);

        if (appUsageList.isEmpty()) {
            name.setText("暂无数据");
            days.setText("暂无数据");

            return;
        }

        AppUsage mostUsed = null;
        long mostUsedCount = -1;
        for (AppUsage appUsage: appUsageList) {
            long nowCount = appUsage.getUsingHistory().entrySet().size();

            if (nowCount > mostUsedCount) {
                mostUsedCount = nowCount;
                mostUsed = appUsage;
            }
        }

        if (mostUsed != null) {
            name.setText(mostUsed.getRealName());
            days.setText(""+mostUsedCount);
        } else {
            name.setText("暂无数据");
            days.setText("暂无数据");
        }
    }

    private void initButton() {
        Button b = (Button)realView.findViewById(R.id.testB);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "im here!!");
                if (mainActivity.isWorking()) {
                    Log.d(TAG, "im here2312312!!");
                    TickTrackerService.UsageBinder binder = mainActivity.getBinder();
                    if (binder.setLimitToApp("com.tencent.mm", 10000)) {
                        Toast.makeText(mainActivity, "s", Toast.LENGTH_SHORT);
                    } else {
                        Toast.makeText(mainActivity, "f", Toast.LENGTH_SHORT);
                    }
                }
            }
        });
    }



}
