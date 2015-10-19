package com.iclearn111gmail.MemoirBuk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ssquasar on 15/7/15.
 */


// TODO: implement database specific functions
public class GridAdapter extends BaseAdapter {

    // string array contains the image path as well as description
    private final List<String[]> myImages = new ArrayList<String[]>();
    private final Context mContext;
    private final String TAG = "debug";
    public boolean[] selectionMode = new boolean[1];
    public ArrayList<Uri> imageUris = new ArrayList<>();

    public GridAdapter(Context context){
        mContext = context;
        selectionMode[0] = false;
    }

    // add an image to the grid adapter
    public void add(String[] image){
        myImages.add(image);

        notifyDataSetChanged();
    }

    // clear all
    public void clear(){
        myImages.clear();
        notifyDataSetChanged();
    }

    // number of items in current list
    @Override
    public int getCount(){
        return myImages.size();
    }

    // image at a given position
    @Override
    public Object getItem(int pos){
        return myImages.get(pos);
    }

    // get id for a given image
    @Override
    public long getItemId(int pos){
        return pos;
    }

    // view for a single image

    public class ViewHolder{
        ImageView imageView;
        TextView caption;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        final String[] image = (String[]) getItem(position);
        LinearLayout view = (LinearLayout) convertView;
        final ViewHolder vH;

        if(view == null){
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (LinearLayout) inflater.inflate(R.layout.item_view, null);
            vH = new ViewHolder();
            // imageView.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, GridView.LayoutParams.MATCH_PARENT));
            vH.imageView = (ImageView) view.findViewById(R.id.image);
            vH.caption = (TextView) view.findViewById(R.id.caption);
            vH.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            vH.imageView.setPadding(8, 8, 8, 8);

            // set tag as the path
            vH.imageView.setTag(image[0]);
            view.setOrientation(LinearLayout.VERTICAL);
            view.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.FILL_PARENT, GridView.LayoutParams.FILL_PARENT));
            view.setTag(vH);

        }
        else
        vH = (ViewHolder) view.getTag();


        vH.caption.setText(image[1]);

        class AttachBitmap extends AsyncTask<String, Void, Bitmap>{
            @Override
            protected Bitmap doInBackground(String ... p){
                Bitmap bitmap = decodeSampledBitmapFromUri(p[0], 150, 150);
                try{

                    ExifInterface ef = new ExifInterface(p[0].toString());
                    int orientation = ef.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    switch (orientation){
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            bitmap = rotateImage(bitmap, 90);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            bitmap = rotateImage(bitmap, 180);
                            break;
                    }
                }
                catch (IOException ie){
                    ie.printStackTrace();
                }

                return bitmap;
            }
            protected void onPostExecute(Bitmap b){
                (vH.imageView).setImageBitmap(b);
            }

        }

        if((image[0] == null) || (image[0].equals("default"))){
            vH.imageView.setImageResource(R.drawable.folder);
        }
        else{
            AttachBitmap attachBitmap = new AttachBitmap();
            attachBitmap.execute(image[0]);
        }

        // set on click listener
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // length of string array to determine which activity to call
                if (image.length == 2) {
                    // open folder view
                    // intent to start another activity with a folder view
                    Intent intent = new Intent(mContext, FolderView.class);
                    intent.putExtra("iconPath", image[0]);
                    // put the folder name as well
                    intent.putExtra("folderName", image[1]);
                    mContext.startActivity(intent);
                } else if (image.length == 3) {
                    // todo check mode if selection mode run different code
                    if(selectionMode[0]){
                        Uri uri = Uri.fromFile(new File(((GridAdapter.ViewHolder) v.getTag()).imageView.getTag().toString()));
                        if(imageUris.contains(uri)){
                            imageUris.remove(uri);
                        }
                        else{
                            imageUris.add(uri);
                        }
                    }
                    else{
                        // open fullscreen view
                        // intent to start another activity with a larger view
                        Intent intent = new Intent(mContext, fullScreen.class);
                        intent.putExtra("path", image[0]);

                        // put the description as well
                        intent.putExtra("descr", image[1]);
                        // if recording there, put it too
                        intent.putExtra("recording", image[2]);
                        mContext.startActivity(intent);
                    }

                }
            }
        });

        if(image.length == 3){

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    // add to selected list
                    Uri uri = Uri.fromFile(new File(((GridAdapter.ViewHolder) v.getTag()).imageView.getTag().toString()));
                    // set a toggle for clicks
                    Log.i(TAG,"long clicked");
                    if(imageUris.contains(uri)){
                        imageUris.remove(uri);
                    }
                    else{
                        imageUris.add(uri);
                    }
                    // set selection mode
                    selectionMode[0] = true;

                    return true;
                }
            });
        }

        return view;

    }




    public Bitmap rotateImage(Bitmap bitmap, int i){
        Matrix m = new Matrix();
        m.postRotate(i);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    public Bitmap decodeSampledBitmapFromUri(String path, int reqWidth, int reqHeight) {

        Bitmap bm = null;
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        bm = BitmapFactory.decodeFile(path, options);

        return bm;
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height
                        / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }

        return inSampleSize;
    }



}