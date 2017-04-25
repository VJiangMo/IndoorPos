package upsoft.ble.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Iterator;

import upsoft.ble.indoorPos.R;

/**
 * Created by yangzhou on 2017/4/15.
 */

public class DataStore {
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private static DataStore mDataStore;

    public static DataStore singleton(Context ctx){
        if(null==mDataStore){
            mDataStore=new DataStore(ctx);
        }
        return mDataStore;
    }

    private DataStore(Context ctx){
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

    public void writeData(String key,String value){
        mEditor.putString(key, value);
        mEditor.commit();
    }

    public String readAlias(String key){
        String res=mSharedPreferences.getString(key,mContext.getResources().getString(R.string.unkown_location_str));
        return res;
    }

    public String readData(String key){
        String res=mSharedPreferences.getString(key,"");
        return res;
    }
}
