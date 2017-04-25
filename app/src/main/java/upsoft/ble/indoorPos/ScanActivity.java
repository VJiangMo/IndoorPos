package upsoft.ble.indoorPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import upsoft.ble.util.BleUtil;
import upsoft.ble.util.DataStore;
import upsoft.ble.util.DateUtil;
import upsoft.ble.util.NetHttpUtil;
import upsoft.ble.util.OfflineSpeechUtil;
import upsoft.ble.util.ScannedDevice;
import upsoft.ble.widget.CustomDialog;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import static upsoft.ble.util.OfflineSpeechUtil.singleton;

public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    private String mTag=this.getClass().toString();
    private Context mContext;
    private BluetoothAdapter mBTAdapter;
    private DeviceAdapter mDeviceAdapter;
    private boolean mIsScanning;
    private CustomDialog mCustomDialog;
    private DataStore mDataStore;
    private OfflineSpeechUtil mSpeechUtil;
    private TextView mLocationTv;
    private LocationHandler mLocationHandler;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO request success
                }
                break;
        }
    }

    private class LocationHandler extends  Handler {
        public LocationHandler(){
            super();
        }
        public LocationHandler(Looper looper){
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0x0101:{
                    String locationStr=(String)msg.obj;
                    showLocation(locationStr);
                    break;
                }
                case 0x0102:{
                    String unkownLocation=mContext.getResources().getString(R.string.unkown_location_str);
                    showLocation(unkownLocation);
                    getActionBar().setSubtitle(null);
                    break;
                }
                case 0x0103:{
                    if(mDeviceAdapter!=null){
                        List<ScannedDevice> deleteList=(List<ScannedDevice>)msg.obj;
                        String locationStr=mLocationTv.getText().toString();
                        for(ScannedDevice device:deleteList){
                            String alias=mDataStore.readAlias(device.getDisplayName());
                            if(alias.equals(locationStr)){
                                String unkownLocation=mContext.getResources().getString(R.string.unkown_location_str);
                                showLocation(unkownLocation);
                                break;
                            }
                        }
                        mDeviceAdapter.removeList(deleteList);
                    }
                    break;
                }
            }
        }
    }

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan);
        mLocationTv=(TextView)findViewById(R.id.location_tv);

        //android 6.0对蓝牙权限的处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        init();
        startScan();//auto scan when inited.
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSpeechUtil=singleton(mContext);
        if ((mBTAdapter != null) && (!mBTAdapter.isEnabled())) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();
        }
        startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSpeechUtil.stop();
        mSpeechUtil.release();
    }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsScanning) {
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_scan).setEnabled(true);
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }
        if ((mBTAdapter == null) || (!mBTAdapter.isEnabled())) {
            menu.findItem(R.id.action_scan).setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            return true;
        } else if (itemId == R.id.action_scan) {
            startScan();
            return true;
        } else if (itemId == R.id.action_stop) {
            stopScan();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onLeScan(final BluetoothDevice newDeivce, final int newRssi,
            final byte[] newScanRecord) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String summary = mDeviceAdapter.update(newDeivce, newRssi, newScanRecord);
                if (summary != null) {
                    getActionBar().setSubtitle(summary);
                }
            }
        });
    }

    private void showLocation(String location){
        mLocationTv.setText(location);
        if(location.equals(mContext.getResources().getString(R.string.unkown_location_str))){
            mLocationTv.setTextColor(mContext.getResources().getColor(R.color.black));
        }else {
            mLocationTv.setTextColor(mContext.getResources().getColor(R.color.holo_green_dark));
        }
    }

    private void init() {
        mContext=this;
        mDataStore=DataStore.singleton(mContext);
        mLocationHandler=new LocationHandler();
        initSpeek();

        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        BluetoothManager manager = BleUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // init listview
        ListView deviceListView = (ListView) findViewById(R.id.list);
        mDeviceAdapter = new DeviceAdapter(this, R.layout.listitem_device,
                new ArrayList<ScannedDevice>(),mSpeechUtil,mDataStore,mLocationHandler);
        deviceListView.setAdapter(mDeviceAdapter);
        stopScan();
    }

    //初始化语音
    void initSpeek(){
        mSpeechUtil=singleton(mContext);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mSpeechUtil.play(R.string.welcome);
            }
        }).start();
    }

    private void startScan() {
        mDeviceAdapter.startLocationThread();
        if ((mBTAdapter != null) && (!mIsScanning)) {
            mBTAdapter.startLeScan(this);
            mIsScanning = true;
            setProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }
    }

    private void stopScan() {
        mDeviceAdapter.stopLocationThread();
        if (mBTAdapter != null) {
            mBTAdapter.stopLeScan(this);
        }
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }
}
