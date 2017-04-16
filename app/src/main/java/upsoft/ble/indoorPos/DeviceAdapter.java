package upsoft.ble.indoorPos;

import java.text.DecimalFormat;
import java.util.ArrayList;
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

    public void startLocationThread(){
        mLocationThread.startRun();
    }

    public void stopLocationThread(){
        mLocationThread.stopRun();
    }

    private class LocationThread extends Thread{
        private boolean mRunFlag=true;
        private double mMinDistance=0.0f;//0米
        private String mMinDistanceAlias="";
        private String mLastSpeakContent="";
        private boolean mIsWait0=false;
        private boolean mIsWait1=false;

        public LocationThread(){
            super();
        }
        public void startRun(){
            mRunFlag=true;
        }
        public void stopRun(){
            mRunFlag=false;
        }
        @Override
        public void run() {
            while(true) {
                mMinDistance = 0.0f;
                mMinDistanceAlias = mContext.getResources().getString(R.string.unkown_location_str);
                List<ScannedDevice> deleteList=new ArrayList<ScannedDevice>();

                try {
                    Thread.sleep(5100);//线程休眠5100ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(mRunFlag) {
                    //计算出距离最小的定位点
                    int ibeaconCount = 0;
                    if (mList != null) {
                        for (ScannedDevice device : mList) {
                            long now = System.currentTimeMillis();
                            if(now-device.getLastUpdatedMs()>1000*10){//说明蓝牙设备已经离线
                                deleteList.add(device);
                            }
                            if (device.getIBeacon() != null) {//当前蓝牙设备为ibeacon设备
                                ibeaconCount++;
                                double deviceDistance = device.getDistance();
                                String deviceName = device.getDisplayName();
                                Log.d("++++++Distance", "deviceDistance:" + deviceDistance + ",\n" +
                                        "deviceName:" + deviceName + "\n");
                                if (mMinDistance < 0.00001f) {
                                    mMinDistance = deviceDistance;
                                    mMinDistanceAlias = mDataStore.readData(deviceName);
                                }
                                if (mMinDistance > deviceDistance) {
                                    mMinDistance = deviceDistance;
                                    mMinDistanceAlias = mDataStore.readData(deviceName);
                                }
                            }
                        }
                        Log.d("++", "-------------------");
                        Log.d("++++++minDistance", "mMinDistance:" + mMinDistance + ",\n" +
                                "mMinDistanceAlias:" + mMinDistanceAlias + "\r\n");
                    }
                    //判断是否播报
                    if (mMinDistance < 1.5f && ibeaconCount > 0) {
                        //刷新界面
                        Message msg = new Message();
                        msg.what = 0x0101;
                        msg.obj = mMinDistanceAlias;
                        mLocationHandler.sendMessage(msg);
                        //语音播报
                        String speechContent = mContext.getResources().getString(R.string.now_location_str) + mMinDistanceAlias;
                        if(!mLastSpeakContent.equals(speechContent)){
                            mSpeechUtil.play(speechContent);
                            mLastSpeakContent=speechContent;
                        }else if(!mIsWait0){
                            mIsWait0=true;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000*5);
                                        mLastSpeakContent="";
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mIsWait0=false;
                                }
                            }).start();
                        }
                    } else if (0 == ibeaconCount) {
                        Message msg = new Message();
                        msg.what = 0x0102;
                        mLocationHandler.sendMessage(msg);
                        //语音播报:未搜索到定位设备
                        String speechContent = mContext.getResources().getString(R.string.have_no_location_device_str);
                        if(!mLastSpeakContent.equals(speechContent)){
                            mSpeechUtil.play(speechContent);
                            mLastSpeakContent=speechContent;
                        }else if(!mIsWait1){
                            mIsWait1=true;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000*5);
                                        mLastSpeakContent="";
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    mIsWait1=false;
                                }
                            }).start();
                        }
                    }
                    //判断是否要清除数据
                    if(deleteList.size()>0){
                        Message msg = new Message();
                        msg.what = 0x0103;
                        msg.obj=deleteList;
                        mLocationHandler.sendMessage(msg);
                    }
                }
            }
        }
    }

    public void removeList(List<ScannedDevice> deleteList){
        mList.removeAll(deleteList);
        notifyDataSetChanged();
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
        ViewHolder holder=null;

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);
            holder=new ViewHolder(convertView);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder)convertView.getTag();
        }

        String aliasStr= mDataStore.readData(getDeviceName(position));
        holder.aliasName.setText(aliasStr);
        holder.name.setText(item.getDisplayName());
        holder.address.setText(item.getDevice().getAddress());
        holder.rssi.setText(PREFIX_RSSI + Integer.toString(item.getRssi()));
        holder.lastupdated.setText(PREFIX_LASTUPDATED + DateUtil.get_yyyyMMddHHmmssSSS(item.getLastUpdatedMs()));
        DecimalFormat df=new DecimalFormat("0.00000");
        holder.distance.setText(df.format(item.getDistance()));

        Resources res = convertView.getContext().getResources();
        if (item.getIBeacon() != null) {
            holder.ibeaconInfo.setText(res.getString(R.string.label_ibeacon) + "\n"
                    + item.getIBeacon().toString());
            holder.ibeaconInfo.setTextColor(mContext.getResources().getColor(R.color.holo_blue_dark));
        } else {
            holder.ibeaconInfo.setText(res.getString(R.string.label_not_ibeacon));
            holder.ibeaconInfo.setTextColor(mContext.getResources().getColor(R.color.red));
        }
        holder.scanRecord.setText(item.getScanRecordHexString());

        return convertView;
    }

    private class ViewHolder{
        public TextView aliasName;
        public TextView name;
        public TextView address;
        public TextView rssi;
        public TextView lastupdated;
        public TextView distance;
        public TextView ibeaconInfo;
        public TextView scanRecord;

        public ViewHolder(View convertView){
            aliasName=(TextView)convertView.findViewById(R.id.device_alias_name);
            name= (TextView) convertView.findViewById(R.id.device_name);
            address= (TextView) convertView.findViewById(R.id.device_address);
            rssi= (TextView) convertView.findViewById(R.id.device_rssi);
            lastupdated= (TextView) convertView.findViewById(R.id.device_lastupdated);
            distance=(TextView) convertView.findViewById(R.id.distance_tv);
            ibeaconInfo = (TextView) convertView.findViewById(R.id.device_ibeacon_info);
            scanRecord = (TextView) convertView.findViewById(R.id.device_scanrecord);
        }
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
