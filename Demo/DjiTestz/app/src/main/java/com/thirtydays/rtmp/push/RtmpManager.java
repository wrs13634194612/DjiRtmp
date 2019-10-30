package com.thirtydays.rtmp.push;

/**
 * Created by chenxiaojin on 2018/3/30.
 * Rtmp管理类， 提供rtmp推流功能
 */

public class RtmpManager {
    static {
//        System.loadLibrary("ssl");
//        System.loadLibrary("crypto");
        System.loadLibrary("RtmpExt");
    }

    /**
     * 完成初始化及连接服务器操作
     *
     * @param rtmpUrl 输入rtmp服务器地址
     * @return 成功返回1，失败返回0
     */
    public native int rtmpConnect(String rtmpUrl);

    /**
     * 完成内存释放及关闭连接操作
     */
    public native void rtmpClose();

    /**
     * 判断当前连接是否正常
     *
     * @return 连接返回true，断开返回false
     */
    public native int isRtmpConnected();

    /**
     * 完成将H264实时视频流封装成rtmp格式发送
     * 数据为H264裸流数据, 非完整nal数据
     *
     * @return 成功返回1，失败返回0
     */
    public native int rtmpSend(byte[] videoData, int dataLen);

//    // TODO 删除
//    public native int rtmpSendError(byte[] videoData, int dataLen);
}
