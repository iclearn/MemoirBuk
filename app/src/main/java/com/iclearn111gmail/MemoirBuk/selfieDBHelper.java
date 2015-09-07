package com.iclearn111gmail.MemoirBuk;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ssquasar on 16/7/15.
 */
public class selfieDBHelper extends SQLiteOpenHelper {
    private static final String SQL_CREATE_IMAGE_TABLE = "CREATE TABLE " + selfieDB.selfieDB_main.TABLE_NAME + " (" + selfieDB.selfieDB_main._ID
            + " INTEGER PRIMARY KEY, " +
            selfieDB.selfieDB_main.FOLDER_NAME + " TEXT, " +
            selfieDB.selfieDB_main.RECORDING_PATH + " TEXT, " +
            selfieDB.selfieDB_main.IMAGE_PATH + " TEXT, " +
            selfieDB.selfieDB_main.CAPTION + " TEXT " +
            " )";

    private static final String SQL_CREATE_FOLDERS_TABLE = "CREATE TABLE " + selfieDB.selfieDB_folders.FOLDER_TABLE + " (" +
            selfieDB.selfieDB_folders._ID + " INTEGER PRIMARY KEY , " +
            selfieDB.selfieDB_folders.FOLDER_NAME + " TEXT, " +
            selfieDB.selfieDB_folders.CREATION_DATE + " TEXT, " +
            selfieDB.selfieDB_folders.ICON_PATH + " TEXT" +
            " )";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + selfieDB.selfieDB_main.TABLE_NAME;
    private static final String SQL_DELETE_FOLDERS_TABLE = "DROP TABLE IF EXISTS " + selfieDB.selfieDB_folders.FOLDER_TABLE;

    private static final String SQL_DELETE_ROWS = "DELETE FROM " + selfieDB.selfieDB_main.TABLE_NAME;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "memoirBuk.db";

    public void deleteRows(SQLiteDatabase db){
        db.execSQL(SQL_DELETE_ROWS);
    }

    public selfieDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL(SQL_CREATE_FOLDERS_TABLE);
        db.execSQL(SQL_CREATE_IMAGE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL(SQL_DELETE_FOLDERS_TABLE);
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion, newVersion);
    }
}