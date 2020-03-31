package com.example.simpletest;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    // 录音开始、停止的按钮及显示录音状态的文本 图标
    private Button btnAudioRecord;
    private TextView recordingState;
    private Drawable drawable;

    // 是否在录音
    private Boolean isStart = false;

    // 用于录音的对象
    private MediaRecorder mr = null;

    // 当前录音片段的存储路径
    private String path;

    // 用于断续录音
    private Handler handler;
    private Timer timer;
    private TimerTask timerTask;

    // 断续录音的长度
    private int period = 30 * 1000;

    // 可存储的最大录音数目及当前文件夹中录音数
    private final int MAX_NUM = 2;
    private int count = 0;

    // 记录录音文件所用的队列
    Queue<String> filepaths = new LinkedList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnAudioRecord = (Button) findViewById(R.id.audioRecord);
        drawable = getResources().getDrawable(R.drawable.ic_radio_button_checked_red_500_24dp);
        drawable.setBounds(0, 0, 300, 300);
        btnAudioRecord.setCompoundDrawables(drawable, null, null, null);
    }

    // 录音按钮的响应函数
    @SuppressLint("HandlerLeak")
    public void record(View v) {
        btnAudioRecord = (Button) findViewById(R.id.audioRecord);
        recordingState = (TextView) findViewById(R.id.recordingState);
        // 点击录音按钮，进行断续录音
        if (!isStart) {
            startRecord();
            handler = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == 1) {
                        // 停止并释放上段录音
                        mr.stop();
                        mr.release();
                        mr = null;
                        count++;
                        filepaths.offer(path);
                        System.out.println(path);
                        if (count > MAX_NUM)
                            deleteRecord();
                        // 开始新的录音
                        startRecord();
                    }
                }
            };
            timer = new Timer(true);
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Message msg = new Message();
                    msg.what = 1;
                    handler.sendMessage(msg);
                }
            };
            // 录音分段存储
            timer.schedule(timerTask, period, period);
            recordingState.setText("正在录音");
            isStart = true;
            //System.out.println(isStart);
        }
        // 停止断续录音
        else {
            stopRecord();
            // 暂停断续录音
            timer.cancel();
            recordingState.setText("录音停止");
            isStart = false;
        }
    }

    // 开始录音
    protected void startRecord() {
        if (mr == null) {
            // 设置录音时的图标
            drawable = getResources().getDrawable(R.drawable.ic_radio_button_unchecked_red_500_24dp);
            drawable.setBounds(0, 0, 300, 300);
            btnAudioRecord.setCompoundDrawables(drawable, null, null, null);
            // 存储录音片段的文件夹
            File dir = new File(Environment.getExternalStorageDirectory(), "audioRecords");
            if (!dir.exists()) {
                dir.mkdir();
            }
            // 用时间命名录音文件
            File audio = new File(dir, System.currentTimeMillis() + ".amr");
            if (!audio.exists()) {
                try {
                    audio.createNewFile();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
            path = audio.getAbsolutePath();
            // mr的initial阶段
            mr = new MediaRecorder();
            // mr的initialized阶段
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            // mr的configure阶段
            mr.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            mr.setOutputFile(path);
            try {
                // 准备并开始录制
                mr.prepare();
                mr.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    // 停止录音
    protected void stopRecord() {
        if (mr != null) {
            // 当前录音数增加
            count++;
            // 如果超过允许的最大录音存储数目
            if (count > MAX_NUM)
                deleteRecord();
            // 设置暂停录音的图标
            drawable = getResources().getDrawable(R.drawable.ic_radio_button_checked_red_500_24dp);
            drawable.setBounds(0, 0, 300, 300);
            btnAudioRecord.setCompoundDrawables(drawable, null, null, null);
            // 停止这段录音并且释放
            mr.stop();
            mr.release();
            mr = null;
            filepaths.offer(path);
            System.out.println(path);

        }
    }

    // 删除第一条录音，为后面的录音留空间
    protected void deleteRecord() {
        String waitingForDelete = filepaths.poll();
        File file = new File(waitingForDelete);
        if (file.exists() && file.isFile()) {
            if (file.delete())
                System.out.println(waitingForDelete + " 已删除成功");
            else
                System.out.println("删除失败");
        }
        else System.out.println("文件不存在");
        count--;
    }
}
