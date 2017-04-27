package upsoft.ble.indoorPos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import upsoft.ble.util.DataStore;
import upsoft.ble.util.NetHttpUtil;
import upsoft.ble.widget.LoadingDialog;

import static upsoft.ble.util.NetHttpUtil.isNetworkAvailable;

/**
 * Created by BASTA on 2017/4/24.
 */

public class LoginActivity extends Activity {
    private String mTag=this.getClass().toString();
    private Context mContext;
    private ViewGroup mViewGroup;
    private HttpHandler mHttpHdl;
    private NetHttpUtil mNetHttpUtil;
    private ClickListener mClickListener;
    private DataStore mDataStore;
    private LoadingDialog mLoadingDialog;

    // 定义一个变量，来标识是否退出
    private static boolean isExit = false;
    Handler mExitHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            // 利用handler延迟发送更改状态信息
            mExitHandler.sendEmptyMessageDelayed(0, 1000);
        } else {
            finish();
            System.exit(0);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater=LayoutInflater.from(mContext=this);
        View layoutView=inflater.inflate(R.layout.activity_login,null);
        setContentView(layoutView);

        init(layoutView);
    }

    private void init(View layoutView){
        mViewGroup=new ViewGroup();
        mViewGroup.mUsrET=(EditText)layoutView.findViewById(R.id.accountEt);
        mViewGroup.mPwdET=(EditText)layoutView.findViewById(R.id.pwdEt);
        mViewGroup.mLoginBtn=(Button)layoutView.findViewById(R.id.subBtn);
        mClickListener=new ClickListener();
        mViewGroup.mLoginBtn.setOnClickListener(mClickListener);
        mDataStore=DataStore.singleton(mContext);
        mLoadingDialog=new LoadingDialog(this,R.style.loading_dialog);
        //判断网络是否可用
        if(!isNetworkAvailable(mContext)){
            Toast.makeText(mContext,"当前网络不可用！",Toast.LENGTH_SHORT).show();
        }

        mHttpHdl=new HttpHandler();
        mNetHttpUtil=new NetHttpUtil(mContext,mHttpHdl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class ViewGroup{
        public EditText mUsrET;
        public EditText mPwdET;
        public Button mLoginBtn;
    }

    private class ClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            int vId=v.getId();
            switch (vId){
                case R.id.subBtn:{
                    login();//执行登录
                    break;
                }
            }
        }
    }

    private class HttpHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int msgId=msg.what;
            switch (msgId){
                case 0x0001:{
                    Bundle jsonBundle=(Bundle)msg.obj;
                    try {
                        JSONObject jobj=new JSONObject(jsonBundle.getString("resJson"));
                        if(jobj.has("result")){
                            String loginResult=jobj.getString("result");
                            if(loginResult.equals("1")){//login success
                                HashMap<String,String> dataMap=new HashMap<String,String>();
                                dataMap.put("usr",mViewGroup.mUsrET.getText().toString().trim());
                                dataMap.put("pwd",mViewGroup.mPwdET.getText().toString().trim());
                                mDataStore.writeData(dataMap);

                                Intent jumpIntent=new Intent(mContext,ScanActivity.class);
                                mContext.startActivity(jumpIntent);
                            }else {//login fail
                                Toast.makeText(mContext,"登录失败！",Toast.LENGTH_SHORT).show();
                            }
                            mLoadingDialog.dismiss();
                        }else {
                            Log.e(mTag,"login API has no result attr.");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case 0x0002:{
                    Toast.makeText(mContext,"登录异常！",Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    private String getResStr(int resId){
        return mContext.getResources().getString(resId);
    }

    //登录操作
    private void login(){
        mLoadingDialog.show();
        String usr=mViewGroup.mUsrET.getText().toString().trim();
        String pwd=mViewGroup.mPwdET.getText().toString().trim();
        if(usr.equals("")||pwd.equals("")){
            Toast.makeText(mContext,"请先输入账户、密码。",Toast.LENGTH_SHORT).show();
            return;
        }
        final JSONObject params=new JSONObject();
        try {
            params.put("username",usr);
            params.put("password",pwd);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String urlStr=getResStr(R.string.url)+"login";
                        String resJson=mNetHttpUtil.getHttpPostData(urlStr,params);
                        if(!resJson.equals("")){
                            Bundle dataBundle=new Bundle();
                            dataBundle.putString("resJson",resJson);
                            Message msg=new Message();
                            msg.what=0x0001;
                            msg.obj=dataBundle;
                            mHttpHdl.sendMessage(msg);
                        }else {
                            mHttpHdl.sendEmptyMessage(0x0002);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
