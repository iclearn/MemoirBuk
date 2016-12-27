package com.iclearn111gmail.MemoirBuk;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// to be used for inside folder view

public class FolderView extends Activity {

    public GridAdapter mAdapter;
    private selfieDBHelper mDBHelper;
    private SQLiteDatabase dbw;
    private SQLiteDatabase dbr;
    private MediaRecorder mediaRecorder = null;
    Intent intent;
    String iconPath, folderName, recordingPath, caption;
    public static String IMAGE_NAMES = "image-names";
    public static String IMAGE_PATHS = "image-paths";
    public static String RECORDING_PATHS = "recording-paths";

    String TAG = "MemoirBuk";
    File image;
    int SET = 0001;
    int CAMERA_REQUEST = 0002;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Holo);
        setContentView(R.layout.activity_main);

        if(savedInstanceState == null){
            intent = getIntent();
            iconPath = intent.getStringExtra("iconPath");
            folderName = intent.getStringExtra("folderName");
        }
        else{
            Log.i(TAG, "i came here");

        }

        // set title folder name

        mDBHelper = new selfieDBHelper(this);

        // change it to load all images of a folder: change the sqlite query
        sqlDatabaseTaskRead read = new  sqlDatabaseTaskRead();
        read.execute();

        // TODO: add a gesture listener for short click, long click, and pinching

        final GridView gridView = (GridView) findViewById(R.id.gridview);

        mAdapter = new GridAdapter(this);
        gridView.setAdapter(mAdapter);


    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState){
        Log.i(TAG, "onr restore instance state");
        super.onRestoreInstanceState(savedState);
        iconPath = savedState.getString("iconPath");
        folderName = savedState.getString("foldername");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        Log.i(TAG, "on saved instance state");
        super.onSaveInstanceState(outState);
        outState.putString("foldername", folderName);
        outState.putString("iconPath", iconPath);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_folder, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_camera:
                camera();
                // implement this todo: exception if no camera?? well
                // i guess the person would be extremely stupid to download this app then :)
                return true;
            case R.id.action_settings:
                // add some settings
                return true;
            case R.id.delete_all:
                delete();
                return true;
            case R.id.share:
                share();
                return true;
            case R.id.video:
                shareVideo();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void shareVideo(){
        // activity for custom settings
        Intent settings = new Intent(this, video_settings.class);
        settings.putStringArrayListExtra(IMAGE_NAMES, mAdapter.imageNames);
        settings.putStringArrayListExtra(IMAGE_PATHS, mAdapter.imagePaths);
        settings.putStringArrayListExtra(RECORDING_PATHS, mAdapter.recordingPaths);
        startActivityForResult(settings, SET);

        return;
    }

    protected void share(){
        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setOnItemClickListener(null);
        // intent to share
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mAdapter.imageUris);
        shareIntent.setType("image/png");
        startActivity(shareIntent);
        mAdapter.selectionMode[0] = false;
        mAdapter.imageUris.clear();
        mAdapter.recordingPaths.clear();
        mAdapter.imagePaths.clear();
    }

    protected void delete(){
        // delete from database
        mDBHelper.deleteRows(mDBHelper.getWritableDatabase());
        // update adapter
        mAdapter.clear();
        // notify about the changes
        mAdapter.notifyDataSetChanged();
    }

    protected void camera(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // folder
        String state = Environment.getExternalStorageState();
        Log.i(TAG, "storage state: " + state);

        if(!Environment.MEDIA_MOUNTED.equals(state) || (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))){
            // internal storage
            image = new File(getApplicationContext().getPackageName()+"/files/" + folderName, "MB_"+timeStamp+".png");
            image.mkdirs();
            Log.i(TAG, "internal storage:" + image.toString());
        }
        else{
            File memoirBukFolder = new File(Environment.getExternalStorageDirectory(), "MemoirBuk/"+folderName);
            // if(!(selfieFolder.exists() && selfieFolder.isDirectory()))
            memoirBukFolder.mkdirs();// error checking to be done catch exception
            image = new File(memoirBukFolder, "MB_" + timeStamp +".png");
            Log.i(TAG, "external storage" + image.toString());
        }
        Uri uriSavedImage = Uri.fromFile(image);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        // todo: add description...alert box
        // button for recording sound...intent to record...store it somewhere

        // description alert box
        // taking input for the folder name
        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK){

            AlertDialog.Builder alert1 = new AlertDialog.Builder(this, R.style.Theme_Transparent);

            alert1.setTitle("Caption");
            alert1.setMessage("Give your memories some words");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            alert1.setView(input);

            alert1.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    caption = input.getText().toString();
                    callAlert();
                }
            });

            alert1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                    caption = "";
                    callAlert();
                }
            });

            alert1.show();
        }

    }


    private void callAlert(){
        final AlertDialog.Builder alertBuilder2 = new AlertDialog.Builder(this, R.style.Theme_Transparent);
        alertBuilder2.setTitle("Recording");
        alertBuilder2.setMessage("Do you want to start recording?");

        alertBuilder2.setPositiveButton("Start", null);
        alertBuilder2.setNegativeButton("Cancel", null);

        final AlertDialog dialog1 = alertBuilder2.create();

        dialog1.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button b1 = dialog1.getButton(DialogInterface.BUTTON_POSITIVE);
                final Button b2 = dialog1.getButton(DialogInterface.BUTTON_NEGATIVE);

                b1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String timeStamp = "";
                        timeStamp += new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

                        String state = Environment.getExternalStorageState();
                        Log.i(TAG, "storage state: " + state);

                        File file;
                        if (!Environment.MEDIA_MOUNTED.equals(state) || (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
                            // internal storage
                            file = new File(getApplicationContext().getPackageName() + "/files/" + folderName, "MBrec_" + timeStamp + ".AMR");
                            file.mkdirs();
                            Log.i(TAG, "internal storage:" + file.toString());
                        } else {
                            File memoirBukFolder = new File(Environment.getExternalStorageDirectory(), "MemoirBuk/" + folderName);
                            // if(!(selfieFolder.exists() && selfieFolder.isDirectory()))
                            memoirBukFolder.mkdirs();// error checking to be done catch exception
                            file = new File(memoirBukFolder, "MBrec_" + timeStamp + ".3gp");
                            Log.i(TAG, "external storage" + file.toString());
                        }
                        recordingPath = file.toString();

                        mediaRecorder = new MediaRecorder();
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                        mediaRecorder.setOutputFile(file.toString());
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                        try {
                            mediaRecorder.prepare();
                        } catch (IOException e) {
                            Log.e(TAG, "prepare() failed");
                        }

                        mediaRecorder.start();
                        b2.setText("Stop");
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
                        } else
                            recordingPath = "";
                        // insert in database
                        sqlDatabaseTaskWrite write = new sqlDatabaseTaskWrite();
                        write.execute();
                        dialog.dismiss();
                    }
                });
            }
        });

        dialog1.show();

    }

    private class sqlDatabaseTaskWrite extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void ... params){

            dbw = mDBHelper.getWritableDatabase();
            return true;
        }
        protected void onPostExecute(Boolean b){
            if(b){
                ContentValues cv = new ContentValues();
                cv.put(selfieDB.selfieDB_main.IMAGE_PATH,
                        image.toString());
                cv.put(selfieDB.selfieDB_main.CAPTION, caption);
                cv.put(selfieDB.selfieDB_main.FOLDER_NAME, folderName);
                cv.put(selfieDB.selfieDB_main.RECORDING_PATH, recordingPath);

                long i = dbw.insert(selfieDB.selfieDB_main.TABLE_NAME,
                        null,
                        cv);

                // here to make a string array
                String[] args = {image.toString(), caption, recordingPath};
                mAdapter.add(args);
                if(i == -1){
                    // error
                    Toast.makeText(FolderView.this, "Picture couldn't be saved :(", Toast.LENGTH_LONG);
                }
                else{
                    Toast.makeText(FolderView.this, "Picture number " + String.valueOf(i) +" taken", Toast.LENGTH_LONG);
                }
            }
        }

    }

    private class sqlDatabaseTaskRead extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            dbr = mDBHelper.getReadableDatabase();
            return true;
        }

        protected void onPostExecute(Boolean b) {
            LoadImagesTask loadImagesTask = new LoadImagesTask();
            loadImagesTask.execute();
        }
    }

    private class LoadImagesTask extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void ... db){
            String projection[] = {selfieDB.selfieDB_main._ID,
            selfieDB.selfieDB_main.IMAGE_PATH, selfieDB.selfieDB_main.RECORDING_PATH, selfieDB.selfieDB_main.CAPTION};

            String sortOrder = selfieDB.selfieDB_main.IMAGE_PATH + " DESC";
            String selectionArgs[] = {folderName};
            if(folderName == null){
                Log.i(TAG, "its null");
            }

            final Cursor c = dbr.query(
                    selfieDB.selfieDB_main.TABLE_NAME,
                    projection,
                    selfieDB.selfieDB_main.FOLDER_NAME+"=?",
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );

            if(c != null && c.getCount() > 0){
                c.moveToFirst();
                String path = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_main.IMAGE_PATH));
                String recPath = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_main.RECORDING_PATH));
                String caption = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_main.CAPTION));

                final String[] args = {path, caption, recPath};
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.add(args);
                        while(c.moveToNext()) {
                            String path1 = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_main.IMAGE_PATH));
                            String recPath1 = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_main.RECORDING_PATH));
                            String caption1 = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_main.CAPTION));
                            final String[] args = {path1, caption1, recPath1};
                            if(path1 != null)
                            mAdapter.add(args);
                        }
                    }
                });

            }
            return true;
        }
    }
}
