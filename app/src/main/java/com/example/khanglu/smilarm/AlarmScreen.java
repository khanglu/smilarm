package com.example.khanglu.smilarm;

/**
 * Created by KhangLu on 18/05/15.
 */
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.EnumSet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.qualcomm.snapdragon.sdk.face.FaceData;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.FP_MODES;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.PREVIEW_ROTATION_ANGLE;


public class AlarmScreen extends Activity implements Camera.PreviewCallback {

    public final String TAG = this.getClass().getSimpleName();

    private WakeLock mWakeLock;
    private MediaPlayer mPlayer;

    private static final int WAKELOCK_TIMEOUT = 60 * 1000;




    // ADDED CODE
    // Global Variables Required

    Camera cameraObj;
    FrameLayout preview;
    FacialProcessing faceProc;
    FaceData[] faceArray = null;// Array in which all the face data values will be returned for each face detected.
    View myView;
    Canvas canvas = new Canvas();
    Paint rectBrush = new Paint();
    private CameraSurfacePreview mPreview;
    private DrawView drawView;
    private final int FRONT_CAMERA_INDEX = 1;
    private final int BACK_CAMERA_INDEX = 0;

    // boolean clicked = false;
    boolean fpFeatureSupported = false;
    boolean cameraPause = false;        // Boolean to check if the "pause" button is pressed or no.
    static boolean cameraSwitch = false;    // Boolean to check if the camera is switched to back camera or no.
    boolean info = false;       // Boolean to check if the face data info is displayed or no.
    boolean landScapeMode = false;      // Boolean to check if the phone orientation is in landscape mode or portrait mode.

    int cameraIndex;// Integer to keep track of which camera is open.
    int smileValue = 0;
    int leftEyeBlink = 0;
    int rightEyeBlink = 0;
    int faceRollValue = 0;
    int pitch = 0;
    int yaw = 0;
    int horizontalGaze = 0;
    int verticalGaze = 0;
    PointF gazePointValue = null;
    //private final String TAG = "AlarmScreen";

    // TextView Variables
    TextView numFaceText, smileValueText, leftBlinkText, rightBlinkText, gazePointText, faceRollText, faceYawText,
            facePitchText, horizontalGazeText, verticalGazeText;

    int surfaceWidth = 0;
    int surfaceHeight = 0;

    OrientationEventListener orientationEventListener;
    int deviceOrientation;
    int presentOrientation;
    float rounded;
    Display display;
    int displayAngle;

    // END OF ADDED CODE







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent home button from stopping the alarm
        startLockTask(); // Android L+ only

        //Setup layout
        this.setContentView(R.layout.activity_alarm_screen);

        String name = getIntent().getStringExtra(AlarmManagerHelper.NAME);
        int timeHour = getIntent().getIntExtra(AlarmManagerHelper.TIME_HOUR, 0);
        int timeMinute = getIntent().getIntExtra(AlarmManagerHelper.TIME_MINUTE, 0);
        String tone = getIntent().getStringExtra(AlarmManagerHelper.TONE);

        TextView tvName = (TextView) findViewById(R.id.alarm_screen_name);
        tvName.setText(name);

        TextView tvTime = (TextView) findViewById(R.id.alarm_screen_time);
        tvTime.setText(String.format("%02d : %02d", timeHour, timeMinute));

        /*Button dismissButton = (Button) findViewById(R.id.alarm_screen_button);
        dismissButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                mPlayer.stop();
                finish();
            }
        });*/

        //Play alarm tone
        mPlayer = new MediaPlayer();
        try {
            if (tone != null && !tone.equals("")) {
                Uri toneUri = Uri.parse(tone);
                if (toneUri != null) {
                    mPlayer.setDataSource(this, toneUri);
                    mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mPlayer.setLooping(true);
                    mPlayer.prepare();
                    mPlayer.start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Ensure wakelock release
        Runnable releaseWakelock = new Runnable() {

            @Override
            public void run() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

                if (mWakeLock != null && mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
            }
        };

        new Handler().postDelayed(releaseWakelock, WAKELOCK_TIMEOUT);






        // ADDED CODE
        myView = new View(AlarmScreen.this);
        // Create our Preview view and set it as the content of our activity.
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        numFaceText = (TextView) findViewById(R.id.numFaces);
        smileValueText = (TextView) findViewById(R.id.smileValue);
        rightBlinkText = (TextView) findViewById(R.id.rightEyeBlink);
        leftBlinkText = (TextView) findViewById(R.id.leftEyeBlink);
        faceRollText = (TextView) findViewById(R.id.faceRoll);
        gazePointText = (TextView) findViewById(R.id.gazePoint);
        faceYawText = (TextView) findViewById(R.id.faceYawValue);
        facePitchText = (TextView) findViewById(R.id.facePitchValue);
        horizontalGazeText = (TextView) findViewById(R.id.horizontalGazeAngle);
        verticalGazeText = (TextView) findViewById(R.id.verticalGazeAngle);

        // Check to see if the FacialProc feature is supported in the device or no.
        fpFeatureSupported = FacialProcessing
                .isFeatureSupported(FacialProcessing.FEATURE_LIST.FEATURE_FACIAL_PROCESSING);

        if (fpFeatureSupported && faceProc == null) {
            Log.e("TAG", "Feature is supported");
            faceProc = FacialProcessing.getInstance();  // Calling the Facial Processing Constructor.
            faceProc.setProcessingMode(FP_MODES.FP_MODE_VIDEO);
        } else {
            Log.e("TAG", "Feature is NOT supported");
            return;
        }

        cameraIndex = Camera.getNumberOfCameras() - 1;// Start with front Camera

        try {
            cameraObj = Camera.open(cameraIndex); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.d("TAG", "Camera Does Not exist");// Camera is not available (in use or does not exist)
        }

        // Change the sizes according to phone's compatibility.
        mPreview = new CameraSurfacePreview(AlarmScreen.this, cameraObj, faceProc);
        preview.removeView(mPreview);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        cameraObj.setPreviewCallback(AlarmScreen.this);

        // Action listener for the screen touch to display the face data info.
        touchScreenListener();

        // Action listener for the Pause Button.
        pauseActionListener();

        // Action listener for the Switch Camera Button.
        cameraSwitchActionListener();

        orientationListener();

        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();


        // END OF ADDED CODE




    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();

        // Set the window to keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Acquire wakelock
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        if (mWakeLock == null) {
            mWakeLock = pm.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), TAG);
        }

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            Log.i(TAG, "Wakelock aquired!!");
        }

        // ADDED CODE
        if (cameraObj != null) {
            stopCamera();
        }

        if (!cameraSwitch)
            startCamera(FRONT_CAMERA_INDEX);
        else
            startCamera(BACK_CAMERA_INDEX);
        // END OF ADDED CODE
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        // ADDED CODE
        stopCamera();
    }




    // ADDED CODE
    FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {

        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            Log.e(TAG, "Faces Detected through FaceDetectionListener = " + faces.length);
        }
    };

    private void orientationListener() {
        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                deviceOrientation = orientation;
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }

        presentOrientation = 90 * (deviceOrientation / 360) % 360;
    }

    /*
     * Function for the screen touch action listener. On touching the screen, the face data info will be displayed.
     */
    private void touchScreenListener() {
        preview.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        if (!info) {
                            LayoutParams layoutParams = preview.getLayoutParams();

                            if (AlarmScreen.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                int oldHeight = preview.getHeight();
                                layoutParams.height = oldHeight * 3 / 4;
                            } else {
                                int oldHeight = preview.getHeight();
                                layoutParams.height = oldHeight * 80 / 100;
                            }
                            preview.setLayoutParams(layoutParams);// Setting the changed parameters for the layout.
                            info = true;
                        } else {
                            LayoutParams layoutParams = preview.getLayoutParams();
                            layoutParams.height = LayoutParams.WRAP_CONTENT;
                            preview.setLayoutParams(layoutParams);// Setting the changed parameters for the layout.
                            info = false;
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:
                        break;

                    case MotionEvent.ACTION_UP:
                        break;
                }

                return true;
            }
        });

    }

    /*
     * Function for switch camera action listener. Switches camera from front to back and vice versa.
     */
    private void cameraSwitchActionListener() {
        ImageView switchButton = (ImageView) findViewById(R.id.switchCameraButton);

        switchButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (!cameraSwitch)// If the camera is facing front then do this
                {
                    stopCamera();
                    cameraObj = Camera.open(BACK_CAMERA_INDEX);
                    mPreview = new CameraSurfacePreview(AlarmScreen.this, cameraObj, faceProc);
                    preview = (FrameLayout) findViewById(R.id.camera_preview);
                    preview.addView(mPreview);
                    cameraSwitch = true;
                    cameraObj.setPreviewCallback(AlarmScreen.this);
                } else						// If the camera is facing back then do this.
                {
                    stopCamera();
                    cameraObj = Camera.open(FRONT_CAMERA_INDEX);
                    preview.removeView(mPreview);
                    mPreview = new CameraSurfacePreview(AlarmScreen.this, cameraObj, faceProc);
                    preview = (FrameLayout) findViewById(R.id.camera_preview);
                    preview.addView(mPreview);
                    cameraSwitch = false;
                    cameraObj.setPreviewCallback(AlarmScreen.this);
                }

            }

        });
    }

    /*
     * Function for pause button action listener to pause and resume the preview.
     */
    private void pauseActionListener() {
        ImageView pause = (ImageView) findViewById(R.id.pauseButton);
        pause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (!cameraPause) {
                    cameraObj.stopPreview();
                    cameraPause = true;
                } else {
                    cameraObj.startPreview();
                    cameraObj.setPreviewCallback(AlarmScreen.this);
                    cameraPause = false;
                }

            }
        });
    }

    /*
     * This function will update the TextViews with the new values that come in.
     */

    public void setUI(int numFaces, int smileValue, int leftEyeBlink, int rightEyeBlink, int faceRollValue,
                      int faceYawValue, int facePitchValue, PointF gazePointValue, int horizontalGazeAngle, int verticalGazeAngle) {

        numFaceText.setText("Number of Faces: " + numFaces);
        smileValueText.setText("Smile Value: " + smileValue);
        leftBlinkText.setText("Left Eye Blink Value: " + leftEyeBlink);
        rightBlinkText.setText("Right Eye Blink Value " + rightEyeBlink);
        faceRollText.setText("Face Roll Value: " + faceRollValue);
        faceYawText.setText("Face Yaw Value: " + faceYawValue);
        facePitchText.setText("Face Pitch Value: " + facePitchValue);
        horizontalGazeText.setText("Horizontal Gaze: " + horizontalGazeAngle);
        verticalGazeText.setText("VerticalGaze: " + verticalGazeAngle);

        if (gazePointValue != null) {
            double x = Math.round(gazePointValue.x * 100.0) / 100.0;// Rounding the gaze point value.
            double y = Math.round(gazePointValue.y * 100.0) / 100.0;
            gazePointText.setText("Gaze Point: (" + x + "," + y + ")");
        } else {
            gazePointText.setText("Gaze Point: ( , )");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
     * This is a function to stop the camera preview. Release the appropriate objects for later use.
     */
    public void stopCamera() {
        if (cameraObj != null) {
            cameraObj.stopPreview();
            cameraObj.setPreviewCallback(null);
            preview.removeView(mPreview);
            cameraObj.release();
            faceProc.release();
            faceProc = null;
        }

        cameraObj = null;
    }

    /*
     * This is a function to start the camera preview. Call the appropriate constructors and objects.
     * @param-cameraIndex: Will specify which camera (front/back) to start.
     */
    public void startCamera(int cameraIndex) {

        if (fpFeatureSupported && faceProc == null) {

            Log.e("TAG", "Feature is supported");
            faceProc = FacialProcessing.getInstance();// Calling the Facial Processing Constructor.
        }

        try {
            cameraObj = Camera.open(cameraIndex);// attempt to get a Camera instance
        } catch (Exception e) {
            Log.d("TAG", "Camera Does Not exist");// Camera is not available (in use or does not exist)
        }

        mPreview = new CameraSurfacePreview(AlarmScreen.this, cameraObj, faceProc);
        preview.removeView(mPreview);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        cameraObj.setPreviewCallback(AlarmScreen.this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.camera_preview, menu);
        return true;
    }

    /*
     * Detecting the face according to the new Snapdragon SDK. Face detection will now take place in this function.
     * 1) Set the Frame
     * 2) Detect the Number of faces.
     * 3) If(numFaces > 0) then do the necessary processing.
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera arg1) {

        presentOrientation = (90 * Math.round(deviceOrientation / 90)) % 360;
        int dRotation = display.getRotation();
        PREVIEW_ROTATION_ANGLE angleEnum = PREVIEW_ROTATION_ANGLE.ROT_0;

        switch (dRotation) {
            case 0:
                displayAngle = 90;
                angleEnum = PREVIEW_ROTATION_ANGLE.ROT_90;
                break;

            case 1:
                displayAngle = 0;
                angleEnum = PREVIEW_ROTATION_ANGLE.ROT_0;
                break;

            case 2:
                // This case is never reached.
                break;

            case 3:
                displayAngle = 180;
                angleEnum = PREVIEW_ROTATION_ANGLE.ROT_180;
                break;
        }

        if (faceProc == null) {
            faceProc = FacialProcessing.getInstance();
        }

        Parameters params = cameraObj.getParameters();
        Size previewSize = params.getPreviewSize();
        surfaceWidth = mPreview.getWidth();
        surfaceHeight = mPreview.getHeight();

        // Landscape mode - front camera
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && !cameraSwitch) {
            faceProc.setFrame(data, previewSize.width, previewSize.height, true, angleEnum);
            cameraObj.setDisplayOrientation(displayAngle);
            landScapeMode = true;
        }
        // landscape mode - back camera
        else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && cameraSwitch) {
            faceProc.setFrame(data, previewSize.width, previewSize.height, false, angleEnum);
            cameraObj.setDisplayOrientation(displayAngle);
            landScapeMode = true;
        }
        // Portrait mode - front camera
        else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                && !cameraSwitch) {
            faceProc.setFrame(data, previewSize.width, previewSize.height, true, angleEnum);
            cameraObj.setDisplayOrientation(displayAngle);
            landScapeMode = false;
        }
        // Portrait mode - back camera
        else {
            faceProc.setFrame(data, previewSize.width, previewSize.height, false, angleEnum);
            cameraObj.setDisplayOrientation(displayAngle);
            landScapeMode = false;
        }

        int numFaces = faceProc.getNumFaces();

        if (numFaces == 0) {
            Log.d("TAG", "No Face Detected");
            if (drawView != null) {
                preview.removeView(drawView);

                drawView = new DrawView(this, null, false, 0, 0, null, landScapeMode);
                preview.addView(drawView);
            }
            canvas.drawColor(0, Mode.CLEAR);
            setUI(0, 0, 0, 0, 0, 0, 0, null, 0, 0);
        } else {

            Log.d("TAG", "Face Detected");
            faceArray = faceProc.getFaceData(EnumSet.of(FacialProcessing.FP_DATA.FACE_RECT,
                    FacialProcessing.FP_DATA.FACE_COORDINATES, FacialProcessing.FP_DATA.FACE_CONTOUR,
                    FacialProcessing.FP_DATA.FACE_SMILE, FacialProcessing.FP_DATA.FACE_ORIENTATION,
                    FacialProcessing.FP_DATA.FACE_BLINK, FacialProcessing.FP_DATA.FACE_GAZE));
            // faceArray = faceProc.getFaceData(); // Calling getFaceData() alone will give you all facial data except the
            // face
            // contour. Face Contour might be a heavy operation, it is recommended that you use it only when you need it.
            if (faceArray == null) {
                Log.e("TAG", "Face array is null");
            } else {
                if (faceArray[0].leftEyeObj == null) {
                    Log.e(TAG, "Eye Object NULL");
                } else {
                    Log.e(TAG, "Eye Object not NULL");
                }

                faceProc.normalizeCoordinates(surfaceWidth, surfaceHeight);
                preview.removeView(drawView);// Remove the previously created view to avoid unnecessary stacking of Views.
                drawView = new DrawView(this, faceArray, true, surfaceWidth, surfaceHeight, cameraObj, landScapeMode);
                preview.addView(drawView);

                for (int j = 0; j < numFaces; j++) {
                    smileValue = faceArray[j].getSmileValue();
                    leftEyeBlink = faceArray[j].getLeftEyeBlink();
                    rightEyeBlink = faceArray[j].getRightEyeBlink();
                    faceRollValue = faceArray[j].getRoll();
                    gazePointValue = faceArray[j].getEyeGazePoint();
                    pitch = faceArray[j].getPitch();
                    yaw = faceArray[j].getYaw();
                    horizontalGaze = faceArray[j].getEyeHorizontalGazeAngle();
                    verticalGaze = faceArray[j].getEyeVerticalGazeAngle();
                }

                if(smileValue >80) {
                    mPlayer.stop();
                    stopLockTask(); // Android L+ only
                    finish();
                }

                setUI(numFaces, smileValue, leftEyeBlink, rightEyeBlink, faceRollValue, yaw, pitch, gazePointValue,
                        horizontalGaze, verticalGaze);
            }
        }
    }

    //END OF ADDED CODE








}