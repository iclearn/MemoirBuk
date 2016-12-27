package com.iclearn111gmail.MemoirBuk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    public ArrayList<String> imagePaths = new ArrayList<>();
    public ArrayList<String> recordingPaths = new ArrayList<>();
    public ArrayList<Uri> imageUris = new ArrayList<>();
    public ArrayList<String> imageNames = new ArrayList<>();

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
            view.setOrientation(LinearLayout.VERTICAL);
            view.setLayoutParams(new GridView.LayoutParams(GridView.AUTO_FIT, 300));
            view.setTag(vH);

        }
        else
        vH = (ViewHolder) view.getTag();

        vH.imageView.setTag(R.id.image,image[0]);
        if(image.length == 3)
        vH.imageView.setTag(R.id.record,image[2]); // holds the video path in case of videos
        vH.caption.setText(image[1]);
        vH.imageView.setTag(R.id.name, image[1]);

        class AttachBitmap extends AsyncTask<String, Void, Bitmap>{
            @Override
            protected Bitmap doInBackground(String ... p){
                Bitmap bitmap = decodeSampledBitmapFromUri(p[0], 300, 300);
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
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            bitmap = rotateImage(bitmap, 270);
                            break;
                        case ExifInterface.ORIENTATION_NORMAL:
                        default:

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
                if (image.length == 4) {
                    // intent to play the video
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.parse(image[0]);// check this if video does not play
                    intent.setDataAndType(uri, "video/*");
                    mContext.startActivity(intent);
                } else if (image.length == 2) {
                    if (image[1] == "Videos") {
                        Intent intent = new Intent(mContext, Videos.class);
                        intent.putExtra("iconPath", image[0]);
                        //intent.putExtra("", )
                        mContext.startActivity(intent);
                    }
                    // open folder view
                    // intent to start another activity with a folder view
                    else {
                        Intent intent = new Intent(mContext, FolderView.class);
                        intent.putExtra("iconPath", image[0]);
                        // put the folder name as well
                        intent.putExtra("folderName", image[1]);
                        mContext.startActivity(intent);

                    }
                } else if (image.length == 3) {
                    String path1 = ((GridAdapter.ViewHolder) v.getTag()).imageView.getTag(R.id.image).toString();
                    String path2 = ((GridAdapter.ViewHolder) v.getTag()).imageView.getTag(R.id.record).toString();
                    String name = ((GridAdapter.ViewHolder) v.getTag()).imageView.getTag(R.id.name).toString();
                    // todo check mode if selection mode run different code
                    if (selectionMode[0]) {
                        Uri uri = Uri.fromFile(new File(path1));
                        if (imageUris.contains(uri)) {
                            imageUris.remove(uri);
                            imagePaths.remove(path1);
                            recordingPaths.remove(path2);
                            imageNames.remove(name);
                            v.setBackgroundColor(Color.TRANSPARENT);
                        } else {
                            imageUris.add(uri);
                            imagePaths.add(path1);
                            recordingPaths.add(path2);
                            imageNames.add(name);
                            v.setBackgroundColor(Color.GRAY);
                        }
                    } else {
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
                    String path1 = ((GridAdapter.ViewHolder) v.getTag()).imageView.getTag(R.id.image).toString();
                    String path2 = ((GridAdapter.ViewHolder) v.getTag()).imageView.getTag(R.id.record).toString();
                    String name  = ((GridAdapter.ViewHolder) v.getTag()).imageView.getTag(R.id.name).toString();
                    // add to selected list
                    Uri uri = Uri.fromFile(new File(path1));
                    // set a toggle for clicks
                    Log.i(TAG,"long clicked");
                    if(imageUris.contains(uri)){
                        imageUris.remove(uri);
                        imagePaths.remove(path1);
                        recordingPaths.remove(path2);
                        imageNames.remove(name);
                        v.setBackgroundColor(Color.TRANSPARENT);
                    }
                    else{
                        imageUris.add(uri);
                        imagePaths.add(path1);
                        recordingPaths.add(path2);
                        imageNames.add(name);
                        v.setBackgroundColor(Color.GRAY);
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
            }
            else if(height > width){
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }

        return inSampleSize;
    }
}