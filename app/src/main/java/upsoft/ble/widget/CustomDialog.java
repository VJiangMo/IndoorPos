package upsoft.ble.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import upsoft.ble.iBeaconPos.R;

/**
 * Created by yangzhou on 2017/4/15.
 */

public class CustomDialog extends Dialog {
    private EditText editText;
    private Button setButton,cancleButton;

    public CustomDialog(Context context) {
        super(context, R.style.CustomDialogStyle);
        setCustomDialog();
    }

    private void setCustomDialog() {
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.custom_dialog, null);
        editText = (EditText) mView.findViewById(R.id.number);
        setButton = (Button) mView.findViewById(R.id.setButton);
        cancleButton=(Button)mView.findViewById(R.id.cancleButton);
        super.setContentView(mView);
    }

    public View getEditText(){
        return editText;
    }

    @Override
    public void setContentView(int layoutResID) {
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
    }

    @Override
    public void setContentView(View view) {
    }

    /**
     * 设置键监听器
     * @param listener
     */
    public void setOnPositiveListener(View.OnClickListener listener){
        setButton.setOnClickListener(listener);
    }

    /**
     * 取消键监听器
     * @param listener
     */
    public void setOnNegativeListener(View.OnClickListener listener){
        cancleButton.setOnClickListener(listener);
    }
}


