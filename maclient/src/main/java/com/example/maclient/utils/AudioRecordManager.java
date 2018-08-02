package com.example.maclient.utils;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by JiaTang on 2018/2/5.
 */

public class AudioRecordManager {

    public static final String TAG = "AudioRecordUtil";
    public static final String RECORD_DB = "record_db";
    public static final String RECORD_TIME = "record_time";
    public static final String RECORD_FILE = "record_file_path";
    public static final int MESSAGE_UPDATE_MIC = 1;
    public static final int MESSAGE_RECIEVE_FILE = 2;

    public static final int MAX_RECORD_LENGTH = 1000 * 60 * 10;

    private volatile static AudioRecordManager mInstance;
    private Handler mHandler;
    private RecordThread mThread;
    private Object mLock; // 对象锁

    private File mRecordedFile;

    private AudioRecord mAudioRecord;
    private int mSampleRate = 44100; // 采样频率
    private int mChannelConfig = AudioFormat.CHANNEL_IN_STEREO; // 声道配置,立体声
    private int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT; // 编码方式，16位PCM编码
    private int minBufSize; // 数据缓冲区大小

    // 标志是否正在录音
    public boolean isRecorded = false;

    private long mStartTime;
    private long mEndTime;
    private int mIntervalTime = 1000; // 每隔1000ms更新一次麦克风状态

    private AudioRecordManager() {
        minBufSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioEncoding);
        Log.i(TAG, "min buffer size is =  " + minBufSize);

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, mChannelConfig,
                mAudioEncoding, minBufSize);

    }

    public static AudioRecordManager getInstance(Handler handler) {
        if (mInstance == null ) {
            mInstance = new AudioRecordManager();
        }
        mInstance.mHandler = handler;
        return mInstance;
    }

    class RecordThread extends Thread {

        @Override
        public void run() {

            Log.i(TAG, "run: 开始录音");

            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

            if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                return;
            }

            OutputStream out = null;
            ByteArrayOutputStream bs = null;

            try {

                bs = new ByteArrayOutputStream();
                byte[] buffer = new byte[minBufSize];
                int len = 0;
                mAudioRecord.startRecording();
                mStartTime = System.currentTimeMillis();

                double mean, db = 0.0;
                while (isRecorded) {
                    // 读取到的音频字节数据
                    len = mAudioRecord.read(buffer, 0, minBufSize);

                    long value = 0;
                    for (int i = 0; i < buffer.length; i++ ) {
                        value += buffer[i] * buffer[i];
                    }
                    mean = value / (double) len;
                    db = 10 * Math.log10(mean);

                    /*
                    Runnable mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            mOnUpdateListener.onUpdate(db);
                        }
                    };
                    */

                    mEndTime = System.currentTimeMillis() - mStartTime;

                    // 因为子线程不能进行UI操作，所以通过Handler来进行UI操作，而不是使用接口
                    // https://www.jianshu.com/p/06eca50ddda4
                    // mHandler.obtainMessage(MESSAGE_UPDATE_MIC, db).sendToTarget();
                    Message message = new Message();
                    message.what = MESSAGE_UPDATE_MIC;
                    Bundle bundle = new Bundle();
                    // bundle.putLong(RECORD_TIME, mEndTime);
                    bundle.putDouble(RECORD_DB, db);
                    message.setData(bundle);
                    mHandler.sendMessage(message);

                    // mHandler.postDelayed(mRunnable, 1000);

                    if (len > 0) {
                        // 将读到的数据写入流中
                        bs.write(buffer, 0, len);
                    }
                }

                buffer = bs.toByteArray();

                Log.i(TAG, "run: 录音文件字节长度 = " + buffer.length);

                out = new FileOutputStream(mRecordedFile);
                out.write(getWavHeader(buffer.length));
                out.write(buffer);

                Message message1 = new Message();
                message1.what = MESSAGE_RECIEVE_FILE;
                Bundle bundle1 = new Bundle();
                bundle1.putString(RECORD_FILE, mRecordedFile.getAbsolutePath());
                message1.setData(bundle1);
                mHandler.sendMessage(message1);

                Log.i(TAG, "run: 文件保存在" + mRecordedFile.getAbsolutePath());

                out.flush();
                out.close();
                bs.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startRecord() {
        isRecorded = true;
        initFilePath();
        if (mThread == null) {
            mThread = new RecordThread();
            mThread.start();
        }
    }

    private void initFilePath() {
        File dir = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath(), "maclient/record");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        mRecordedFile = new File(dir, "maclient_" + System.currentTimeMillis()
                + ".wav");
    }

    public long stopRecord() {
        if (mAudioRecord == null) {
            return 0L;
        }

        isRecorded = false;

        mThread = null;
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        mEndTime = System.currentTimeMillis();

        return mEndTime - mStartTime;
    }

    // 设置wav格式头
    private byte[] getWavHeader(long totalAudioLen){
        int mChannels = 2;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = mSampleRate;
        long byteRate = mSampleRate * 2 * mChannels;

        byte[] header = new byte[44];
        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) mChannels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * mChannels);  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }

}
