package upsoft.ble.indoorPos;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import upsoft.ble.util.DataStore;
import upsoft.ble.util.DateUtil;
import upsoft.ble.util.NetHttpUtil;
import upsoft.ble.util.OfflineSpeechUtil;
import upsoft.ble.util.ScannedDevice;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.upsoft.ibeacon.IBeacon;

import org.json.JSONException;
import org.json.JSONObject;


public class DeviceAdapter extends ArrayAdapter<ScannedDevice> {
    private String mTag=this.getClass().toString();
    private static final String PREFIX_RSSI = "信号强度：";
    private static final String PREFIX_LASTUPDATED = "上次刷新时间：";
    private List<ScannedDevice> mList;
    private LayoutInflater mInflater;
    private Context mContext;
    private DataStore mDataStore;
    private OfflineSpeechUtil mSpeechUtil;
    private LocationThread mLocationThread;
    private Handler mLocationHandler;
    private HttpPostHdl mHttpPostHdl;
    private NetHttpUtil mNetHttpUtil;
    private int mResId;
    private int updataCount=0;

    private class HttpPostHdl extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int msgId=msg.what;
            Bundle jsonBundle=(Bundle)msg.obj;
            String jsonStr=jsonBundle.getString("resJson");
            Log.d(mTag,"result json:"+jsonStr);

            switch (msgId){
                case 0x0101:{
                    try {
                        JSONObject jobj=new JSONObject(jsonStr);
                        if(jobj.has("result")){
                            String loginResult=jobj.getString("result");
                            if(!loginResult.equals("1")){//up data fail
                                String reasonStr=jobj.getString("reason");
                                Toast.makeText(mContext,"up_data fail:"+reasonStr,Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case 0x0102:{
                    try {
                        JSONObject jobj=new JSONObject(jsonStr);
                        if(jobj.has("result")){
                            String loginResult=jobj.getString("result");
                            if(!loginResult.equals("1")){//up data fail
                                String reasonStr=jobj.getString("reason");
                                Toast.makeText(mContext,"down_data fail:"+reasonStr,Toast.LENGTH_SHORT).show();
                            }else {
                                String down_data=jobj.getString("down_data");
                                JSONObject data=new JSONObject(down_data);
                                String uuid_str=data.getString("uuid");
                                String alias_str=data.getString("alias");
                                mDataStore.writeData(uuid_str,alias_str);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    //down_data api
    private void down_data(final String uuid_str){
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonData=new JSONObject();
                try {
                    jsonData.put("user",mDataStore.readData("usr"));
                    jsonData.put("uuid",uuid_str);

                    String jsonStr=jsonData.toString();
                    Log.d(mTag,"down_data param json:"+jsonStr);
                    String urlStr=getResStr(R.string.url)+"down_data";
                    String resJson=mNetHttpUtil.getHttpPostData(urlStr,jsonData);
                    if(!resJson.equals("")){
                        Bundle dataBundle=new Bundle();
                        dataBundle.putString("resJson",resJson);
                        Message msg=new Message();
                        msg.what=0x0102;
                        msg.obj=dataBundle;
                        mHttpPostHdl.sendMessage(msg);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //up_data api
    private void up_data(final String uuid_str, final String alias_name, final String distance){
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject jsonData=new JSONObject();
                try {
                    jsonData.put("user",mDataStore.readData("usr"));
                    jsonData.put("uuid",uuid_str);
                    jsonData.put("alias",alias_name);
                    jsonData.put("up_time", DateUtil.get_yyyyMMddHHmmssSSS(System.currentTimeMillis()));
                    jsonData.put("distance",distance);

                    String jsonStr=jsonData.toString();
                    Log.d(mTag,"up_data param json:"+jsonStr);
                    String urlStr=getResStr(R.string.url)+"up_data";
                    String resJson=mNetHttpUtil.getHttpPostData(urlStr,jsonData);
                    if(!resJson.equals("")){
                        Bundle dataBundle=new Bundle();
                        dataBundle.putString("resJson",resJson);
                        Message msg=new Message();
                        msg.what=0x0101;
                        msg.obj=dataBundle;
                        mHttpPostHdl.sendMessage(msg);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String getResStr(int resId){
        return mContext.getResources().getString(resId);
    }

    public void startLocationThread(){
        mLocationThread.startRun();
    }

    public void stopLocationThread(){
        mLocationThread.stopRun();
    }

    private class LocationThread extends Thread{
        private boolean mRunFlag=true;
        private double mMinDistance=0.0f;
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
                List<ScannedDevice> offlineDeviceList=new ArrayList<ScannedDevice>();

                try {
                    Thread.sleep(1000);//线程休眠1000ms
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
                                offlineDeviceList.add(device);
                            }
                            if (device.getIBeacon() != null) {//当前蓝牙设备为ibeacon设备
                                ibeaconCount++;
                                double deviceDistance = device.getDistance();
                                String deviceName = device.getDisplayName();
                                Log.d("++++++Distance", "deviceDistance:" + deviceDistance + ",\n" +
                                        "deviceName:" + deviceName + "\n");
                                if (mMinDistance < 0.00001f) {
                                    mMinDistance = deviceDistance;
                                    mMinDistanceAlias = mDataStore.readAlias(deviceName);
                                }
                                if (mMinDistance > deviceDistance) {
                                    mMinDistance = deviceDistance;
                                    mMinDistanceAlias = mDataStore.readAlias(deviceName);
                                }
                            }
                        }
                    }
                    //判断是否播报
                    if (mMinDistance < 4.0f && ibeaconCount > 0) {
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
                    if(offlineDeviceList.size()>0){
                        Message msg = new Message();
                        msg.what = 0x0103;
                        msg.obj=offlineDeviceList;
                        mLocationHandler.sendMessage(msg);
                    }
                }
            }
        }
    }

    public void removeList(List<ScannedDevice> offlineDeviceList){
        mList.removeAll(offlineDeviceList);
        notifyDataSetChanged();
    }

    public DeviceAdapter(Context context, int resId, List<ScannedDevice> objects,
                         OfflineSpeechUtil speech, DataStore dataStore,Handler handler) {
        super(context, resId, objects);
        mContext=context;
        mHttpPostHdl=new HttpPostHdl();
        mNetHttpUtil=new NetHttpUtil(mContext,mHttpPostHdl);
        mResId = resId;
        mList=objects;
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
        updataCount++;

        IBeacon iBeacon=item.getIBeacon();
        if(iBeacon!=null&&updataCount%20==0){
            down_data(item.getDisplayName());
        }

        String aliasStr= mDataStore.readAlias(getDeviceName(position));
        holder.aliasName.setText(aliasStr);
        holder.name.setText(item.getDisplayName());
        holder.address.setText(item.getDevice().getAddress());
        holder.rssi.setText(PREFIX_RSSI + Integer.toString(item.getRssi()));
        holder.lastupdated.setText(PREFIX_LASTUPDATED + DateUtil.get_yyyyMMddHHmmssSSS(item.getLastUpdatedMs()));
        DecimalFormat df=new DecimalFormat("0.00000");
        holder.distance.setText(df.format(item.getDistance()));

        Resources res = convertView.getContext().getResources();
        if (iBeacon != null) {
            holder.ibeaconInfo.setText(res.getString(R.string.label_ibeacon) + "\n"
                    + iBeacon.toString());
            holder.ibeaconInfo.setTextColor(mContext.getResources().getColor(R.color.holo_blue_dark));
            if(updataCount>=40){
                up_data(item.getDisplayName(),aliasStr,Double.toString(item.getDistance()));
                updataCount=0;
            }
        } else {
            holder.ibeaconInfo.setText(res.getString(R.string.label_not_ibeacon));
            holder.ibeaconInfo.setTextColor(mContext.getResources().getColor(R.color.red));
        }
        //holder.scanRecord.setText(item.getScanRecordHexString());
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
        //public TextView scanRecord;

        public ViewHolder(View convertView){
            aliasName=(TextView)convertView.findViewById(R.id.device_alias_name);
            name= (TextView) convertView.findViewById(R.id.device_name);
            address= (TextView) convertView.findViewById(R.id.device_address);
            rssi= (TextView) convertView.findViewById(R.id.device_rssi);
            lastupdated= (TextView) convertView.findViewById(R.id.device_lastupdated);
            distance=(TextView) convertView.findViewById(R.id.distance_tv);
            ibeaconInfo = (TextView) convertView.findViewById(R.id.device_ibeacon_info);
            //scanRecord = (TextView) convertView.findViewById(R.id.device_scanrecord);
        }
    }

    /**
     * add or update BluetoothDevice List
     * 
     * @param newBluetoothDevice Scanned Bluetooth Device
     * @param rssi RSSI
     * @param scanRecord advertise data
     * @return summary ex. "iBeacon:3 (Total:10)"
     */
    public String update(BluetoothDevice newBluetoothDevice, int rssi, byte[] scanRecord) {
        if ((newBluetoothDevice == null) || (newBluetoothDevice.getAddress() == null)) {
            return "";
        }
        long now = System.currentTimeMillis();

        boolean contains = false;
        for (ScannedDevice device : mList) {
            if (newBluetoothDevice.getAddress().equals(device.getDevice().getAddress())) {
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
            ScannedDevice newScanDevice=new ScannedDevice(newBluetoothDevice, rssi, scanRecord, now);
            if(newScanDevice.getIBeacon()!=null){//仅仅添加定位设备(ibeacon)，过滤掉其他设备(ibeacon)
                mList.add(newScanDevice);
            }
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
