package io.github.dudevictor.whatismyringsize.activity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageSize;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PictureShowActivity extends AppCompatActivity implements
        View.OnTouchListener {

    private ImageView imgView;
    private Mat mat;
    private ImageLoader imageLoader;
    private Bitmap originalPicture;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);


            Uri selectedImage = (Uri) getIntent().getExtras().get("imageUri");

            DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder().considerExifParams(true)
                    .cacheOnDisk(true)
                    .build();
            originalPicture = imageLoader.loadImageSync(selectedImage.toString(),
                    new ImageSize(imgView.getWidth(), imgView.getHeight()), displayImageOptions);
            imgView.setImageBitmap(originalPicture);
            imgView.setDrawingCacheEnabled(true);
            originalPicture = Bitmap.createBitmap(imgView.getDrawingCache());
            imgView.setDrawingCacheEnabled(false);
            Bitmap bmp32 = originalPicture.copy(Bitmap.Config.ARGB_8888, true);
            mat = new Mat();
            Utils.bitmapToMat(bmp32, mat);
            imgView.setOnTouchListener(PictureShowActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_show);
        imgView = (ImageView) findViewById(R.id.imgView);
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(getBaseContext()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mat.cols();
        int rows = mat.rows();

        int x = (int) event.getX();
        int y = (int) event.getY();

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x > 4) ? x - 4 : 0;
        touchedRect.y = (y > 4) ? y - 4 : 0;

        touchedRect.width = (x + 60 < cols) ? x + 60 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y + 60 < rows) ? y + 60 - touchedRect.y : rows - touchedRect.y;

        Mat hand = findHand(x, y);
        MatOfPoint contours = findContours(hand);
        MatOfInt hull = findConvexHull(contours);
        Point[] fingers = findFingers(contours, hull);

        Imgproc.drawContours(mat, Arrays.asList(contours), -1, new Scalar(0, 255, 0, 255));

        //Imgproc.circle(mat, centerHand, 5, new Scalar(255, 0, 255), 1, Imgproc.LINE_AA, 0);
        //Imgproc.circle(mat, centerHand, hand_radius, new Scalar(0, 0, 255), 1, Imgproc.LINE_AA, 0);

        for (int i = 0; i < num_fingers; i++) {
            if (fingers[i] != null) {
                Imgproc.circle(mat, fingers[i], 10,
                        new Scalar(255, 0, 0), 10, Imgproc.LINE_AA, 0);
            }
        }

       /* for (int i = 0; i < deffectsPoints.length ; i++) {
            Imgproc.circle(mat, deffectsPoints[i], 2,
                    new Scalar(200, 200, 200), 2, Imgproc.LINE_AA, 0);
        }*/



        Utils.matToBitmap(mat, originalPicture);

        imgView.setImageBitmap(originalPicture);
        return false;
    }

    private Point[] findFingers(MatOfPoint contour, MatOfInt hull) {
        Point fingers[] = new Point[6];

        if (contour != null && hull != null) {
            int n = (int) contour.total();
            double dist, dist1 = 0, dist2 = 0;
            double cx = centerHand.x;
            double cy = centerHand.y;

            Point maxPoint = new Point();
            Point pontos[] = contour.toArray();

            for (int i = 0; i < n; i++) {


                dist = (cx - pontos[i].x) * (cx - pontos[i].x) +
                        (cy - pontos[i].y) * (cy - pontos[i].y);

                if (dist < dist1 && dist1 > dist2 && maxPoint.x != 0
                        && maxPoint.y < originalPicture.getHeight() - 10) {

                    fingers[num_fingers++] = maxPoint;
                    if (num_fingers >= 6)
                        break;
                }

                dist2 = dist1;
                dist1 = dist;
                maxPoint = pontos[i];
            }

        }

        return fingers;

    }

    public Point centerHand;
    public Integer hand_radius = 0;
    public Integer num_fingers = 5;
    public Point deffectsPoints[] = new Point[8];

    private MatOfInt findConvexHull(MatOfPoint contour) {
        if (contour == null) {
            return null;
        }

        int x = 0, y = 0;
        int dist = 0;

        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(contour, hull, true);

        if (hull != null) {
            MatOfInt4 defects = new MatOfInt4();
            Imgproc.convexityDefects(contour, hull, defects);
            int[] defects_array = defects.toArray();
            if (defects.total() != 0L) {
                for (int i = 0; i < defects.total() && i < 8; i++) {
                    x += defects_array[i*4];
                    y += defects_array[i*4 + 1];

                    deffectsPoints[i] = new Point(x, y);
                }

                x /= defects.total();
                y /= defects.total();
                centerHand = new Point(x, y);
                for (int i = 0; i < defects.total(); i++) {
                    int d = (x - defects_array[i*4]) *
                    (x - defects_array[i]) +
                            (y - defects_array[i*4+1]) *
                    (y - defects_array[i+1]);

                    dist += Math.sqrt(d);
                }
                hand_radius = (int) (dist / defects.total());
            }

        }

        return hull;
    }

    private MatOfPoint findContours(Mat hand) {
        // Find max contour area
        double maxArea = 0;
        List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint maxContour = null;
        Imgproc.findContours(hand, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
            maxContour = wrapper;
        }

        return maxContour;
    }

    private Mat findHand(int x, int y) {
        Mat hand = new Mat();

        mat.copyTo(hand);

        Imgproc.GaussianBlur(hand, hand, new Size(5,5), 0, 0);
        Imgproc.medianBlur(hand, hand, 9);

        Imgproc.cvtColor(hand, hand, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(hand, new Scalar(0, 48, 80), new Scalar(20, 255, 255), hand);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(4, 4));
        Imgproc.morphologyEx(hand, hand, Imgproc.MORPH_OPEN, kernel);

        Imgproc.floodFill(hand, new Mat(), new Point(x, y), new Scalar(127, 127, 127));
        Imgproc.floodFill(hand, new Mat(), new Point(0, 0), new Scalar(255, 255, 255));
        Core.bitwise_not(hand, hand);
        Imgproc.floodFill(hand, new Mat(), new Point(x, y), new Scalar(255, 255, 255));

        Imgproc.morphologyEx(hand, hand, Imgproc.MORPH_OPEN, kernel);
        Imgproc.medianBlur(hand, hand, 3);
        return hand;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
