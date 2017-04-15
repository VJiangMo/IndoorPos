package upsoft.ble.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import com.upsoft.ibeacon.IBeacon;

/** LeScanned Bluetooth Device */
public class ScannedDevice {
    private static final String UNKNOWN = "Unknown";
    /** BluetoothDevice */
    private BluetoothDevice mDevice;
    /** RSSI */
    private int mRssi;
    /** Display Name */
    private String mDisplayName;
    /** Advertise Scan Record */
    private byte[] mScanRecord;
    /** parsed iBeacon Data */
    private IBeacon mIBeacon;
    /** last updated (Advertise scanned) */
    private long mLastUpdatedMs;

    public double getDistance(){
        int textPower=mIBeacon.getTxPower();
        double rssi=mIBeacon.getRssi();
        return mIBeacon.calculateAccuracy(textPower,rssi);
    }

    public ScannedDevice(BluetoothDevice device, int rssi, byte[] scanRecord, long now) {
        if (device == null) {
            throw new IllegalArgumentException("BluetoothDevice is null");
        }
        mLastUpdatedMs = now;
        mDevice = device;
        mDisplayName = device.getName();
        if ((mDisplayName == null) || (mDisplayName.length() == 0)) {
            mDisplayName = UNKNOWN;
        }
        mRssi = rssi;
        mScanRecord = scanRecord;
        checkIBeacon();
    }

    private void checkIBeacon() {
        if (mScanRecord != null) {
            mIBeacon = IBeacon.fromScanData(mScanRecord, mRssi);
        }
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getRssi() {
        return mRssi;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public long getLastUpdatedMs() {
        return mLastUpdatedMs;
    }

    public void setLastUpdatedMs(long lastUpdatedMs) {
        mLastUpdatedMs = lastUpdatedMs;
    }

    public byte[] getScanRecord() {
        return mScanRecord;
    }

    public String getScanRecordHexString() {
        return ScannedDevice.asHex(mScanRecord);
    }

    public void setScanRecord(byte[] scanRecord) {
        mScanRecord = scanRecord;
        checkIBeacon();
    }

    public IBeacon getIBeacon() {
        return mIBeacon;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        // DisplayName,MAC Addr,RSSI,Last Updated,iBeacon flag,Proximity UUID,major,minor,TxPower
        sb.append(mDisplayName).append(",");
        sb.append(mDevice.getAddress()).append(",");
        sb.append(mRssi).append(",");
        sb.append(DateUtil.get_yyyyMMddHHmmssSSS(mLastUpdatedMs)).append(",");
        if (mIBeacon == null) {
            sb.append("false,,0,0,0");
        } else {
            sb.append("true").append(",");
            sb.append(mIBeacon.toCsv());
        }
        return sb.toString();
    }


    @SuppressLint("DefaultLocale")
    public static String asHex(byte bytes[]) {
        if ((bytes == null) || (bytes.length == 0)) {
            return "";
        }

        StringBuffer sb = new StringBuffer(bytes.length * 2);

        for (int index = 0; index < bytes.length; index++) {
            int bt = bytes[index] & 0xff;

            if (bt < 0x10) {
                sb.append("0");
            }

            sb.append(Integer.toHexString(bt).toUpperCase());
        }

        return sb.toString();
    }
}
