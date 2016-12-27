package com.iclearn111gmail.MemoirBuk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

// to be used for inside folder view

public class MainActivity extends Activity {

    public static GridAdapter mAdapter;
    private selfieDBHelper mDBHelper; 
    private SQLiteDatabase dbw;
    private SQLiteDatabase dbr;
    String TAG = "memoirBuk";
    GridView gridView;
    File image;// todo: set it to the folder icon obtained from the database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Holo);
        setContentView(R.layout.activity_main);

        // grid view defined in the xml layout
        gridView = (GridView) findViewById(R.id.gridview);

        // adapter for the folder view
        mAdapter = new GridAdapter(this);
        gridView.setAdapter(mAdapter);

        // database helper reference
        mDBHelper = new selfieDBHelper(this);

        // read the folder table to display in the main activity
        sqlDatabaseTaskRead read = new  sqlDatabaseTaskRead();
        read.execute();

        // TODO: add a gesture listener for short click- open the folder, long click- for deletion and sharing


    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // this is the parent activity here
        int id = item.getItemId();

        switch (id){
            case R.id.addFolder:
                createFolder();
                return true;
            case R.id.action_settings:
                // add some settings
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createFolder(){

        // taking input for the folder name
        AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.Theme_Transparent);


        alert.setTitle("Folder Name");
        alert.setMessage("What would you like to name this folder?");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        // value stores the name for the folder
        final String[] value = new String[2];

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                value[0] = input.getText().toString();
                if (value[0] == "") {
                    Toast.makeText(getParent(), "Please provide a name!", Toast.LENGTH_LONG).show();
                }
                if(value[0] != null  && value[0] != ""){
                    // writing it to the database
                    sqlDatabaseTaskWrite write = new sqlDatabaseTaskWrite();
                    write.execute(value);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();


    }

     private class sqlDatabaseTaskWrite extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String ... params){

            dbw = mDBHelper.getWritableDatabase();
            return params[0];
        }
        protected void onPostExecute(String b){

            // add the folder to the folders database
            ContentValues cv = new ContentValues();
            cv.put(selfieDB.selfieDB_folders.FOLDER_NAME, b);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            cv.put(selfieDB.selfieDB_folders.CREATION_DATE, timeStamp);
            cv.put(selfieDB.selfieDB_folders.ICON_PATH, "default"); // path to a placeholder image
            long i = dbw.insert(selfieDB.selfieDB_folders.FOLDER_TABLE,
                    null,
                    cv);


            if(i == -1){
                // error
                Toast.makeText(MainActivity.this, "Folder could not be created :(", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(MainActivity.this, "Folder: " + " created", Toast.LENGTH_SHORT).show();
                // make string array of icon path and folder name and add it to the adapter
                String[] item = {"default", b};
                mAdapter.add(item);
            }

        }

    }

    private class sqlDatabaseTaskRead extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void ...params){

            dbr = mDBHelper.getReadableDatabase();
            return true;
        }

        protected void onPostExecute(Boolean b){
            LoadFoldersTask loadFoldersTask = new LoadFoldersTask();
            loadFoldersTask.execute();
        }
    }

    private class LoadFoldersTask extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void ... db){
            String projection[] = {selfieDB.selfieDB_folders._ID,
                    selfieDB.selfieDB_folders.FOLDER_NAME, selfieDB.selfieDB_folders.CREATION_DATE, selfieDB.selfieDB_folders.ICON_PATH};

            // add folder creation date to the database and use it to sort
            String sortOrder = selfieDB.selfieDB_folders.CREATION_DATE+ " DESC";

            final Cursor c = dbr.query(
                    selfieDB.selfieDB_folders.FOLDER_TABLE,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    sortOrder
            );

            if(c != null && c.getCount() > 0){
                c.moveToFirst();

                final String iconPath = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_folders.ICON_PATH));
                final String folderName = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_folders.FOLDER_NAME));
                final String[] item = {iconPath, folderName};

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.add(item);
                        while(c.moveToNext()) {
                            String iconPath1 = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_folders.ICON_PATH));
                            String folderName1 = c.getString(c.getColumnIndexOrThrow(selfieDB.selfieDB_folders.FOLDER_NAME));
                            String[] item1 = {iconPath1, folderName1};
                            if(item1 != null)
                                mAdapter.add(item1);
                        }
                    }
                });

            }
            return true;
        }
    }
}