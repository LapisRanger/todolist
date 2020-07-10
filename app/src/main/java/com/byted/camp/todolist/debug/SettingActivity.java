package com.byted.camp.todolist.debug;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.byted.camp.todolist.Data;
import com.byted.camp.todolist.MainActivity;
import com.byted.camp.todolist.R;

import java.util.HashMap;

public class SettingActivity extends AppCompatActivity {

    //提示文字
    private TextView textView;
    //确认按钮
    private Button button;
    //选择按钮
    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private Button button5;
    //音频播放
    private SoundPool mSoundPool=null;
    private HashMap<Integer,Integer> soundID=new HashMap<Integer,Integer>();
    //默认值
    private Integer default_audio=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        try{
            initSP();
        }catch (Exception e){
            e.printStackTrace();
        }
        bindViews();
    }

    private void bindViews(){
        textView=findViewById(R.id.textView);
        button=findViewById(R.id.confirm_btn);
        button1=findViewById(R.id.au_1);
        button2=findViewById(R.id.au_2);
        button3=findViewById(R.id.au_3);
        button4=findViewById(R.id.au_4);
        button5=findViewById(R.id.au_5);

        final Data app=(Data)getApplication();

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSoundPool.play(soundID.get(1),1,1,0,0,1);
                default_audio=1;
                app.setDA(default_audio);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSoundPool.play(soundID.get(2),1,1,0,0,1);
                default_audio=2;
                app.setDA(default_audio);
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSoundPool.play(soundID.get(3),1,1,0,0,1);
                default_audio=3;
                app.setDA(default_audio);
            }
        });
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSoundPool.play(soundID.get(4),1,1,0,0,1);
                default_audio=4;
                app.setDA(default_audio);
            }
        });
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSoundPool.play(soundID.get(5),1,1,0,0,1);
                default_audio=5;
                app.setDA(default_audio);
            }
        });


        button.setOnClickListener(new View.OnClickListener() {//返回首页,把默认音效参数传到首页
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(SettingActivity.this, MainActivity.class);
                Bundle data=new Bundle();
                data.putInt("default_audio",default_audio);
                intent.putExtra("data",data);
                startActivity(intent);
            }
        });
    }

    private void initSP() throws Exception{
        AudioAttributes abs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        mSoundPool=new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(abs).build();
        soundID.put(1,mSoundPool.load(this,R.raw.reward1,1));
        soundID.put(2,mSoundPool.load(this,R.raw.reward2,1));
        soundID.put(3,mSoundPool.load(this,R.raw.winwinwin,1));
        soundID.put(4,mSoundPool.load(this,R.raw.nice,1));
        soundID.put(5,mSoundPool.load(this,R.raw.high,1));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSoundPool.release();
    }
}