package upsoft.ble.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.protocol.HTTP;

import android.os.Environment;
import android.util.Log;

public class CsvDumpUtil {
    private static final String TAG = CsvDumpUtil.class.getSimpleName();
    private static final String HEADER = "DisplayName,MAC Addr,RSSI,Last Updated,iBeacon flag,Proximity UUID,major,minor,TxPower";
    private static final String DUMP_PATH = "/iBeaconDetector/";

    /**
     * dump scanned device list csv to external storage. Filename include now timestamp.
     * 
     * @param deviceList BLE scanned device list
     * @return csv file path. If Error, return null.
     */
    public static String dump(List<ScannedDevice> deviceList) {
        if ((deviceList == null) || (deviceList.size() == 0)) {
            return null;
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + DUMP_PATH
                + DateUtil.get_nowCsvFilename();
        Log.d(TAG, "dump path=" + path);

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append("\n");
        for (ScannedDevice device : deviceList) {
            sb.append(device.toCsv()).append("\n");
        }
        try {
            FileUtils.write(new File(path), sb.toString(), HTTP.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return path;
    }

    private CsvDumpUtil() {
        // util
    }
}
