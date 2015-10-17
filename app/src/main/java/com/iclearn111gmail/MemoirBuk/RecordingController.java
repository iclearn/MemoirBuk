package com.iclearn111gmail.MemoirBuk;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.widget.MediaController;

/**
 * Created by ssquasar on 3/9/15.
 */
public class RecordingController extends MediaController {

    public RecordingController(Context c, Boolean b){
        super(c, b);
    }

    public void hide(){}

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if(keyCode == KeyEvent.KEYCODE_BACK){
            Context c = getContext();
            ((Activity)c).finish();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

}
