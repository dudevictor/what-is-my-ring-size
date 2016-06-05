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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import io.github.dudevictor.whatismyringsize.util.OpencvTools;

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
        findFingers(contours, hull);

        Imgproc.drawContours(mat, Arrays.asList(contours), -1, new Scalar(0, 255, 0, 255));
        //OpencvTools.drawDefects(mat, deffectsPoints, centerHand);
        Imgproc.circle(mat, centerHand, 10, new Scalar(255, 0, 0, 255), -1);

        for (Point dedo : maxPointFingers) {

            if (dedo == null) continue;
            Imgproc.circle(mat, dedo, 10, new Scalar(0, 0, 255, 255), -1);
        }

        for (Point dedo : minPointFingers) {

            if (dedo == null) continue;
            Imgproc.circle(mat, dedo, 10, new Scalar(255, 0, 255, 255), -1);
        }

        Utils.matToBitmap(mat, originalPicture);

        imgView.setImageBitmap(originalPicture);
        return false;
    }

    private void findFingers(MatOfPoint contour, MatOfInt hull) {
        List<Point> localMaxPointFingers = new ArrayList<>();
        minPointFingers = new ArrayList<>();

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

                    localMaxPointFingers.add(new Point(maxPoint.x, maxPoint.y));

                    if (localMaxPointFingers.size() >= 10)
                        break;
                }

                if (dist > dist1 && dist1 < dist2 && maxPoint.x != 0) {

                    minPointFingers.add(new Point(maxPoint.x, maxPoint.y));
                    if (minPointFingers.size() >= 10)
                        break;
                }

                dist2 = dist1;
                dist1 = dist;
                maxPoint = pontos[i];
            }

            PriorityQueue<Point> dedos = new PriorityQueue<>(5, new Comparator<Point>() {
                @Override
                public int compare(Point lhs, Point rhs) {
                    Double minLhs1 = Double.MAX_VALUE, minLhs2 = Double.MAX_VALUE,
                            minRhs1 = Double.MAX_VALUE, minRhs2 = Double.MAX_VALUE;

                    for (Point min : minPointFingers) {
                        double min1 = OpencvTools.getDistanceBetweenPoints(lhs, min);
                        double min2 = OpencvTools.getDistanceBetweenPoints(rhs, min);

                        if (min1 < minLhs1) {
                            minLhs1 = min1;
                        } else if (min1 < minLhs2) {
                            minLhs2 = min1;
                        }

                        if (min2 < minRhs1) {
                            minRhs1 = min2;
                        } else if (min2 < minRhs2) {
                            minRhs2 = min2;
                        }

                    }

                    if (minLhs1 + minLhs2 - minRhs1 - minRhs2 > 0) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });

            dedos.addAll(localMaxPointFingers);
            maxPointFingers = new ArrayList<>();
            int size = dedos.size();
            for (int i = 0; i < 5 && i < size; i++) {
                maxPointFingers.add(dedos.poll());
            }

        }

    }

    public Point centerHand;
    public List<Point[]> deffectsPoints;
    public List<Point> maxPointFingers;
    public List<Point>  minPointFingers;

    private MatOfInt findConvexHull(MatOfPoint contour) {
        if (contour == null) {
            return null;
        }

        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(contour, hull, true);
        centerHand = OpencvTools.getCentroid(contour);

        if (hull != null) {
            MatOfInt4 defects = new MatOfInt4();
            Imgproc.convexityDefects(contour, hull, defects);

            deffectsPoints = filterDefects(defects, contour);

        }

        return hull;
    }

    private List<Point[]> filterDefects(MatOfInt4 convexityDefects, MatOfPoint handContour){
        List<Point[]> finalDefects = new ArrayList<Point[]>();

        int defects[] = convexityDefects.toArray();
        List<Point> contour = handContour.toList();
        Point centroid = OpencvTools.getCentroid(handContour);
		/*
		 * convexityDefects -> structure containing (by order) start, end, depth_point, depth.
		 */
        Point start;
        //		Point end;
        Point farthest;
        //		float depth;
        for (int i = 0; i < defects.length; i = i + 4) {
            start = contour.get(defects[i]);
            farthest = contour.get(defects[i + 2]);
            Point[] points = new Point[2];
            points[0] = start;
            points[1] = farthest;
            finalDefects.add(points);
        }
        return finalDefects;
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
            double area = Math.abs(Imgproc.contourArea(wrapper));
            if (area > maxArea) {
                maxArea = area;
                maxContour = wrapper;
            }
        }

        if (maxContour != null) {
            MatOfPoint2f maxContour2f = new MatOfPoint2f();
            MatOfPoint2f approxPoly = new MatOfPoint2f();
            maxContour.convertTo(maxContour2f, CvType.CV_32FC2);

            Imgproc.approxPolyDP(maxContour2f, approxPoly, 2, true);
            approxPoly.convertTo(maxContour, CvType.CV_32S);

        }

        return maxContour;
    }

    private Mat findHand(int x, int y) {
        Mat hand = new Mat();
        mat.copyTo(hand);
        Imgproc.cvtColor(hand, hand, Imgproc.COLOR_RGB2HSV_FULL);
        Imgproc.GaussianBlur(hand, hand, new Size(3,3), 0, 0);

        Mat frame_gray = new Mat();
        Core.inRange(hand, new Scalar(0, 48, 80), new Scalar(20, 255, 255), frame_gray);

        Imgproc.threshold(frame_gray, frame_gray, 60, 255, Imgproc.THRESH_BINARY);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(frame_gray, frame_gray, Imgproc.MORPH_ERODE,kernel);
        kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(7, 7));
        Imgproc.morphologyEx(frame_gray, frame_gray, Imgproc.MORPH_OPEN,kernel);
        kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(7, 7));
        Imgproc.morphologyEx(frame_gray, frame_gray, Imgproc.MORPH_CLOSE,kernel);
        Imgproc.medianBlur(frame_gray, frame_gray, 15);

        Imgproc.floodFill(frame_gray, new Mat(), new Point(x, y), new Scalar(127, 127, 127));
        Imgproc.floodFill(frame_gray, new Mat(), new Point(0, 0), new Scalar(255, 255, 255));
        Core.bitwise_not(frame_gray, frame_gray);
        Imgproc.floodFill(frame_gray, new Mat(), new Point(x, y), new Scalar(255, 255, 255));

        return frame_gray;
    }
}
