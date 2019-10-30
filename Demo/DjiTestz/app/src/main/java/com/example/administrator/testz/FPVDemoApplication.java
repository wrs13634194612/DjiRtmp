package com.example.administrator.testz;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;


import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by wrs on 2019/6/4,10:49
 * projectName: Testz
 * packageName: com.example.administrator.testz
 */
public class FPVDemoApplication extends Application {

    public static final String FLAG_CONNECTION_CHANGE = "fpv_tutorial_connection_change";

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback;
    private static BaseProduct mProduct;
    public Handler mHandler;
    private Application instance;

    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public FPVDemoApplication() {

    }

    /**
     * 此函数用于获取DJIBaseProduct的实例。
     * 如果没有连接产品，则返回null。
     */
    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    //电池
    public static synchronized Battery getBatteryInstance(){
        if (getProductInstance() == null){
            return null;
        }
        Battery battery = null;
        BaseProduct productInstance = getProductInstance();
        if (productInstance instanceof Aircraft){
            battery = ((Aircraft)productInstance).getBattery();
            //手持云台
        }else if (productInstance instanceof HandHeld){

        }
        return battery;
    }

    //相机
    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) {
            return null;
        }
        Camera camera = null;
        if (getProductInstance() instanceof Aircraft) {
            camera = ((Aircraft) getProductInstance()).getCamera();
        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }
        return camera;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        /**
         * 启动SDK服务时，将使用接口DJISDKManager.DJISDKManagerCallback的实例进行监听
          SDK注册结果和产品更改。
         */
        mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
            //Listens to the SDK registration result
            @Override
            public void onRegister(DJIError djiError) {
                if (djiError== DJISDKError.REGISTRATION_SUCCESS){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //注册成功
                            Toast.makeText(getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                        }
                    });
                    DJISDKManager.getInstance().startConnectionToProduct();
                }else{
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                Log.e("TAG", djiError.toString());
            }

            @Override
            public void onProductDisconnect() {
                Log.d("TAG", "onProductDisconnect");
                notifyStatusChange();
            }

            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                Log.d("TAG", String.format("onProductConnect newProduct:%s", baseProduct));
                notifyStatusChange();
            }

            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                if (newComponent != null) {
                    newComponent.setComponentListener(new BaseComponent.ComponentListener() {
                        @Override
                        public void onConnectivityChange(boolean isConnected) {
                            Log.d("TAG", "onComponentConnectivityChanged: " + isConnected);
                            notifyStatusChange();
                        }
                    });
                }
                Log.d("TAG",
                        String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                componentKey,
                                oldComponent,
                                newComponent));
            }
        };
        //Check the permissions before registering the application for android system 6.0 above.
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 == 0)) {
            //This is used to start SDK services and initiate SDK.
            DJISDKManager.getInstance().registerApp(getApplicationContext(), mDJISDKManagerCallback);
            Toast.makeText(getApplicationContext(), "registering, pls wait...", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Please check if the permission is granted.", Toast.LENGTH_LONG).show();
        }

    }

    private void notifyStatusChange(){
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable,500);
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            getApplicationContext().sendBroadcast(intent);
        }
    };

}
