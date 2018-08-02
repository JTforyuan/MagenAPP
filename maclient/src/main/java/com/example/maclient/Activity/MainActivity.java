package com.example.maclient.Activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TimeUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.maclient.R;
import com.example.maclient.Views.AudioRecordView;
import com.example.maclient.utils.AsyncUtils;
import com.example.maclient.utils.AudioRecordManager;
import com.example.maclient.utils.NetUtils;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int MESSAGE_UPLOAD_WAV = 1;
    private static final int MESSAGE_DOWNLOAD_MIDI = 2;

    private String[] mPremission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Button mControlButton, mUploadButton, mDownloadButton;
    private AudioRecordView mAudioRecordView;
    private AudioRecordManager mAudioRecordManager;

    private CountDownTimer mTimer;
    private long mCountDownTime = 60000;
    private long mTime = 0;
    private String mFilePath;
    private String mGenerateFile;

    private Handler mHandler;
    private HandlerThread mThread;
    private Handler mUploadHandler;

    private Resources mRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRes = getResources();
        checkPremission();

        initBackgroundThread();

        mTimer = new CountDownTimer(mCountDownTime, 1000) {
            @Override
            public void onTick(long l) {
                mTime += 1000;
                String time = time2String(mTime);
                mAudioRecordView.setText(time);
            }

            @Override
            public void onFinish() {
                mTime = 0;
                mAudioRecordView.setText("00:00");
            }
        };

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case AudioRecordManager.MESSAGE_UPDATE_MIC :
                        updateMicState(msg.getData().getDouble(AudioRecordManager.RECORD_DB)); break;
                    case AudioRecordManager.MESSAGE_RECIEVE_FILE :
                        mFilePath = msg.getData().getString(AudioRecordManager.RECORD_FILE);
                        Toast.makeText(MainActivity.this, "录音完成，文件保存在" + mFilePath,
                                Toast.LENGTH_LONG).show();
                        Log.i(TAG, "handleMessage: 文件存储在" + mFilePath.split("/")[5]);break;
                }
            }
        };

        mAudioRecordManager = AudioRecordManager.getInstance(mHandler);

        mControlButton = findViewById(R.id.btn_control);
        mControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "onClick: 你点击了录音按钮" );
                if ( ! mAudioRecordManager.isRecorded ) {
                    mAudioRecordManager.startRecord();
                    mTimer.start();
                    mControlButton.setBackground(mRes.getDrawable(R.drawable.ic_record_stop));
                } else {
                    mAudioRecordManager.stopRecord();
                    mTimer.onFinish();
                    mTimer.cancel();
                    mControlButton.setBackground(mRes.getDrawable(R.drawable.ic_record_start));
                }
            }
        });

        mUploadButton = findViewById(R.id.btn_upload);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUploadHandler.obtainMessage(MESSAGE_UPLOAD_WAV).sendToTarget();
            }
        });

        mDownloadButton = findViewById(R.id.btn_download);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mUploadHandler.obtainMessage(MESSAGE_DOWNLOAD_MIDI).sendToTarget();
            }
        });

        mAudioRecordView = findViewById(R.id.image_audio_record);


    }

    private void initBackgroundThread() {
        mThread = new HandlerThread("uploadThread");
        mThread.start();
        mUploadHandler = new Handler(mThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_UPLOAD_WAV : handleUploadMessage();
                        break;
                    case MESSAGE_DOWNLOAD_MIDI : handleDownloadMessage();
                        break;
                }
            }
        };
    }

    private void handleUploadMessage() {

        Log.i(TAG, "即将上传的录音文件：" + mFilePath);
        mGenerateFile = AsyncUtils.UploadWavFile(mFilePath);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "上传生成的文件为 ： " + mGenerateFile);
                if ( mGenerateFile != null )
                    Toast.makeText(MainActivity.this, "上传完成！", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(MainActivity.this, "上传失败", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleDownloadMessage() {
        AsyncUtils.downloadMidiFile(mGenerateFile);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "下载完成！", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void updateMicState(double db) {
        Log.i(TAG, "updateMicState: 分贝值" + db);
        mAudioRecordView.getImageDrawable().setLevel((int) (3000 + 6000 * db / 100));
    }

    private void checkPremission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (i != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, mPremission, 0);
            }
        }
    }

    /**
     *  将时间转换成毫秒数
     * @param duration
     * @return
     */
    public static String time2String(long duration) {
        String time = "" ;

        long minute = duration / 60000 ;
        long seconds = duration % 60000 ;

        long second = Math.round((float)seconds/1000) ;

        if( minute < 10 ){
            time += "0" ;
        }
        time += minute+":" ;

        if( second < 10 ){
            time += "0" ;
        }
        time += second ;

        return time ;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
