package com.iclearn111gmail.MemoirBuk;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.GridView;

/**
 * Created by ssquasar on 19/12/15.
 */
public class Videos extends Activity{

    public GridAdapter mAdapter;
    private selfieDBHelper mDBHelper;
    private SQLiteDatabase dbw;// for deleting a video
    private SQLiteDatabase dbr;
   // Intent intent;
    //String iconPath, videoName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Holo);
        setContentView(R.layout.activity_main);

       /* if(savedInstanceState == null){
            intent = getIntent();
            iconPath = intent.getStringExtra("iconPath");
            videoName = intent.getStringExtra("videoName");
        }*/

        mDBHelper = new selfieDBHelper(this);


        final GridView gridView = (GridView) findViewById(R.id.gridview);

        mAdapter = new GridAdapter(this);
        gridView.setAdapter(mAdapter);
        // change it to load all images of videos folder: change the sqlite query
        sqlDatabaseTaskRead read = new  sqlDatabaseTaskRead();
        read.execute();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private class sqlDatabaseTaskWrite extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void ... params){

            dbw = mDBHelper.getWritableDatabase();
            return true;
        }
        protected void onPostExecute(Boolean b){
            if(b){
             // deleting a video

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
            String projection[] = {selfieDB.selfieDB_videos._ID,
                    selfieDB.selfieDB_videos.ICON_PATH, selfieDB.selfieDB_videos.VIDEO_PATH,
                    selfieDB.selfieDB_videos.VIDEO_NAME};

            String sortOrder = selfieDB.selfieDB_videos.VIDEO_PATH + " DESC";
            // cursor to select all the videos


            final Cursor c = dbr.query(
                    selfieDB.selfieDB_videos.VIDEO_TABLE,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    sortOrder
            );

            if(c != null && c.getCount() > 0){
                c.moveToFirst();
                String icon_path = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_videos.ICON_PATH));
                String video_Path = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_videos.VIDEO_PATH));
                String video_name = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_videos.VIDEO_NAME));

                final String[] args = {icon_path, video_name, video_Path, "Videos"};
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.add(args);
                        while(c.moveToNext()) {
                            String icon_path = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_videos.ICON_PATH));
                            String video_Path = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_videos.VIDEO_PATH));
                            String video_name = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_videos.VIDEO_NAME));

                            final String[] args = {icon_path, video_name, video_Path, "Videos"};
                            if(icon_path != null)
                                mAdapter.add(args);
                        }
                    }
                });

            }
            return true;
        }
    }
}
