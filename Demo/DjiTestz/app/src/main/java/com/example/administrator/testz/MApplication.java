package com.example.administrator.testz;

import android.app.Application;
import android.content.Context;

import com.example.administrator.crashLog.CrashHandler;
import com.secneo.sdk.Helper;

/**
 * Created by wrs on 2019/6/4,10:47
 * projectName: Testz
 * packageName: com.example.administrator.testz
 */
public class MApplication extends Application {
    private FPVDemoApplication fpvDemoApplication;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Helper.install(MApplication.this);
        if (fpvDemoApplication == null) {
            fpvDemoApplication = new FPVDemoApplication();
            fpvDemoApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fpvDemoApplication.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
    }
}
