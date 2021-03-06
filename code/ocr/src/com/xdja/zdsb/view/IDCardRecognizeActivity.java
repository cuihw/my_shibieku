package com.xdja.zdsb.view;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import com.xdja.zdsb.R;
import com.xdja.zdsb.auth.RecognizerAuth;
import com.xdja.zdsb.bean.DriverIDCard;
import com.xdja.zdsb.bean.SecondIDCard;
import com.xdja.zdsb.utils.Constant;
import com.xdja.zdsb.utils.FileUtils;
import com.xdja.zdsb.utils.Zzlog;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import wintone.idcard.android.IDCardAPI;
import wintone.idcard.android.RecogParameterMessage;
import wintone.idcard.android.RecogService;
import wintone.idcard.android.ResultMessage;

/**
 * @version
 */
@SuppressLint("NewApi")
public class IDCardRecognizeActivity extends Activity
        implements SurfaceHolder.Callback, Camera.PreviewCallback, OnClickListener {

    private static final String TAG = "CameraActivity";

    public String PATH = Environment.getExternalStorageDirectory().toString() + "/xdja";

    private int screenWidth, screenHeight, previewWIDTH, previewHEIGHT;

    private Camera camera;

    private SurfaceView surfaceView;

    private SurfaceHolder surfaceHolder;

    private RelativeLayout bg_camera_doctype;

    public RecogService.recogBinder recogBinder;

    private DisplayMetrics displayMetrics = new DisplayMetrics();

    private ViewfinderView viewfinder_view;

    private int uiRot = 0;

    private ImageButton imbtn_flash, imbtn_camera_back;

    private IDCardAPI api = new IDCardAPI();

    // preview graphic data.
    private byte[] data1;

    private int regWidth, regHeight, left, right, top, bottom, nRotateType;

    private TextView tv_camera_doctype;

    // end
    private int quality = 100;

    private String picPathString = PATH + "/xdjaIDCard.jpg";

    private String HeadJpgPath;

    private String recogResultString = "";

    private double screenInches;

    private int[] nflag = new int[4];

    private boolean isTakePic = false;

    public static int nMainIDX; // 2 is Id card.

    private Vibrator mVibrator;

    private int Format = ImageFormat.NV21;// .YUY2

    private String name = "";

    private boolean isFocusSuccess = false;

    private boolean isTouched = false;

    private boolean isFirstGetSize = true;

    private Size size;

    private TimerTask timerTask;

    private boolean isConfirmSideLine = true;

    private int ConfirmSideSuccess = 0;

    private int CheckPicIsClearCounts = 0;

    private Timer timer;

    private int daltaW;

    private boolean savePic;

    @Override
    @SuppressLint("NewApi")
    @TargetApi(19)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        setContentView(R.layout.id_card_recognize_activity);
        double x = Math.pow(displayMetrics.widthPixels / displayMetrics.xdpi, 2);
        double y = Math.pow(displayMetrics.heightPixels / displayMetrics.ydpi, 2);
        screenInches = Math.sqrt(x + y);

        Zzlog.out(TAG, "Screen inches : " + screenInches);
        Zzlog.out(TAG, "Screen width : " + screenWidth + ", height = " + screenHeight);

        initView();

        Intent intent = getIntent();

        if (!RecognizerAuth.check(intent)) {
            Intent retIntent = new Intent();
            retIntent.putExtra("number", "");
            retIntent.putExtra("SFZH", "");
            retIntent.putExtra("data", "authentication failed.");
            setResult(RESULT_OK, retIntent);
            finish();
        }
        savePic = intent.getBooleanExtra("savePic", true);


    }

    @Override
    protected void onResume() {
        Zzlog.out(TAG, "onResume()");
        RecogService.isRecogByPath = false;
        Intent intent = getIntent();
        nMainIDX = intent.getIntExtra("nMainId", 2);

        ViewfinderView.setIdcardType(nMainIDX);

        switch (nMainIDX) {
        case 3000:
            tv_camera_doctype.setText(getString(R.string.mrz));
            break;
        case 13:
            tv_camera_doctype.setText(getString(R.string.passport));
            break;
        case 2:
            tv_camera_doctype.setText(getString(R.string.ID_card));
            break;
        case 9:
            tv_camera_doctype.setText(getString(R.string.EPT_HK_Macau));
            break;
        case 11:
            tv_camera_doctype.setText(getString(R.string.MRTTTP));
            break;
        case 12:
            tv_camera_doctype.setText(getString(R.string.visa));
            break;
        case 22:
            tv_camera_doctype.setText(getString(R.string.NEEPT_HK_Macau));
            break;
        case 5:
            tv_camera_doctype.setText(getString(R.string.china_driver));
            break;
        case 6:
            tv_camera_doctype.setText(getString(R.string.china_driving_license));
            break;
        case 1001:
            tv_camera_doctype.setText(getString(R.string.HK_IDcard));
            break;
        case 14:
            tv_camera_doctype.setText(getString(R.string.HRPO));
            break;
        case 15:
            tv_camera_doctype.setText(getString(R.string.HRPR));
            break;
        case 1005:
            tv_camera_doctype.setText(getString(R.string.IDCard_Macau));
            break;
        case 10:
            tv_camera_doctype.setText(getString(R.string.TRTTTMTP));
            break;
        case 1031:
            tv_camera_doctype.setText(getString(R.string.Taiwan_IDcard_front));
            break;
        case 1032:
            tv_camera_doctype.setText(getString(R.string.Taiwan_IDcard_reverse));
            break;
        case 1030:
            tv_camera_doctype.setText(getString(R.string.National_health_insurance_card));
            break;
        case 2001:
            tv_camera_doctype.setText(getString(R.string.MyKad));
            break;
        case 2004:
            tv_camera_doctype.setText(getString(R.string.Singapore_IDcard));
            break;
        case 2003:
            tv_camera_doctype.setText(getString(R.string.Driver_license));
            break;
        case 2002:
            tv_camera_doctype.setText(getString(R.string.California_driver_license));
            break;
        default:
            break;
        }

        super.onResume();
    }

    @SuppressWarnings("deprecation")
    private void initView() {
        findView();
        surfaceHolder = surfaceView.getHolder();
        int surfaceViewHeight = surfaceView.getHeight();
        int surfaceViewWidth = surfaceView.getWidth();
        Zzlog.out(TAG, "surfaceViewWidth = " + surfaceViewWidth + ", surfaceViewHeight = " + surfaceViewHeight);

        surfaceHolder.addCallback(IDCardRecognizeActivity.this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        tv_camera_doctype.setTextColor(Color.rgb(243, 153, 18));
    }

    /**
     * @Title: findView
     */
    @SuppressWarnings("deprecation")
    private void findView() {
        bg_camera_doctype = (RelativeLayout) findViewById(R.id.bg_camera_doctype);
        viewfinder_view = (ViewfinderView) findViewById(R.id.viewfinder_view);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceViwe);
        imbtn_flash = (ImageButton) findViewById(R.id.imbtn_flash);
        imbtn_camera_back = (ImageButton) findViewById(R.id.imbtn_camera_back);
        imbtn_camera_back.setOnClickListener(this);
        tv_camera_doctype = (TextView) findViewById(R.id.tv_camera_doctype);
        imbtn_flash.setOnClickListener(this);

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        uiRot = getWindowManager().getDefaultDisplay().getRotation();
        viewfinder_view.setDirecttion(uiRot);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams((int) (screenWidth * 0.1),
                (int) (screenWidth * 0.1));

        layoutParams.leftMargin = (int) (screenWidth * 0.04);
        layoutParams.topMargin = (int) (screenHeight * 0.05);
        imbtn_flash.setLayoutParams(layoutParams);

        layoutParams = new RelativeLayout.LayoutParams((int) (screenWidth * 0.1), (int) (screenWidth * 0.1));

        layoutParams.leftMargin = (int) (screenWidth * 0.04);
        layoutParams.topMargin = (int) (screenHeight * 0.96) - (int) (screenWidth * 0.1);
        imbtn_camera_back.setLayoutParams(layoutParams);

        layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, screenHeight);
        surfaceView.setLayoutParams(layoutParams);

        layoutParams = new RelativeLayout.LayoutParams((int) (screenWidth * 0.65), (int) (screenWidth * 0.05));
        layoutParams.leftMargin = (int) (screenWidth * 0.2);
        layoutParams.topMargin = (int) (screenHeight * 0.46);
        bg_camera_doctype.setLayoutParams(layoutParams);

        if (screenInches >= 8) {
            tv_camera_doctype.setTextSize(25);
        } else {
            tv_camera_doctype.setTextSize(20);
        }
    }

    @SuppressWarnings("deprecation")
    private void initCamera() {
        if (camera == null) {
            camera = Camera.open();
        }

        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        Camera.Size size;
        int previewWidth = 640;
        int previewheight = 480;
        int second_previewWidth = 0;
        int second_previewheight = 0;

        int length = list.size();

        if (length == 1) {
            size = list.get(0);
            previewWidth = size.width;
            previewheight = size.height;
        } else {
            for (int i = 0; i < length; i++) {
                size = list.get(i);
                Zzlog.out(TAG, "size: " + size.width + " , " + size.height);
                if (size.width <= 960 || size.height <= 720) {
                    second_previewWidth = size.width;
                    second_previewheight = size.height;
                    if (previewWidth <= second_previewWidth) {
                        previewWidth = second_previewWidth;
                        previewheight = second_previewheight;
                    }
                }
            }
        }

        previewWIDTH = previewWidth;
        previewHEIGHT = previewheight;

        // Please do not delete this comments. Chris Cui.
        // int surfaceWidth = screenHeight * previewWIDTH / previewHEIGHT;
        // int surfaceHeight = screenHeight;
        //
        // daltaW = screenWidth - surfaceWidth / 2;
        //
        // if (surfaceWidth > screenWidth) {
        // surfaceWidth = screenWidth;
        // surfaceHeight = screenWidth * previewHEIGHT / previewWIDTH;
        // daltaW = 0;
        // }

        int surfaceWidth = screenHeight * previewWIDTH / previewHEIGHT;
        int surfaceHeight = screenHeight;
        daltaW = 0;

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(surfaceWidth, surfaceHeight);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        surfaceView.setLayoutParams(layoutParams);

        Zzlog.out(TAG, "surfaceWidth :" + surfaceWidth);

        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        parameters.setPictureFormat(PixelFormat.JPEG);

        parameters.setExposureCompensation(0);
        parameters.setPreviewSize(previewWIDTH, previewHEIGHT);
        Zzlog.out(TAG, "previewWIDTH:" + previewWIDTH + ", previewHEIGHT:" + previewHEIGHT);

        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.setPreviewCallback(IDCardRecognizeActivity.this);
        camera.setParameters(parameters);
        camera.startPreview();

        // start auto focus.
        Zzlog.out(TAG, "Build.MODEL:" + Build.MODEL);

        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timerTask = new FocusTimerTask();
        timer.schedule(timerTask, 200, 2500);
        setISO();
    }

    private void setISO() {
        Camera.Parameters parameters = camera.getParameters();

        String supportedIsoValues = parameters.get("iso-values");

        Zzlog.out(TAG, "supportedIsoValues:" + supportedIsoValues);
    }

    private class FocusTimerTask extends TimerTask {

        @Override
        public void run() {
            if (camera != null) {
                try {
                    isFocusSuccess = false;
                    autoFocus();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        initCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        closeCamera();
    }

    private void closeCamera() {
        synchronized (this) {
            try {
                if (camera != null) {
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

            } catch (Exception e) {
                Zzlog.out("TAG", e.getMessage());
            }
            unbindRecognizeService();
        }
    }

    private void autoFocus() {
        if (camera != null) {
            synchronized (camera) {
                try {
                    if (camera.getParameters().getSupportedFocusModes() != null && camera.getParameters()
                            .getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {

                        camera.autoFocus(new AutoFocusCallback() {

                            // focus and tack picture.
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (success) {
                                    isFocusSuccess = true;
                                    Zzlog.out(TAG, "isFocusSuccess:" + isFocusSuccess);

                                    new Thread(new Runnable() {

                                        @Override
                                        public void run() {
                                            try {
                                                Thread.sleep(2000);
                                                isFocusSuccess = false;
                                                Zzlog.out(TAG, "startRecognizing().. isFocusSuccess = false");
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }).start();
                                }
                            }
                        });
                    } else {

                        Toast.makeText(getBaseContext(), getString(R.string.unsupport_auto_focus), Toast.LENGTH_LONG)
                                .show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    camera.stopPreview();
                    camera.startPreview();
                    Toast.makeText(this, R.string.toast_autofocus_failure, Toast.LENGTH_SHORT).show();

                }
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            closeCamera();
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPreviewFrame(byte[] data, final Camera camera) {
        if (isTouched) {
            return;
        }

        if (isFirstGetSize) {
            isFirstGetSize = false;
            size = camera.getParameters().getPreviewSize();
        }

        if (!isTakePic) {
            if (isFocusSuccess) {
                int CheckPicIsClear = 0;
                if (nMainIDX == 2 || nMainIDX == 22 || nMainIDX == 1030 || nMainIDX == 1031 || nMainIDX == 1032
                        || nMainIDX == 1005 || nMainIDX == 1001 || nMainIDX == 2001 || nMainIDX == 2004
                        || nMainIDX == 2002 || nMainIDX == 2003 || nMainIDX == 14 || nMainIDX == 15) {

                    if (daltaW == 0) {
                        api.SetROI((int) (size.width * 0.20), (int) (size.height - size.width * 0.65 * 54 / 85.6) / 2,
                                (int) (size.width * 0.85), (int) (size.height + size.width * 0.65 * 54 / 85.6) / 2);

                    } else {
                        api.SetROI((int) (size.width * 0.53 - 85.6 * size.height * 0.82 / 54 / 2),
                                (int) (size.height * 0.09),
                                (int) (size.width * 0.53 + 85.6 * size.height * 0.82 / 54 / 2),
                                (int) (size.height * 0.91));
                    }

                } else if (nMainIDX == 5 || nMainIDX == 6) {
                    api.SetROI((int) (size.width * 0.22), (int) (size.height - 0.41004673 * size.width) / 2,
                            (int) (size.width * 0.83), (int) (size.height + 0.41004673 * size.width) / 2);
                } else {
                    api.SetROI((int) (size.width * 0.2), (int) (size.height - 0.45 * size.width) / 2,
                            (int) (size.width * 0.85), (int) (size.height + 0.45 * size.width) / 2);
                }

                Zzlog.out(TAG, "nflag = " + nflag[0] + ", " + nflag[1] + ", " + nflag[2] + ", " + nflag[3]);
                if (isConfirmSideLine) {
                    ConfirmSideSuccess = api.ConfirmSideLine(data, size.width, size.height, nflag);
                }
                Zzlog.out(TAG,
                        "isConfirmSideLine = " + isConfirmSideLine + "ConfirmSideSuccess = " + ConfirmSideSuccess
                                + "size.width = " + size.width + "size.height = " + size.height + "nflag = " + nflag[0]
                                + ", " + nflag[1] + ", " + nflag[2] + ", " + nflag[3]);

                if (ConfirmSideSuccess == 1) {
                    isConfirmSideLine = false;
                    CheckPicIsClearCounts = CheckPicIsClearCounts + 1;
                    if (CheckPicIsClearCounts > 5) {
                        isConfirmSideLine = true;
                        CheckPicIsClearCounts = 0;
                        return;
                    }

                    CheckPicIsClear = api.CheckPicIsClear(data, size.width, size.height);

                    if (CheckPicIsClear == 1) {
                        isConfirmSideLine = true;
                        CheckPicIsClearCounts = 0;
                        viewfinder_view.setCheckLeftFrame(nflag[0]);
                        viewfinder_view.setCheckTopFrame(nflag[1]);
                        viewfinder_view.setCheckRightFrame(nflag[2]);
                        viewfinder_view.setCheckBottomFrame(nflag[3]);
                    }
                }

                if (ConfirmSideSuccess == 1 && CheckPicIsClear == 1) {
                    data1 = data;
                    name = pictureName();

                    HeadJpgPath = FileUtils.getStringFileName(FileUtils.MEDIA_TYPE_HEAD_IMAGE);
                    picPathString = FileUtils.getStringFileName(FileUtils.MEDIA_TYPE_IMAGE);

                    File file = new File(PATH);

                    if (!file.exists())
                        file.mkdirs();

                    YuvImage yuvimage = new YuvImage(data1, Format, size.width, size.height, null);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (nMainIDX == 2 || nMainIDX == 22 || nMainIDX == 1030 || nMainIDX == 1031 || nMainIDX == 1032
                            || nMainIDX == 1005 || nMainIDX == 1001 || nMainIDX == 2001 || nMainIDX == 2004
                            || nMainIDX == 2002 || nMainIDX == 2003 || nMainIDX == 14 || nMainIDX == 15) {
                        yuvimage.compressToJpeg(new Rect(0, 0, size.width, size.height), quality, baos);
                    } else {
                        yuvimage.compressToJpeg(
                                new Rect((int) (size.width * 0.15), (int) (size.height - 0.47 * size.width) / 2,
                                        (int) (size.width * 0.8), (int) (size.height + 0.47 * size.width) / 2),
                                quality, baos);
                    }
                    FileOutputStream outStream;
                    try {
                        outStream = new FileOutputStream(picPathString);
                        outStream.write(baos.toByteArray());
                        outStream.close();
                        baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    name = pictureName();

                    isTakePic = true;
                    if (timerTask != null)
                        timerTask.cancel();

                    RecogService.isRecogByPath = false;
                    Intent recogIntent = new Intent(IDCardRecognizeActivity.this, RecogService.class);
                    bindService(recogIntent, recogConn, Service.BIND_AUTO_CREATE);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.imbtn_camera_back:
            closeCamera();
            finish();
            break;
        case R.id.imbtn_flash:
            Camera.Parameters parameters = camera.getParameters();
            List<String> flashList = parameters.getSupportedFlashModes();
            if (flashList != null && flashList.contains(Camera.Parameters.FLASH_MODE_TORCH)) {

                String mode = parameters.getFlashMode();
                Zzlog.out(TAG, "mode = " + mode);
                if (mode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    imbtn_flash.setBackgroundResource(R.drawable.flash_on);
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                } else {
                    imbtn_flash.setBackgroundResource(R.drawable.flash_off);
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                }

            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.unsupportflash), Toast.LENGTH_SHORT).show();
            }

            break;
        default:
            break;
        }
    }

    private ServiceConnection recogConn = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            recogBinder = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {

            String packageName = name.getPackageName();
            Zzlog.out(TAG, "package name = " + packageName);

            recogBinder = (RecogService.recogBinder) service;

            RecogParameterMessage rpm = new RecogParameterMessage();
            rpm.nTypeLoadImageToMemory = 0;
            rpm.nMainID = nMainIDX;
            rpm.nSubID = null;
            rpm.GetSubID = true;
            rpm.GetVersionInfo = true;
            rpm.logo = "";
            rpm.userdata = "";
            rpm.sn = "";
            rpm.authfile = "";
            rpm.isCut = false;
            rpm.triggertype = 0;
            rpm.devcode = Constant.DEVCODE;
            rpm.isOnlyClassIDCard = true;
            if (nMainIDX == 3000) {
                rpm.nv21bytes = data1;
                rpm.top = top;
                rpm.bottom = bottom;
                rpm.left = left;
                rpm.right = right;
                rpm.nRotateType = nRotateType;
                rpm.width = regWidth;
                rpm.height = regHeight;

            } else if (nMainIDX == 2) {
                rpm.isAutoClassify = true;
                // pic data
                rpm.nv21bytes = data1;
                rpm.nv21_width = previewWIDTH;
                rpm.nv21_height = previewHEIGHT;

                // jpg path
                rpm.lpHeadFileName = HeadJpgPath;
                rpm.lpFileName = picPathString; // rpm.lpFileName
            } else {
                rpm.nv21bytes = data1;
                rpm.nv21_width = previewWIDTH;
                rpm.nv21_height = previewHEIGHT;
                rpm.lpHeadFileName = HeadJpgPath;
                rpm.lpFileName = picPathString; // rpm.lpFileName
            }
            // end
            try {

                camera.stopPreview();
                ResultMessage resultMessage;
                resultMessage = recogBinder.getRecogResult(rpm);
                if (resultMessage.ReturnAuthority == 0 && resultMessage.ReturnInitIDCard == 0
                        && resultMessage.ReturnLoadImageToMemory == 0 && resultMessage.ReturnRecogIDCard > 0) {

                    String[] GetFieldName = resultMessage.GetFieldName;
                    String[] GetRecogResult = resultMessage.GetRecogResult;

                    for (int i = 1; i < GetFieldName.length; i++) {
                        if (GetRecogResult[i] != null) {
                            if (!recogResultString.equals(""))
                                recogResultString = recogResultString + GetFieldName[i] + ":" + GetRecogResult[i] + ",";
                            else {
                                recogResultString = GetFieldName[i] + ":" + GetRecogResult[i] + ",";
                            }
                        }
                    }

                    setIdBean(GetFieldName, GetRecogResult);

                    camera.setPreviewCallback(null);
                    closeCamera();
                    mVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
                    mVibrator.vibrate(200);
                    Zzlog.out(TAG, "recogResultString:" + recogResultString);

                    recognizerResult(recogResultString);

                } else {
                    String string = "";
                    if (resultMessage.ReturnAuthority == -100000) {
                        string = getString(R.string.exception) + resultMessage.ReturnAuthority;
                    } else if (resultMessage.ReturnAuthority != 0) {
                        string = getString(R.string.exception1) + resultMessage.ReturnAuthority;
                    } else if (resultMessage.ReturnInitIDCard != 0) {
                        string = getString(R.string.exception2) + resultMessage.ReturnInitIDCard;
                    } else if (resultMessage.ReturnLoadImageToMemory != 0) {
                        if (resultMessage.ReturnLoadImageToMemory == 3) {
                            string = getString(R.string.exception3) + resultMessage.ReturnLoadImageToMemory;
                        } else if (resultMessage.ReturnLoadImageToMemory == 1) {
                            string = getString(R.string.exception4) + resultMessage.ReturnLoadImageToMemory;
                        } else {
                            string = getString(R.string.exception5) + resultMessage.ReturnLoadImageToMemory;
                        }
                    } else if (resultMessage.ReturnRecogIDCard <= 0) {
                        if (resultMessage.ReturnRecogIDCard == -6) {
                            string = getString(R.string.exception9);
                        } else {
                            string = getString(R.string.exception6) + resultMessage.ReturnRecogIDCard;
                        }
                    }
                    string = "-1 error:" + string;
                    closeCamera();

                    if (!savePic) {
                        File file = new File(HeadJpgPath);
                        if (file.exists()) {
                            file.delete();
                        }

                        file = new File(picPathString);
                        if (file.exists()) {
                            file.delete();
                        }
                    }

                    recognizerResult(string);

                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), getString(R.string.recognized_failed), Toast.LENGTH_SHORT)
                        .show();

            } finally {
                unbindRecognizeService();
            }

        }
    };

    protected void recognizerResult(final String recogResult) {

        closeCamera();

        String result = "";

        if (recogResult.startsWith("-1 error:")) {

            result = recogResult.substring(recogResult.indexOf(":") + 1);

        } else {

            String[] splite_Result = recogResult.split(",");
            for (int i = 0; i < splite_Result.length; i++) {
                if (result.equals("")) {
                    result = splite_Result[i] + "\n";
                } else {
                    result = result + splite_Result[i] + "\n";
                }
            }
        }

        new AlertDialog.Builder(this).setTitle(getString(R.string.recognize_result)).setMessage(result)

                .setPositiveButton(getString(R.string.confirm_btn_string), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setRecognizeResult(recogResult);
                        IDCardRecognizeActivity.this.finish();
                    }
                })

                .setNegativeButton(getString(R.string.retry_scan), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // reset parameter.
                        isTakePic = false;
                        api = new IDCardAPI();
                        isConfirmSideLine = true;
                        ConfirmSideSuccess = 0;
                        CheckPicIsClearCounts = 0;
                        recogResultString = "";
                        viewfinder_view.setCheckLeftFrame(0);
                        viewfinder_view.setCheckTopFrame(0);
                        viewfinder_view.setCheckRightFrame(0);
                        viewfinder_view.setCheckBottomFrame(0);
                        initCamera();
                    }

                }).show();

    }

    private SecondIDCard secondIDCard = null;

    private DriverIDCard driverIDCard = null;

    protected void setIdBean(String[] getFieldName, String[] getRecogResult) {
        secondIDCard = null;
        driverIDCard = null;
        if (null != getFieldName) {
            if (nMainIDX == 2) {
                SecondIDCard card = new SecondIDCard();
                card.setBlzd(getRecogResult[0]);
                card.setXm(getRecogResult[1]);
                card.setXb(getRecogResult[2]);
                card.setMz(getRecogResult[3]);
                card.setCsrq(getRecogResult[4]);
                card.setCsdz(getRecogResult[5]);
                card.setSfzh(getRecogResult[6]);
                secondIDCard = card;
            }

            if (nMainIDX == 5) {
                DriverIDCard card = new DriverIDCard();
                card.setBlzd(getRecogResult[0]);
                card.setSfzh(getRecogResult[1]);
                card.setXm(getRecogResult[2]);
                card.setXb(getRecogResult[3]);
                card.setCsdz(getRecogResult[4]);
                card.setCsrq(getRecogResult[5]);
                card.setCslzrq(getRecogResult[6]);
                card.setZjcx(getRecogResult[7]);
                card.setYxqqsrq(getRecogResult[8]);
                card.setYxqx(getRecogResult[9]);
                driverIDCard = card;
            }
        }

    }

    protected void setRecognizeResult(String recogResult) {
        Intent intent = new Intent();
        Gson gson = new Gson();

        if (secondIDCard != null) {
            intent.putExtra("SFZH", secondIDCard.getSfzh());
            intent.putExtra("DATA", gson.toJson(secondIDCard));
        }
        if (driverIDCard != null) {
            intent.putExtra("SFZH", driverIDCard.getSfzh());
            intent.putExtra("DATA", gson.toJson(driverIDCard));
        }

        intent.putExtra("data", recogResult);
        setResult(RESULT_OK, intent);
    }

    private void unbindRecognizeService() {
        try {
            if (recogBinder != null) {
                unbindService(recogConn);
                recogBinder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     */
    public String pictureName() {
        String str = "";
        Time t = new Time();
        t.setToNow();
        int year = t.year;
        int month = t.month + 1;
        int date = t.monthDay;
        int hour = t.hour; // 0-23
        int minute = t.minute;
        int second = t.second;

        if (month < 10)
            str = String.valueOf(year) + "0" + String.valueOf(month);
        else {
            str = String.valueOf(year) + String.valueOf(month);
        }
        if (date < 10)
            str = str + "0" + String.valueOf(date);
        else {
            str = str + String.valueOf(date);
        }
        if (hour < 10)
            str = str + "0" + String.valueOf(hour);
        else {
            str = str + String.valueOf(hour);
        }
        if (minute < 10)
            str = str + "0" + String.valueOf(minute);
        else {
            str = str + String.valueOf(minute);
        }
        if (second < 10)
            str = str + "0" + String.valueOf(second);
        else {
            str = str + String.valueOf(second);
        }
        return str;
    }

}
