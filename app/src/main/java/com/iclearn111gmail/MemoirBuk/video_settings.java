package com.iclearn111gmail.MemoirBuk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class video_settings extends Activity {

    boolean def = true, record = true;
    String backgroundSoundPath, videoName;
    RadioGroup customization, sound;
    ListView lV;
    Button save, cancel;
    Intent intent;
    int last = 100, percentageTime[];
    ArrayList<String> imageNames, paths, recordings;
    int SET = 0001, BROWSE = 0002;
    SQLiteDatabase dbw;
    selfieDBHelper mDBHelper;
    File video, temp;
    private MediaRecorder mediaRecorder = null;
    myAdapter adapter;
    public static boolean videoExists = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_settings);
        mDBHelper = new selfieDBHelper(this);

        // variables for all the data for the settings
        intent = this.getIntent();
        imageNames = intent.getStringArrayListExtra(FolderView.IMAGE_NAMES);
        paths = intent.getStringArrayListExtra(FolderView.IMAGE_PATHS);
        recordings = intent.getStringArrayListExtra(FolderView.RECORDING_PATHS);
        percentageTime = new int[paths.size()];
        customization = (RadioGroup) findViewById(R.id.customization);
        sound = (RadioGroup) findViewById(R.id.sound);
        lV = (ListView) findViewById(R.id.list);
        adapter = new myAdapter();
        lV.setAdapter(adapter);
        save = (Button) findViewById(R.id.save);
        cancel = (Button) findViewById(R.id.cancel);

        lV.setOnTouchListener(new View.OnTouchListener() {
            // Setting on Touch Listener for handling the touch inside ScrollView
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });


        customization.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.def) {
                    // disable custom settings
                    def = true;
                    findViewById(R.id.record).setClickable(false);
                    //for(int i = 0; i < imageNames.size(); i++)
                    findViewById(R.id.percentage).setClickable(false);

                } else if (checkedId == R.id.custom) {
                    // enable custom settings
                    def = false;
                    findViewById(R.id.record).setClickable(true);
                    //for(int i = 0; i < imageNames.size(); i++)
                    findViewById(R.id.percentage).setClickable(true);
                }
            }

        });

        sound.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.record){
                    record = true;
                }
                else
                    record = false;
            }
        });

        // add all the selected images

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // submit
                if(!videoExists){
                    videoFolder vF = new videoFolder();
                    vF.execute();
                    videoExists = true;
                }
                videoName = ((EditText) findViewById(R.id.videoName)).getText().toString();
                generateVideo();
                setResult(RESULT_OK);
                // make the folder video in main Activity
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // cancel
                setResult(RESULT_CANCELED);
                finish();
            }
        });

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private class ViewHolder2 {
        TextView name;
        NumberPicker percentage;
    }

    private class myAdapter extends ArrayAdapter {
        public myAdapter() {
            super(video_settings.this, R.layout.item_view, R.id.name, imageNames);

        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder2 vH;
            LayoutInflater inflater = getLayoutInflater();
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.list_item, null, false);
                vH = new ViewHolder2();
                convertView.setTag(vH);
            } else {
                vH = (ViewHolder2) convertView.getTag();
            }

            vH.name = (TextView) convertView.findViewById(R.id.name);
            vH.name.setText(imageNames.get(position));
            vH.percentage = (NumberPicker) convertView.findViewById(R.id.percentage);
            vH.percentage.setMaxValue(100);
            vH.percentage.setMinValue(0);
            if (position == imageNames.size() - 1)
                vH.percentage.setValue(last);
            else
                vH.percentage.setValue(0);
            vH.percentage.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    if (position != imageNames.size() - 1) {
                        percentageTime[position] = newVal;
                        last -= newVal - oldVal;
                    } else{
                        percentageTime[position] = last;
                        picker.setValue(last);
                    }
                }
            });

            return convertView;
        }


    }


    void generateVideo() {
        // folder
        String state = Environment.getExternalStorageState();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        if (!Environment.MEDIA_MOUNTED.equals(state) || (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
            // internal storage
            temp = new File(getApplicationContext().getPackageName() + "/files/Videos", "temp.mp4");
            temp.mkdirs();
            video = new File(getApplicationContext().getPackageName() + "/files/Videos", "MB_" + timeStamp + ".mp4");
            video.mkdirs();
        } else {
            File memoirBukFolder = new File(Environment.getExternalStorageDirectory(), "MemoirBuk/Videos");
            // if(!(selfieFolder.exists() && selfieFolder.isDirectory()))
            memoirBukFolder.mkdirs();// error checking to be done catch exception
            temp = new File(memoirBukFolder, "temp.mp4");
            video = new File(memoirBukFolder, "MB_" + timeStamp + ".mp4");
        }
        Log.d("dsf", video.toString());


        // settings...
        // if default: separate function
        if (def) {
            defaultGenerate generate = new defaultGenerate();
            generate.execute();
        }

        // if customize:
        else {
            if(record){

                callAlert();
            }
            else{
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                startActivityForResult(intent,BROWSE);
            }
        }

        // add entry in database
        sqlDatabaseTaskWrite taskWrite = new sqlDatabaseTaskWrite();
        String[] s = {videoName, paths.get(0)};
        taskWrite.execute(s);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == BROWSE) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();


                // Will return "image:x*"
                String wholeID = DocumentsContract.getDocumentId(uri);
                Log.d("browse", wholeID);

// Split at colon, use second item in the array
                String id = wholeID.split(":")[1];

                String[] column = { MediaStore.Audio.Media.DATA };

// where id is equal to
                String sel = MediaStore.Audio.Media._ID + "=?";

                Cursor cursor = getContentResolver().
                        query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                column, sel, new String[]{ id }, null);

                String filePath = "";

                int columnIndex = cursor.getColumnIndex(column[0]);

                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex);
                }

                cursor.close();
                // File file = new File(uri.getPath());
                backgroundSoundPath = filePath;
                Log.d("sfd", backgroundSoundPath);
                // generate
                Generate generate = new Generate();
                generate.execute();
            }
        }
    }

    private class Generate extends AsyncTask<Void, Integer, Void> {

        ProgressDialog progressDialog;
        @Override
        protected  void onPreExecute(){
            progressDialog = new ProgressDialog(video_settings.this);
            progressDialog.setTitle("Generating video");
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer ... values){
            super.onProgressUpdate(values);
            progressDialog.setProgress(values[0]);

        }

        @Override
        protected Void doInBackground(Void... arg0) {



            try {
                FrameGrabber grabber1 = new FFmpegFrameGrabber(paths.get(0));
                FrameGrabber grabber2 = new FFmpegFrameGrabber(backgroundSoundPath);
                Log.d("hgbj", backgroundSoundPath);
                grabber1.start();
                grabber2.start();

                FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(video, 960,
                        540, grabber2.getAudioChannels());// 320, 240
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);//AV_CODEC_ID_MPEG4
                //recorder.setSampleRate(grabber2.getSampleRate());
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
                recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                recorder.setInterleaved(true);
                recorder.setVideoOption("tune", "zerolatency");
                recorder.setVideoOption("preset", "ultrafast");
                recorder.setFormat("mp4");//mp4
                recorder.setFrameRate(1);

                recorder.start();
                Frame frame1, frame2 = grabber2.grabFrame();


                long duration1 = grabber2.getLengthInTime()/1000000 + 1;
                System.out.println("here i m :" + duration1);
                int count = 0;

                for (int i = 0; i < paths.size(); i++) {
                    frame1 = grabber1.grabFrame();
                    Log.d("percentagetime: " , ""+percentageTime[i]);
                    for(int n = 0; n < duration1*percentageTime[i]/100; n++){
                        Log.d("h", "record image "+i+" called");
                        recorder.record(frame1);
                        count++;
                        publishProgress((int) (count*100/duration1));
                    }
                    if (i < paths.size() - 1) {
                        grabber1.stop();
                        grabber1 = new FFmpegFrameGrabber(paths.get(i + 1));
                        grabber1.start();
                    }
                }
                while(frame2 != null){
                    recorder.record(frame2);
                    frame2 = grabber2.grabFrame();
                }

                recorder.stop();
                grabber1.stop();
                grabber2.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(video), "video/*");
            startActivity(intent);
            finish();
        }
    }
    long duration;


    private class defaultGenerate extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... agrs) {

            try {
                FrameGrabber grabber1 = new FFmpegFrameGrabber(paths.get(0));
                FrameGrabber grabber2 = new FFmpegFrameGrabber(recordings.get(0));
                grabber1.start();
                grabber2.start();
                FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(video, 960,
                        540, grabber2.getAudioChannels());
               recorder.setInterleaved(true);
               recorder.setVideoOption("tune", "zerolatency");
               recorder.setVideoOption("preset", "ultrafast");
               //recorder.setVideoOption("crf", "0");
               //recorder.setVideoMetadata("Creation Time", video.toString());
              // recorder.setVideoBitrate(68 * 1024 * 8);
               recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
               recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setFormat("mp4");
                recorder.setFrameRate(1);
                //recorder.setAudioBitrate(125 * 1024 * 8);
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
               //recorder.setSampleRate(44100);
                recorder.start();


               // recorder.setAudioOption("crf", "0");
                //recorder.setAudioQuality(0);
                // recorder.setAudioChannels(2);
                //recorder.setVideoOption("crf", "28");
                // recorder.setVideoBitrate(10 * 1024 * 1024);
                //recorder.setFormat("mkv");
                Frame frame1, frame2;
                avutil.AVFrame frameYUV = null;

                long startTime, timestamp;
                ArrayList<Long> timestamps = new ArrayList<Long>();
                startTime = SystemClock.elapsedRealtime();
                int k = 0;
                for (int i = 0; i < paths.size(); i++) {
                    duration = grabber2.getLengthInTime();
                    frame1 = grabber1 == null ? null : grabber1.grabFrame();
                    frame2 = grabber2 == null ? null : grabber2.grabFrame();
                    boolean first = true;
                    // convert RGB tp yuv
                   // final IplImage imageRGB = cvLoadImage(paths.get(i));
                    //final IplImage imageYUV = cvCreateImage(cvSize(imageRGB.width(), imageRGB.height()), IPL_DEPTH_8U, 1);

                    //cvCvtColor(imageRGB, imageYUV, CV_BGR2YUV);
                    //OpenCVFrameConverter.ToIplImage frameConverter = new OpenCVFrameConverter.ToIplImage();
                    //frame1 = frameConverter.convert(imageYUV);
                    /*while((timestamp = SystemClock.elapsedRealtime() - startTime) < (grabber2.getLengthInTime()/1000)){

                        //Log.d("frame1", "" +grabber2.getLengthInTime()+" " + (System.currentTimeMillis() - startTime));
                        recorder.setTimestamp(timestamp);
                        recorder.record(frame1);
                    }*/
                    while (frame2 != null) {
                       // timestamp = 1000*(SystemClock.elapsedRealtime() - startTime);
                       // if(timestamp > recorder.getTimestamp())
                        //recorder.setTimestamp(timestamp);
                        recorder.record(frame2);
                        //timestamps.add(timestamp);
                        System.out.println(recorder.getTimestamp());
                        frame2 = grabber2.grabFrame();
                        first = false;
                    }
                    for(k = 0; k <= (duration)/1000000; k++){
                       // if(timestamps.get(k) > recorder.getTimestamp())
                       // recorder.setTimestamp(timestamps.get(k));
                        recorder.record(frame1);
                    }

                    if (i < paths.size() - 1) {
                        //Log.i("hello", "hello");
                        if (paths.get(i + 1) != null) {
                            grabber1.stop();
                            grabber1 = new FFmpegFrameGrabber(paths.get(i + 1));
                            grabber1.start();
                        } else {
                            grabber1 = null;
                        }

                        if (recordings.get(i + 1) != null) {
                            grabber2.stop();
                            grabber2 = new FFmpegFrameGrabber(recordings.get(i + 1));
                            grabber2.start();
                        } else {
                            grabber2 = null;
                        }

                    }
                }
                recorder.stop();
                grabber1.stop();
                grabber2.stop();


            } catch (Exception e) {
                e.printStackTrace();

            }
            return null;
        }

        protected void onPostExecute(Void result) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(video), "video/*");
            startActivity(intent);
            finish();
            /*ProcessVideo processVideo = new ProcessVideo();
            processVideo.execute();*/

        }
    }

    /*private class ProcessVideo extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params){
            try{
                // apply filter
                FrameGrabber grabber = new FFmpegFrameGrabber(temp);
                Log.d("tag", "temp: " + temp);
                grabber.setImageWidth(240);
                grabber.setImageHeight(320);
                grabber.setFormat("mkv");
                grabber.setFrameRate(10);
                grabber.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                grabber.setVideoBitrate(10 * 1024 * 1024);
                grabber.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                //grabber.start();

                FFmpegFrameRecorder recorder1 = new FFmpegFrameRecorder(video, grabber.getImageWidth(),
                        grabber.getImageHeight(), grabber.getAudioChannels());

                Frame frame;
               // recorder1.setInterleaved(true);
                //recorder1.setVideoOption("tune", "zerolatency");
                recorder1.setVideoOption("preset", "ultrafast");
                //recorder1.setVideoOption("crf", "28");
                //recorder1.setVideoBitrate(2000000);
                recorder1.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                recorder1.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder1.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder1.setFormat("mp4");
                recorder1.setFrameRate(1);
                // recorder1.setAudioOption("crf", "0");
                // recorder1.setAudioQuality(0);
                // recorder1.setAudioBitrate(192000);
                // recorder1.setSampleRate(44100);
                //recorder1.setAudioChannels(2);
                recorder1.start();
                FFmpegFrameFilter fFmpegFrameFilter = new FFmpegFrameFilter("tblend=all_mode=lighten,format=yuv420p", 960, 540);
                //fFmpegFrameFilter.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                //"setpts=N,fade=t=in:st=0:d=1,fade=t=out:st="+((duration/1000000)-1)+":d=1"
                //fFmpegFrameFilter.setFrameRate(10);
                //fFmpegFrameFilter.setFilters("setpts=N,fade=t=in:st=2.5:d=4");

                Frame frame3;
                fFmpegFrameFilter.start();
                while((frame = grabber.grabFrame()) != null){
                    fFmpegFrameFilter.push(frame);
                    while((frame3 = fFmpegFrameFilter.pull()) != null)
                    recorder1.record(frame3);

                }
                fFmpegFrameFilter.stop();
                recorder1.stop();
                grabber.stop();
            }
            catch (Exception e){
                e.printStackTrace();
                FFmpegLogCallback.set();
            }
            return null;
        }

        protected void onPostExecute(Void result){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(video), "video/*");
            startActivity(intent);
            finish();
        }
    }*/


    private class sqlDatabaseTaskWrite extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {

            dbw = mDBHelper.getWritableDatabase();
            return params;
        }

        protected void onPostExecute(String[] b) {

            // add the folder to the folders database
            ContentValues cv = new ContentValues();
            cv.put(selfieDB.selfieDB_videos.VIDEO_NAME, b[0]);
            cv.put(selfieDB.selfieDB_videos.ICON_PATH, b[1]);
            cv.put(selfieDB.selfieDB_videos.VIDEO_PATH, video.toString());
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            cv.put(selfieDB.selfieDB_videos.CREATION_DATE, timeStamp);
            long i = dbw.insert(selfieDB.selfieDB_videos.VIDEO_TABLE,
                    null,
                    cv);


            if (i == -1) {

            } else {

                String[] item = {b[1], b[0]};
            }

        }

    }

    private void callAlert() {
        final AlertDialog.Builder alertBuilder2 = new AlertDialog.Builder(this);
        alertBuilder2.setTitle("Recording");
        alertBuilder2.setMessage("Press Start to start recording?");

        alertBuilder2.setPositiveButton("Start", null);
        alertBuilder2.setNegativeButton("Stop", null);

        final AlertDialog dialog1 = alertBuilder2.create();

        dialog1.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button b1 = dialog1.getButton(DialogInterface.BUTTON_POSITIVE);
                final Button b2 = dialog1.getButton(DialogInterface.BUTTON_NEGATIVE);
                b2.setClickable(false);

                b1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        b2.setClickable(true);
                        String timeStamp = "";
                        timeStamp += new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

                        String state = Environment.getExternalStorageState();

                        File file;
                        if (!Environment.MEDIA_MOUNTED.equals(state) || (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
                            // internal storage
                            file = new File(getApplicationContext().getPackageName() + "/files/recordings", "MBrec_" + timeStamp + ".AMR");
                            file.mkdirs();
                        } else {
                            File memoirBukFolder = new File(Environment.getExternalStorageDirectory(), "MemoirBuk/recordings");
                            // if(!(selfieFolder.exists() && selfieFolder.isDirectory()))
                            memoirBukFolder.mkdirs();// error checking to be done catch exception
                            file = new File(memoirBukFolder, "MBrec_" + timeStamp + ".3gp");
                        }
                        backgroundSoundPath = file.getAbsolutePath();

                        mediaRecorder = new MediaRecorder();
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                        mediaRecorder.setOutputFile(file.toString());
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                        try {
                            mediaRecorder.prepare();
                        } catch (IOException e) {
                        }

                        mediaRecorder.start();
                        b1.setClickable(false);

                    }
                });

                b2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mediaRecorder != null) {
                            mediaRecorder.stop();
                            mediaRecorder.release();
                            mediaRecorder = null;
                        }
                        dialog.dismiss();
                        // generate
                        Generate generate = new Generate();
                        generate.execute();
                    }
                });
            }
        });

        dialog1.show();

    }


    private class videoFolder extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            dbw = mDBHelper.getWritableDatabase();
            return null;
        }

        protected void onPostExecute(Void b) {

            // add the folder to the folders database
            ContentValues cv = new ContentValues();
            cv.put(selfieDB.selfieDB_folders.FOLDER_NAME, "Videos");
            cv.put(selfieDB.selfieDB_folders.CREATION_DATE, "99999999_999999");
            cv.put(selfieDB.selfieDB_folders.ICON_PATH, "default"); // path to a placeholder image todo: maybe some other image
            long i = dbw.insert(selfieDB.selfieDB_folders.FOLDER_TABLE,
                    null,
                    cv);


            if (i == -1) {

            } else {
                String[] item = {"default", "Videos"};
                MainActivity.mAdapter.add(item);
            }

        }

    }

}
