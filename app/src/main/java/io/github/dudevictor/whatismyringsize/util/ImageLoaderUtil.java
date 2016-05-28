package io.github.dudevictor.whatismyringsize.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by victor on 26/05/16.
 */
public class ImageLoaderUtil {

    private ImageLoaderUtil() {

    }

    public static Bitmap getDownsampledBitmap(Uri uri, int targetWidth, int targetHeight,
                                       ContentResolver contentResolver) {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options outDimens = getBitmapDimensions(uri, contentResolver);

            int sampleSize = calculateSampleSize(outDimens.outHeight, outDimens.outWidth, targetWidth, targetHeight);

            bitmap = downsampleBitmap(uri, sampleSize, contentResolver);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotadedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap = rotadedBitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private static BitmapFactory.Options getBitmapDimensions(Uri uri, ContentResolver content)
            throws IOException {
        BitmapFactory.Options outDimens = new BitmapFactory.Options();
        outDimens.inJustDecodeBounds = true;

        InputStream is= content.openInputStream(uri);
        BitmapFactory.decodeStream(is, null, outDimens);
        is.close();

        return outDimens;
    }

    private static int calculateSampleSize(int width, int height, int targetWidth, int targetHeight) {
        int inSampleSize = 1;

        if (height > targetHeight || width > targetWidth) {

            final int heightRatio = Math.round((float) height
                    / (float) targetHeight);
            final int widthRatio = Math.round((float) width / (float) targetWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    private static Bitmap downsampleBitmap(Uri uri, int sampleSize, ContentResolver content)
            throws IOException {
        Bitmap resizedBitmap;
        BitmapFactory.Options outBitmap = new BitmapFactory.Options();
        outBitmap.inJustDecodeBounds = false;
        outBitmap.inSampleSize = sampleSize;

        InputStream is = content.openInputStream(uri);
        resizedBitmap = BitmapFactory.decodeStream(is, null, outBitmap);
        is.close();

        return resizedBitmap;
    }

}
