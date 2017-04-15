package upsoft.ble.util;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import cn.yunzhisheng.tts.offline.TTSPlayerListener;
import cn.yunzhisheng.tts.offline.basic.ITTSControl;
import cn.yunzhisheng.tts.offline.basic.TTSFactory;

import static java.lang.Thread.sleep;

/**
 * Created by yangzhou on 2017/4/15.
 */

public class OfflineSpeechUtil implements TTSPlayerListener {
    public static final String appKey = "_appKey_";
    public static final String  secret = "_secret_";
    private ITTSControl mTTSPlayer;
    private Context mContext;
    private static OfflineSpeechUtil mOfflineSpeechUtil;

    public static OfflineSpeechUtil singleton(Context ctx){
        if(null==mOfflineSpeechUtil){
            mOfflineSpeechUtil=new OfflineSpeechUtil(ctx);
        }
        return mOfflineSpeechUtil;
    }

    private OfflineSpeechUtil(Context ctx) {
        this.mContext = ctx;
        init();
    }

    /**
     * 初始化引擎
     * @author JPH
     * @date 2015-4-14 下午7:32:58
     */
    private void init() {
        mTTSPlayer = TTSFactory.createTTSControl(mContext, appKey);// 初始化语音合成对象
        mTTSPlayer.setTTSListener(this);// 设置回调监听
        mTTSPlayer.setStreamType(AudioManager.STREAM_MUSIC);//设置音频流
        mTTSPlayer.setVoiceSpeed(2.5f);//设置播报语速,播报语速，数值范围 0.1~2.5 默认为 1.0
        mTTSPlayer.setVoicePitch(1.1f);//设置播报音高,调节音高，数值范围 0.9～1.1 默认为 1.0
        mTTSPlayer.init();// 初始化合成引擎
    }
    /**
     * 停止播放
     * @author JPH
     * @date 2015-4-14 下午7:50:35
     */
    public void stop(){
        mTTSPlayer.stop();
    }

    /**
     * 播放
     *
     * @author JPH
     * @date 2015-4-14 下午7:29:24
     */
    public void play(final String content) {
        mTTSPlayer.play(content);
    }

    public void play(int resId){
        String audioContent= mContext.getResources().getString(resId);
        play(audioContent);
    }

    /**
     * 释放资源
     *
     * @author JPH
     * @date 2015-4-14 下午7:27:56
     */
    public void release() {
        // 主动释放离线引擎
        mTTSPlayer.release();
    }

    @Override
    public void onPlayEnd() {
        // 播放完成回调
        Log.i("msg", "onPlayEnd");
    }

    @Override
    public void onPlayBegin() {
        // 开始播放回调
        Log.i("msg", "onPlayBegin");
    }

    @Override
    public void onInitFinish() {
        // 初始化成功回调
        Log.i("msg", "onInitFinish");
    }

    @Override
    public void onError(cn.yunzhisheng.tts.offline.common.USCError arg0) {
        // 语音合成错误回调
        Log.i("msg", "onError");
    }

    @Override
    public void onCancel() {
        // 取消播放回调
        Log.i("msg", "onCancel");
    }

    @Override
    public void onBuffer() {
        // 开始缓冲回调
        Log.i("msg", "onBuffer");
    }
}
