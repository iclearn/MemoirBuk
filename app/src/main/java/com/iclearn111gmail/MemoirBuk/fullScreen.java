package com.iclearn111gmail.MemoirBuk;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

/**
 * Created by ssquasar on 25/7/15.
 */
public class fullScreen extends Activity implements MediaPlayerControl {

    String TAG = "memoirBuk";
    String path, recording;

    MediaPlayer mp;
    private RecordingController controller;

    private void setController(){
        controller = new RecordingController(this, false);
        if(controller == null){
            Log.i(TAG,"it is null");
        }
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.fullImage));
        controller.setEnabled(true);
        // set disabled if recording path is ""
    }

    public boolean canSeekForward(){
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public int getCurrentPosition(){
        return 0;
    }

    public boolean canPause(){
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    public void seekTo(int a){
        if(mp != null){
            mp.seekTo(a);
        }
    }

    @Override
    public boolean isPlaying() {
        return mp.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    public void start(){
        mp = new MediaPlayer();
        try {
            if(recording != ""){
                mp.setDataSource(recording);
                mp.prepare();
                mp.start();
                controller.show(999999990);
            }
            else
                Toast.makeText(this, "No recording for Image", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

    }

    @Override
    public void pause() {
        if(mp != null){
            mp.pause();
            mp.release();
            mp = null;
        }

    }


    @Override
    public int getDuration() {
        if(mp != null)
        return mp.getDuration();
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full);
        setController();
        Intent intent = getIntent();
        ImageView imageView = (ImageView)findViewById(R.id.fullImage);

        path = intent.getStringExtra("path");
        recording = intent.getStringExtra("recording");
        //int width = imageView.getWidth();
       // int height = imageView.getHeight();
        // load image in async task
        loadImage li = new loadImage();
        li.execute(path);
        start();
    }



    public class loadImage extends AsyncTask<String, Void, Bitmap>{
        @Override
        protected Bitmap doInBackground(String ... p){
            Bitmap bitmap = decodeSampledBitmapFromUri(p[0]);
            return bitmap;
        }

        protected void onPostExecute(Bitmap b){
            ImageView imageView = (ImageView)findViewById(R.id.fullImage);
            imageView.setImageBitmap(b);

        }

    }

    public Bitmap decodeSampledBitmapFromUri(String path) {

        Bitmap bm = null;
        // First decode with inJustDecodeBounds=true to check dimensions
      //  final BitmapFactory.Options options = new BitmapFactory.Options();
        //options.inJustDecodeBounds = true;
        bm = BitmapFactory.decodeFile(path);
        try{

            ExifInterface ef = new ExifInterface(path.toString());
            int orientation = ef.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bm = rotateImage(bm, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    bm = rotateImage(bm, 180);
                    break;
            }
        }
        catch (IOException ie){
            ie.printStackTrace();
        }

        return bm;
    }

    public Bitmap rotateImage(Bitmap bitmap, int i){
        Matrix m = new Matrix();
        m.postRotate(i);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

}