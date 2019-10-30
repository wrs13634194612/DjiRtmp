package com.example.administrator.testz;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.thirtydays.rtmp.push.RtmpManager;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {
    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    protected DJICodecManager mCodecManager = null;
    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn,btn_battery;
    private ToggleButton mRecordBtn;
    private TextView recordingTime;
    private ImageView iv_connected;
    private Handler handler;
    //otto同样需要注册和取消注册、订阅事件
    private boolean isConnect;

    private RtmpManager mRtmpManager;
    private int rnum = -1;

    private Switch sw_main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        initUI();

        mRtmpManager = new RtmpManager();
        //   startrtmpConnect();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(2000);
                        if (mRtmpManager != null) {
                            rnum = mRtmpManager.isRtmpConnected();
                        }
                        String datah264 = "rtmp_connect:" + rnum;
                        NettyClient.getInstance().sendMsgToServer(datah264);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void startrtmpConnect() {
        mRtmpManager.rtmpConnect("rtmp://10.0.2.209:1935/live/test");
    }

    public void sendrtmpConnect(byte[] data, int dataLen) {
        if (mRtmpManager.isRtmpConnected() == 1) {
            mRtmpManager.rtmpSend(data, dataLen);
        }
    }

    public void stoprtmpConnect() {
        if (mRtmpManager.isRtmpConnected() == 1) {
            mRtmpManager.rtmpClose();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        //用于接收摄像机实时视图的原始H264视频数据的回调
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {

                if (mCodecManager != null) {

                    sendrtmpConnect(videoBuffer,size);

                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(@NonNull SystemState cameraSystemState) {
                    if (null != cameraSystemState) {
                        final int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;
                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideorecording = cameraSystemState.isRecording();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recordingTime.setText(timeString);
                                if (isVideorecording) {
                                    recordingTime.setVisibility(View.VISIBLE);
                                } else {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });
        }






    }




    protected void onProductChange() {
        initPreviewer();
        loginAccount();
    }

    private void loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this, new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
            @Override
            public void onSuccess(UserAccountState userAccountState) {
                Log.e(TAG, "Login success");
            }

            @Override
            public void onFailure(DJIError djiError) {
                showToast("Login Error" + djiError.getDescription());
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        initPreviewer();
        onProductChange();
        if (mVideoSurface == null) {
            Log.e(TAG, "mVideosurface is nnull");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        uninitPreviewer();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onReturn(View view) {
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uninitPreviewer();
    }

    private void initUI() {
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        iv_connected = (ImageView) findViewById(R.id.iv_connected);
        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        btn_battery = (Button) findViewById(R.id.btn_battery);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        sw_main = (Switch) findViewById(R.id.sw_main);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);
        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });

        btn_battery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,BattryActivity.class);
                startActivity(intent);
            }
        });


        sw_main.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!buttonView.isPressed()){
                    return;
                }
                if (isChecked){
                    startrtmpConnect();
                }else{
                    stoprtmpConnect();
                }

            }
        });

    }

    private void initPreviewer() {
        BaseProduct product = FPVDemoApplication.getProductInstance();
        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_capture: {
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode: {
                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode: {
                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            default:
                break;
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode) {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    private void captureAction() {
        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE;
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //延时2s执行
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null) {
                                            showToast("take photo :success");
                                        } else {
                                            showToast(djiError.getDescription());
                                        }
                                    }
                                });
                            }
                        }, 2000);
                    }
                }
            });
        }
    }

    private void startRecord() {
        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        showToast("Record video success");
                    } else {
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    private void stopRecord() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        showToast("Stop recording success");
                    } else {
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
    }


}
