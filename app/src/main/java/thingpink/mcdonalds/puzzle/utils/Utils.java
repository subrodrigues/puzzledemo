package thingpink.mcdonalds.puzzle.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;

import thingpink.mcdonalds.puzzle.Globals;
import thingpink.mcdonalds.puzzle.R;
import thingpink.mcdonalds.puzzle.views.PuzzleView;

/**
 * Created by Filipe Rodrigues on 29/11/2016.
 */

public class Utils {
    public static float getImageAspectRatio(PuzzleView v) {
        Bitmap bitmap = v.getBitmap();

        if (bitmap == null) {
            return 1;
        }

        float width = bitmap.getWidth();
        float height = bitmap.getHeight();

        return width / height;
    }

    public static File getSaveDirectory() {
        File root = new File(Environment.getExternalStorageDirectory().getPath());
        File dir = new File(root, Globals.FILENAME_PHOTO_DIR);

        if (!dir.exists()) {
            if (!root.exists() || !dir.mkdirs()) {
                return null;
            }
        }

        return dir;
    }

    public static Bitmap loadBitmap(Context context, Uri uri, int targetWidth, int targetHeight) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            InputStream imageStream = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(imageStream, null, o);

            if (o.outWidth > o.outHeight && targetWidth < targetHeight) {
                int i = targetWidth;
                targetWidth = targetHeight;
                targetHeight = i;
            }

            if (targetWidth < o.outWidth || targetHeight < o.outHeight) {
                double widthRatio = (double) targetWidth / (double) o.outWidth;
                double heightRatio = (double) targetHeight / (double) o.outHeight;
                double ratio = Math.max(widthRatio, heightRatio);

                o.inSampleSize = (int) Math.pow(2, (int) Math.round(Math.log(ratio) / Math.log(0.5)));
            } else {
                o.inSampleSize = 1;
            }

            o.inScaled = false;
            o.inJustDecodeBounds = false;

            imageStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream, null, o);

            int rotate = 0;

            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        rotate = cursor.getInt(0);

                        if (rotate == -1) {
                            rotate = 0;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            if (rotate != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            return bitmap;
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    public static void playSound(Context context, int soundId) {
        MediaPlayer player = MediaPlayer.create(context, soundId);
        player.start();
    }

    public static String sizeToString(Context context, int width, int height) {
        return MessageFormat.format(context.getString(R.string.puzzle_size_x_y), width, height);
    }

    public static SharedPreferences getPreferences(Context context, String key) {
        return context.getSharedPreferences(key, Activity.MODE_PRIVATE);
    }
}
