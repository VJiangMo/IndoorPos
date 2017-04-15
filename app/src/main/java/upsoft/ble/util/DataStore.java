package upsoft.ble.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by yangzhou on 2017/4/15.
 */

public class DataStore {
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    public DataStore(Context ctx){
        mContext=ctx;
        mSharedPreferences=mContext.getSharedPreferences("IndoorPosData",
                Context.MODE_PRIVATE);
        mEditor=mSharedPreferences.edit();
    }

    public void writeData(HashMap<String,String> data){
        Iterator iter = data.entrySet().iterator();
        while (iter.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) iter.next();
            String key = (String)entry.getKey();
            String val = (String)entry.getValue();
            mEditor.putString(key,val);
        }
        mEditor.commit();
    }

    public String readData(String key){
        String res=mSharedPreferences.getString(key,"");
        return res;
    }
}
