package com.encorsa.cameraxwithoverlay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"}; //, "android.permission.WRITE_EXTERNAL_STORAGE"
    TextureView textureView;
    private ImageView overlay;
    private Bitmap bmpOverlay;
    private Paint sRectPaint;
    private Paint sTextPaint;
    private TextView textinfo;
    private long lastAnalyzedTimestamp = 0L;
    private TextRecognizer textRecognizer;
    private Rational screenAspectRatio;
    private Size screenSize;
    private Canvas canvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.view_finder);
        overlay = (ImageView) findViewById(R.id.imageView);
//        textinfo = (TextView) findViewById(R.id.infoTextView);

        sRectPaint = new Paint();
        sRectPaint.setColor(Color.WHITE);
        sRectPaint.setStyle(Paint.Style.STROKE);
        sRectPaint.setStrokeWidth(4.0f);

        sTextPaint = new Paint();
        sTextPaint.setColor(Color.WHITE);
        sTextPaint.setTextSize(30.0f);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //Here you can get the size!


        if (allPermissionsGranted()) {
            //rotation = Display.getRotation();

            startCamera(); //start camera if permission has been granted by user
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {

        CameraX.unbindAll();
        textRecognizer = new TextRecognizer.Builder(this).build();
        screenAspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        screenSize = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        PreviewConfig pConfig = new PreviewConfig.Builder()
                //.setTargetAspectRatio(screenAspectRatio)
                // .setTargetResolution(screenSize)
                .build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {

                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();

                    }
                });


        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.BACK).setTargetAspectRatio(screenAspectRatio).setTargetResolution(screenSize).build();
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);
        // findViewById(R.id.imgCapture)

        ImageAnalysis imageAnalysis = setImageAnalysis();

        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                File file = null;
//                Helper.deleteCache(getApplicationContext());
//                try {
//                    File outputDir = getCacheDir(); // context being the Activity pointer
//                    file = File.createTempFile("CAMERA_TEMP", ".JPG", outputDir);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

//                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
//                    @Override
//                    public void onImageSaved(@NonNull File file) {
//                        Intent i = new Intent(ScanLabelActivity.this, ImageActivity.class);
//                        i.putExtra("absolutePath", file.getAbsolutePath());
//                        startActivity(i);
//                    }
//
//
//                    @Override
//                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
//                        String msg = "Pic capture failed : " + message;
//                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
//                        if(cause != null){
//                            cause.printStackTrace();
//                        }
//                    }
//                });
            }
        });


        //bind to lifecycle:
        CameraX.bindToLifecycle((LifecycleOwner) this, preview, imgCap, imageAnalysis);
    }

    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = textureView.getDisplay().getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate(-(float) rotationDgr, cX, cY);
        textureView.setTransform(mx);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private ImageAnalysis setImageAnalysis() {

        // Setup image analysis pipeline that computes average pixel luminance
        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();


        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
//                .setMaxResolution(new Size(810, 1080))
                //.setTargetAspectRatio(screenAspectRatio)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
                .setImageQueueDepth(1).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        //Analyzing live camera feed begins.
                        if (image == null || image.getImage() == null) {
                            return;
                        }

                        long currentTimestamp = System.currentTimeMillis();
                        if ((currentTimestamp - lastAnalyzedTimestamp) >= TimeUnit.SECONDS.toMillis(1)) {
                            if (textRecognizer.isOperational()) {

                                Frame frame = new Frame.Builder().setBitmap(textureView.getBitmap()).build();//.setImageData(image.getPlane  .getPlanes().[0].getBuffer(), image.getWidth(), image.getHeight(), image.getFormat());
                                SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

                                bmpOverlay = Bitmap.createBitmap(overlay.getWidth(), overlay.getHeight(), Bitmap.Config.ARGB_8888);
                                canvas = new Canvas(bmpOverlay);
                                for (int index = 0; index < textBlocks.size(); index++) {
                                    //extract scanned text blocks here
                                    TextBlock tBlock = textBlocks.valueAt(index);
                                    Rect boundingBox = textBlocks.valueAt(index).getBoundingBox();
                                    Log.i("MainActivity", "" + boundingBox.toString() + tBlock.getValue());
                                    // .blocks = blocks + tBlock.getValue() + "\n" + "\n";

                                    canvas.drawRect(boundingBox, sRectPaint);

                                    for (Text line : tBlock.getComponents()) {

                                        float left = line.getBoundingBox().left;
                                        float bottom = line.getBoundingBox().bottom;
                                        canvas.drawText(line.getValue(), left, bottom, sTextPaint);
                                    }
                                }
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //Your code to run in GUI thread here
                                        overlay.setImageBitmap(bmpOverlay);
                                        overlay.postInvalidate();
                                    }
                                });
                            }
                            lastAnalyzedTimestamp = currentTimestamp;
                        }
                    }
                });

        return imageAnalysis;

    }
}
