package com.example.administrator.testz;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by wrs on 2019/9/10,11:19
 * projectName: DjiTestz
 * packageName: com.example.administrator.testz
 */
public class TestNettyActivity extends Activity implements MessageListener, NettyListener, View.OnClickListener {



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nettytest);

        //netty连接服务端
        NettyClient.getInstance().setNettyListener(this);
        NettyClient.getInstance().setMessageListener(this);
        if (!NettyClient.getInstance().isConnected()) {
            NettyClient.getInstance().connect();
        }

    }

    @Override
    public void onClick(View v) {

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
