package com.example.administrator.testz;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by wrs on 2019/6/4,10:46
 * projectName: Testz
 * packageName: com.example.administrator.testz
 */

public class ConnectionActivity extends Activity implements View.OnClickListener, MessageListener, NettyListener {
    private static final String TAG = ConnectionActivity.class.getName();

    private TextView mTextConnectionStatus;
    private TextView mTextProduct;
    private TextView mVersionTv;
    private Button mBtnOpen;
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        setContentView(R.layout.activity_connection);
        initUI();


        //netty连接服务端
        NettyClient.getInstance().setNettyListener(this);
        NettyClient.getInstance().setMessageListener(this);
        if (!NettyClient.getInstance().isConnected()) {
            System.out.println("连接成功");
            NettyClient.getInstance().connect();
        }



        //广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }


    //动态权限申请
    private void checkAndRequestPermissions() {
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i > 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        //权限有了  开始发送广播
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!");
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    showToast("registering,pls wait...");
                    DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                                DJISDKManager.getInstance().startConnectionToProduct();
                                showToast("Register Success");
                            } else {
                                showToast("Register sdk fails,check network is available");
                            }
                            Log.e(TAG, djiError.getDescription());
                        }

                        @Override
                        public void onProductDisconnect() {
                            Log.e(TAG, "onProductDisconnect");
                            showToast("Produce DisConnected");
                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            Log.e(TAG, String.format("onProductConnect newProduce:%s", baseProduct));
                            showToast("Produce Connected");
                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {
                                    @Override
                                    public void onConnectivityChange(boolean isConnected) {
                                        Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                    }
                                });
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));
                        }
                    });
                }
            });
        }

    }

    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "" + toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initUI() {
        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        mTextProduct = (TextView) findViewById(R.id.text_product_info);
        mBtnOpen = (Button) findViewById(R.id.btn_open);
        mBtnOpen.setOnClickListener(this);
        mBtnOpen.setEnabled(false);
        mVersionTv = (TextView) findViewById(R.id.textView2);
        mVersionTv.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_open:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = FPVDemoApplication.getProductInstance();
        if (null != mProduct && mProduct.isConnected()) {
            Log.e(TAG, "refreshDSK:true");
            mBtnOpen.setEnabled(true);
            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandleHald";
            mTextConnectionStatus.setText("Status:" + str + " connected");
            if (null != mProduct.getModel()) {
                mTextProduct.setText("" + mProduct.getModel().getDisplayName());
            } else {
                mTextProduct.setText(R.string.product_information);
            }
        } else {
            Log.v(TAG, "refreshSDK: False");
            mBtnOpen.setEnabled(false);

            mTextProduct.setText(R.string.product_information);
            mTextConnectionStatus.setText(R.string.connection_loose);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onConnected() {
        NettyClient.getInstance().sendHeartBeatData("连接成功");
    }

    @Override
    public void onDisConnect() {
        NettyClient.getInstance().sendHeartBeatData("连接失败");
    }
}
