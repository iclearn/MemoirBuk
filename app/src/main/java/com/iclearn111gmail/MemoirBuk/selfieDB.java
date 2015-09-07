package com.iclearn111gmail.MemoirBuk;

import android.provider.BaseColumns;

/**
 * Created by ssquasar on 16/7/15.
 */
public class selfieDB {
    // preventing instantiating this class
    public void sefieDB(){};

    // folders table
    public static abstract class selfieDB_main implements BaseColumns{
        public static final String TABLE_NAME = "main_table";
        public static final String FOLDER_NAME = "folder_name";
        public static final String IMAGE_PATH = "path";// IMAGE LOCATION
        public static final String RECORDING_PATH = "recording_path";
        public static final String CAPTION = "caption";
    }

    // what if i create a separate table for folder name, creation time date and icon...cool
    public static abstract class selfieDB_folders implements BaseColumns{
        public static final String FOLDER_TABLE = "folder_table";
        public static final String FOLDER_NAME = "folder_name";
        public static final String CREATION_DATE = "creation_date";
        public static final String ICON_PATH = "icon_path";
    }
}
