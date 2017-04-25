package upsoft.ble.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by BASTA on 2017/4/24.
 */

public class NetHttpUtil {
    private String mTag=this.getClass().toString();
    private Context mContext;
    private Handler mHandler;

    public NetHttpUtil(Context ctx, Handler hdl){
        mContext=ctx;
        mHandler=hdl;
    }

    public static byte[] readBytes(InputStream is){
        try {
            byte[] buffer = new byte[1024];
            int len = -1 ;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((len = is.read(buffer)) != -1){
                baos.write(buffer, 0, len);
            }
            baos.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null ;
    }
    public static String readString(InputStream is){
        return new String(readBytes(is));
    }

    /**
     * @function     发起http post请求数据
     * @param       serviceUrl 请求的url地址，postData 请求的json数据对象
     * @return      String 返回的json数据字符串
     */
    public String getHttpPostData(String serviceUrl, JSONObject postData) throws Exception{
        String resultJsonStr="";//post请求返回的json字符串
        URL url=null;//将要发起请求的Url

        //判断网址是否合法
        boolean isUrlValid=serviceUrl.contains("http://")||serviceUrl.contains("https://");
        if(!isUrlValid||postData==null){
            return  resultJsonStr;
        }
        //TODO: 发起post请求相关操作
        try {
            url = new URL(serviceUrl);
            String content = String.valueOf(postData);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);// 设置允许输出
            conn.setRequestMethod("POST");
            conn.setRequestProperty("ser-Agent", "Fiddler");// 设置User-Agent: Fiddler
            conn.setRequestProperty("Content-Type", "application/json");// 设置contentType

            OutputStream os = conn.getOutputStream();
            os.write(content.getBytes());
            os.close();

            int code = conn.getResponseCode();//服务器返回的响应码
            if (code == 200) {// 等于200了,下面呢我们就可以获取服务器的数据了
                /*开始解析从服务器返回的数据*/
                InputStream is = conn.getInputStream();
                resultJsonStr = readString(is);
            }else {
                Log.d(mTag,"error code:"+code);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(mTag,"catch connect timeout exception:"+e.toString());
        }
        return resultJsonStr;
    }

    public  JSONObject jsonStr2JsonObject(String jsonStr){
        JSONObject resultJsonObject=null;
        if(jsonStr==null||jsonStr.equals("")){
            return null;
        }else{
            try{
                resultJsonObject = new JSONObject(jsonStr);
            }catch (Exception e){
                e.printStackTrace();
            }
            return resultJsonObject;
        }
    }

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }
}
