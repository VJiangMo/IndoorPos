package upsoft.ble.iBeaconPos;

import java.util.ArrayList;
import java.util.HashMap;

import upsoft.ble.util.BleUtil;
import upsoft.ble.util.DataStore;
import upsoft.ble.util.ScannedDevice;
import upsoft.ble.widget.CustomDialog;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback {

    private Context mContext;
    private BluetoothAdapter mBTAdapter;
    private DeviceAdapter mDeviceAdapter;
    private boolean mIsScanning;
    private CustomDialog mCustomDialog;
    private DataStore mDataStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan);

        init();
        startScan();//auto scan when inited.
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ((mBTAdapter != null) && (!mBTAdapter.isEnabled())) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
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

    private void init() {
        mContext=this;
        mDataStore=new DataStore(mContext);

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
                new ArrayList<ScannedDevice>(),mDataStore);
        deviceListView.setAdapter(mDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name=mDeviceAdapter.getDeviceName(position);
                dialog(name);
            }
        });
        stopScan();
    }

    // 弹窗
    private void dialog(final String name) {
        mCustomDialog = new CustomDialog(mContext);
        final EditText editText = (EditText) mCustomDialog.getEditText();//方法在CustomDialog中实现
        mCustomDialog.setOnPositiveListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //dosomething
                String deviceAliasName=editText.getText().toString();
                HashMap<String,String> dataMap=new HashMap<String, String>();
                if(!deviceAliasName.isEmpty()){
                    dataMap.put(name,deviceAliasName);
                }
                mDataStore.writeData(dataMap);
                mCustomDialog.dismiss();
            }
        });

        mCustomDialog.setOnNegativeListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCustomDialog.dismiss();
            }
        });
        mCustomDialog.show();
    }

    private void startScan() {
        if ((mBTAdapter != null) && (!mIsScanning)) {
            mBTAdapter.startLeScan(this);
            mIsScanning = true;
            setProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }
    }

    private void stopScan() {
        if (mBTAdapter != null) {
            mBTAdapter.stopLeScan(this);
        }
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }

}
