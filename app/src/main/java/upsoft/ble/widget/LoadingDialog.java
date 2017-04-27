package upsoft.ble.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import upsoft.ble.indoorPos.R;

/**
 * Created by BASTA on 2017/4/27.
 */

public class LoadingDialog extends Dialog{
    private View mView;
    private Animation mAnimation;
    private ImageView mLoadingImage;
    private LinearLayout mLayOut;
    private boolean mIsVisible=false;

    public LoadingDialog(Context context, int resId){
        super(context,resId);
        LayoutInflater inflater = LayoutInflater.from(context);
        mView =inflater.inflate(R.layout.loading_dialog, null);
        mAnimation= AnimationUtils.loadAnimation(
                context, R.anim.loading_animation);
        mLoadingImage = (ImageView) mView.findViewById(R.id.img);
        mLayOut=(LinearLayout) mView.findViewById(R.id.dialog_view);// 加载布局
        setContentView(mLayOut, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));// 设置布局
        this.setCancelable(false);
    }

    public synchronized boolean isVisible(){
        return mIsVisible;
    }

    @Override
    public synchronized void show() {
        if(mIsVisible){
            return;
        }
        super.show();
        mLoadingImage.startAnimation(mAnimation);
        mIsVisible=true;
    }

    @Override
    public synchronized void dismiss() {
        if(!mIsVisible){
            return;
        }
        super.dismiss();
        mAnimation.cancel();
        mIsVisible=false;
    }
}
