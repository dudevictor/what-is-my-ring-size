package io.github.dudevictor.whatismyringsize.activity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import io.github.dudevictor.whatismyringsize.util.ImageLoaderUtil;

public class PictureShowActivity extends AppCompatActivity implements
        View.OnTouchListener{

    private ImageView imgView;
    private Mat originalImage;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);

            Uri selectedImage = (Uri) getIntent().getExtras().get("imageUri");
            Bitmap bitmap = ImageLoaderUtil.getDownsampledBitmap(selectedImage,
                    imgView.getWidth(), imgView.getHeight(), getContentResolver());
            imgView.setImageBitmap(bitmap);


            Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            originalImage = new Mat();
            Utils.bitmapToMat(bmp32, originalImage);
            imgView.setOnTouchListener(PictureShowActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_show);
        imgView = (ImageView) findViewById(R.id.imgView);

    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0,this, mLoaderCallback);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = originalImage.cols();
        int rows = originalImage.rows();

        int x = (int)event.getX();
        int y = (int)event.getY();

        Toast.makeText(PictureShowActivity.this, "Touch image coordinates: (" + x + ", " + y + ")",
                Toast.LENGTH_SHORT).show();

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = originalImage.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        Scalar mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        Scalar mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Toast.makeText(PictureShowActivity.this, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")", Toast.LENGTH_SHORT).show();

        return false;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
