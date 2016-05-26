package io.github.dudevictor.whatismyringsize.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


public class CameraActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        BaseLoaderCallback mCallBack = new BaseLoaderCallback(CameraActivity.this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case BaseLoaderCallback.SUCCESS: {
                        Log.i("Teste", "OpenCV loaded successfully");
                        mOpenCvCameraView.enableView();
                        break;
                    }
                    default: {
                        super.onManagerConnected(status);
                        break;
                    }
                }
            }
        };
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, CameraActivity.this, mCallBack);

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat src = inputFrame.rgba();
        image = src;
        Mat mRgbaT = src.t();
        Core.flip(src.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, src.size());
        src = mRgbaT;

        /*Imgproc.blur( src, src, new Size(3,3));
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2HSV);
        Core.inRange(src, new Scalar(0, 30, 60), new Scalar(20, 150, 255), src);
        Imgproc.threshold(src, src, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

        Mat cannyEdges = new Mat();

        Imgproc.Canny(src, src,10, 100);*/

        /*List<MatOfPoint> matOfPoints = new ArrayList<>();
        Imgproc.findContours(cannyEdges, matOfPoints,  new Mat(),
                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        List<MatOfInt> hull = new ArrayList<>();


        for (MatOfPoint mop : matOfPoints) {
            MatOfInt matOfInt = new MatOfInt();
            Imgproc.convexHull(mop, matOfInt, false);
            hull.add(matOfInt);
        }

        List<Point[]> hullpoints = new ArrayList<>();
        for(int i=0; i < hull.size(); i++){
            Point[] points = new Point[hull.get(i).rows()];

            // Loop over all points that need to be hulled in current contour
            for(int j=0; j < hull.get(i).rows(); j++){
                int index = (int)hull.get(i).get(j, 0)[0];
                points[j] = new Point(matOfPoints.get(i).get(index, 0)[0], matOfPoints.get(i).get(index, 0)[1]);
            }

            hullpoints.add(points);
        }

        // Convert Point arrays into MatOfPoint
        List<MatOfPoint> hullmop = new ArrayList<MatOfPoint>();
        for(int i=0; i < hullpoints.size(); i++){
            MatOfPoint mop = new MatOfPoint();
            mop.fromArray(hullpoints.get(i));
            hullmop.add(mop);
        }


        // Draw contours + hull result
        Scalar color = new Scalar(0, 255, 0);   // Green
        for(int i=0; i < matOfPoints.size(); i++){
            Imgproc.drawContours(src, matOfPoints, i, color);
            Imgproc.drawContours(src, hullmop, i, color);
        }*/

        return src;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /*@Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }*/
}
