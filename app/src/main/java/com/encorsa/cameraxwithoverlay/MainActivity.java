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
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.ml.vision.FirebaseVision;
//import com.google.firebase.ml.vision.common.FirebaseVisionImage;
//import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
//import com.google.firebase.ml.vision.text.FirebaseVisionText;
//import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

public class MainActivity extends AppCompatActivity {
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"}; //, "android.permission.WRITE_EXTERNAL_STORAGE"
    TextureView textureView;
    private ImageView overlay;
    private Bitmap bmpOverlay;
    private Paint sRectPaint;
    private TextView textinfo;
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
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //Here you can get the size!
        bmpOverlay = Bitmap.createBitmap(textureView.getWidth(), textureView.getHeight(), Bitmap.Config.ARGB_8888);
        if(allPermissionsGranted()){
            //rotation = Display.getRotation();

            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {

        CameraX.unbindAll();

        Rational aspectRatio = new Rational (textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){

                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();

                    }
                });


        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).setLensFacing(CameraX.LensFacing.BACK).setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
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
        CameraX.bindToLifecycle((LifecycleOwner)this, preview, imgCap, imageAnalysis);
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = textureView.getDisplay().getRotation();

        switch(rotation){
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

        mx.postRotate(-(float)rotationDgr, cX, cY);
        textureView.setTransform(mx);

//        float scaledWidth, scaledHeight;
//        if (w > h) {
//            scaledHeight = w;
//            scaledWidth = w;
//        } else {
//            scaledHeight = h;
//            scaledWidth = w;
//        }
//        float xScale = scaledWidth / w;
//        float yScale = scaledHeight / h;
//        mx.preScale(xScale,yScale,cX,cY);
//        textureView.setTransform(mx);
//        textureView.getSurfaceTexture().setDefaultBufferSize(textureView.getWidth(),textureView.getHeight());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
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

                        Image mediaImage = image.getImage();




                        //TextRecognizer txtRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                        //int rotation = degreesToFirebaseRotation(rotationDegrees);
//                        FirebaseVisionImage image1 =
//                                FirebaseVisionImage.fromMediaImage(mediaImage, rotation);
//                        recognizeText(image1);
//                        Mat mat = new Mat();
//                        Utils.bitmapToMat(bitmap, mat);
//
//
//                        Imgproc.cvtColor(mat, mat, currentImageType);
//                        Utils.matToBitmap(mat, bitmap);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                ivBitmap.setImageBitmap(bitmap);
//                            }
//                        });

                    }
                });


        return imageAnalysis;

    }

//    private int degreesToFirebaseRotation(int degrees) {
//        switch (degrees) {
//            case 0:
//                return FirebaseVisionImageMetadata.ROTATION_0;
//            case 90:
//                return FirebaseVisionImageMetadata.ROTATION_90;
//            case 180:
//                return FirebaseVisionImageMetadata.ROTATION_180;
//            case 270:
//                return FirebaseVisionImageMetadata.ROTATION_270;
//            default:
//                throw new IllegalArgumentException(
//                        "Rotation must be 0, 90, 180, or 270.");
//        }
//    }
//
//    private void recognizeText(FirebaseVisionImage image) {
//
//
//        // [START get_detector_default]
//        Canvas canvas = new Canvas(bmpOverlay);
//
//        //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
//        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
//                .getOnDeviceTextRecognizer();
//        // [END get_detector_default]
//        float scaleX = overlay.getWidth() / image.getBitmap().getWidth();
//        float scaleY = overlay.getHeight() / image.getBitmap().getHeight();
//        Toast.makeText(this, "scaleX" + scaleX + " scaleY:" + scaleY, Toast.LENGTH_SHORT).show();
//
//        // [START run_detector]
//        Task<FirebaseVisionText> result =
//                detector.processImage(image)
//                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
//                            @Override
//                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
//                                // Task completed successfully
//                                // [START_EXCLUDE]
//                                // [START get_text]
//
//                                for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
//                                    Rect boundingBox = block.getBoundingBox();
//                                    RectF scaledBoundigBox = new RectF(boundingBox.left*scaleX, boundingBox.top*scaleY, boundingBox.right*scaleX, boundingBox.bottom*scaleY);
//                                    Point[] cornerPoints = block.getCornerPoints();
//                                    String text = block.getText();
//                                    canvas.drawRect(scaledBoundigBox, sRectPaint);
////                                    for (FirebaseVisionText.Line line: block.getLines()) {
////
////
////                                        //Toast.makeText(MainActivity.this, line.getText() + " - " + line.getBoundingBox().toString(), Toast.LENGTH_SHORT).show();line.getBoundingBox();
////
////
////                                        for (FirebaseVisionText.Element element: line.getElements()) {
////                                            // ...
////                                        }
////                                    }
//
//                                }
//                                // [END get_text]
//                                // [END_EXCLUDE]
////                                MainActivity.this.runOnUiThread(new Runnable() {
////                                    @Override
////                                    public void run() {
//                                        //Your code to run in GUI thread here
//                                        overlay.setImageBitmap(bmpOverlay);
//                                        overlay.postInvalidate();
////                                    }
////                                });
//                            }
//                        })
//                        .addOnFailureListener(
//                                new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        // Task failed with an exception
//                                        // ...
//                                    }
//                                });
//        // [END run_detector]
//    }
}
