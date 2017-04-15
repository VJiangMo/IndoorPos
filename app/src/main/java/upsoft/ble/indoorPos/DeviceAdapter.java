package upsoft.ble.indoorPos;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import upsoft.ble.util.DataStore;
import upsoft.ble.util.DateUtil;
import upsoft.ble.util.OfflineSpeechUtil;
import upsoft.ble.util.ScannedDevice;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class DeviceAdapter extends ArrayAdapter<ScannedDevice> {
    private static final String PREFIX_RSSI = "信号强度：";
    private static final String PREFIX_LASTUPDATED = "上次刷新时间：";
    private List<ScannedDevice> mList;
    private LayoutInflater mInflater;
    private int mResId;
    private Context mContext;
    private DataStore mDataStore;
    private OfflineSpeechUtil mSpeechUtil;
    private LocationThread mLocationThread;//定位线程
    private Handler mLocationHandler;

    private class LocationThread extends Thread{
        private boolean mRunFlag=true;
        private double mMinDistance=0.0f;//0米
        private String mMinDistanceAlias="";

        public LocationThread(){
            super();
        }
        public void stopLocationThread(){
            mRunFlag=false;
        }
        @Override
        public void run() {
            while(mRunFlag){
                mMinDistance=0.0f;
                mMinDistanceAlias=mContext.getResources().getString(R.string.unkown_location_str);

                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //计算出距离最小的定位点
                if (mList != null) {
                    for (ScannedDevice device : mList) {
                        if (device.getIBeacon() != null) {
                            double deviceDistance=device.getDistance();
                            String deviceName=device.getDisplayName();
                            Log.d("++++++Distance","deviceDistance:"+deviceDistance+",\n"+
                                    "deviceName:"+deviceName+"\n");
                            if(mMinDistance<0.00001f){
                                mMinDistance=deviceDistance;
                                mMinDistanceAlias=mDataStore.readData(deviceName);
                            }
                            if(mMinDistance>deviceDistance){
                                mMinDistance=deviceDistance;
                                mMinDistanceAlias=mDataStore.readData(deviceName);
                            }
                        }
                    }
                    Log.d("++","-------------------");
                    Log.d("++++++minDistance","mMinDistance:"+mMinDistance+",\n"+
                            "mMinDistanceAlias:"+mMinDistanceAlias+"\r\n");
                }
                //判断是否播报
                if(mMinDistance<1.0f){
                    //刷新界面
                    Message msg=new Message();
                    msg.what=0x0101;
                    msg.obj=mMinDistanceAlias;
                    mLocationHandler.sendMessage(msg);
                    //语音播报
                    String speechContent=mContext.getResources().getString(R.string.now_location_str)+mMinDistanceAlias;
                    mSpeechUtil.play(speechContent);
                }
            }
        }
    }

    public DeviceAdapter(Context context, int resId, List<ScannedDevice> objects,
                         OfflineSpeechUtil speech, DataStore dataStore,Handler handler) {
        super(context, resId, objects);
        mContext=context;
        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSpeechUtil=speech;
        mDataStore=dataStore;
        mLocationHandler=handler;
        mLocationThread=new LocationThread();
        mLocationThread.start();
    }

    String getDeviceName(int position){
        String name=mList.get(position).getDisplayName();
        return name;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScannedDevice item = (ScannedDevice) getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);
        }

        TextView aliasName=(TextView)convertView.findViewById(R.id.device_alias_name);
        String aliasStr= mDataStore.readData(getDeviceName(position));
        aliasName.setText(aliasStr);

        TextView name = (TextView) convertView.findViewById(R.id.device_name);
        name.setText(item.getDisplayName());

        TextView address = (TextView) convertView.findViewById(R.id.device_address);
        address.setText(item.getDevice().getAddress());

        TextView rssi = (TextView) convertView.findViewById(R.id.device_rssi);
        rssi.setText(PREFIX_RSSI + Integer.toString(item.getRssi()));

        TextView lastupdated = (TextView) convertView.findViewById(R.id.device_lastupdated);
        lastupdated.setText(PREFIX_LASTUPDATED + DateUtil.get_yyyyMMddHHmmssSSS(item.getLastUpdatedMs()));

        TextView distance=(TextView) convertView.findViewById(R.id.distance_tv);
        DecimalFormat df=new DecimalFormat("0.00000");
        distance.setText(df.format(item.getDistance()));

        TextView ibeaconInfo = (TextView) convertView.findViewById(R.id.device_ibeacon_info);
        Resources res = convertView.getContext().getResources();
        if (item.getIBeacon() != null) {
            ibeaconInfo.setText(res.getString(R.string.label_ibeacon) + "\n"
                    + item.getIBeacon().toString());
            ibeaconInfo.setTextColor(mContext.getResources().getColor(R.color.holo_blue_dark));
        } else {
            ibeaconInfo.setText(res.getString(R.string.label_not_ibeacon));
            ibeaconInfo.setTextColor(mContext.getResources().getColor(R.color.red));
        }

        TextView scanRecord = (TextView) convertView.findViewById(R.id.device_scanrecord);
        scanRecord.setText(item.getScanRecordHexString());

        return convertView;
    }

    /**
     * add or update BluetoothDevice List
     * 
     * @param newDevice Scanned Bluetooth Device
     * @param rssi RSSI
     * @param scanRecord advertise data
     * @return summary ex. "iBeacon:3 (Total:10)"
     */
    public String update(BluetoothDevice newDevice, int rssi, byte[] scanRecord) {
        if ((newDevice == null) || (newDevice.getAddress() == null)) {
            return "";
        }
        long now = System.currentTimeMillis();

        boolean contains = false;
        for (ScannedDevice device : mList) {
            if (newDevice.getAddress().equals(device.getDevice().getAddress())) {
                contains = true;
                // update
                device.setRssi(rssi);
                device.setLastUpdatedMs(now);
                device.setScanRecord(scanRecord);
                break;
            }
        }
        if (!contains) {
            // add new BluetoothDevice
            mList.add(new ScannedDevice(newDevice, rssi, scanRecord, now));
        }

        // sort by RSSI
        Collections.sort(mList, new Comparator<ScannedDevice>() {
            @Override
            public int compare(ScannedDevice lhs, ScannedDevice rhs) {
                if (lhs.getRssi() == 0) {
                    return 1;
                } else if (rhs.getRssi() == 0) {
                    return -1;
                }
                if (lhs.getRssi() > rhs.getRssi()) {
                    return -1;
                } else if (lhs.getRssi() < rhs.getRssi()) {
                    return 1;
                }
                return 0;
            }
        });

        notifyDataSetChanged();

        // create summary
        int totalCount = 0;
        int iBeaconCount = 0;
        if (mList != null) {
            totalCount = mList.size();
            for (ScannedDevice device : mList) {
                if (device.getIBeacon() != null) {
                    iBeaconCount++;
                }
            }
        }
        String summary = "定位设备:" + Integer.toString(iBeaconCount) + " (总计:"
                + Integer.toString(totalCount) + ")";
        return summary;
    }
    
    public List<ScannedDevice> getList() {
        return mList;
    }
}
