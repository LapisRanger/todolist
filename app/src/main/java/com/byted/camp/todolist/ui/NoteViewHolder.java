package com.byted.camp.todolist.ui;

import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.byted.camp.todolist.Data;
import com.byted.camp.todolist.MainActivity;
import com.byted.camp.todolist.NoteOperator;
import com.byted.camp.todolist.R;
import com.byted.camp.todolist.beans.Note;
import com.byted.camp.todolist.beans.State;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created on 2019/1/23.
 *
 * @author xuyingyi@bytedance.com (Yingyi Xu)
 */
public class NoteViewHolder extends RecyclerView.ViewHolder {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.ENGLISH);

    private final NoteOperator operator;

    private CheckBox checkBox;
    private TextView contentText;
    private TextView dateText;
    private View deleteBtn;

    //音频播放
    private SoundPool mSoundPool=null;
    private HashMap<Integer,Integer> soundID=new HashMap<Integer,Integer>();
    //默认值
    private Integer default_audio=1;

    public NoteViewHolder(@NonNull View itemView, NoteOperator operator) {
        super(itemView);
        this.operator = operator;

        checkBox = itemView.findViewById(R.id.checkbox);
        contentText = itemView.findViewById(R.id.text_content);
        dateText = itemView.findViewById(R.id.text_date);
        deleteBtn = itemView.findViewById(R.id.btn_delete);

        AudioAttributes abs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        mSoundPool=new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(abs).build();
        soundID.put(1,mSoundPool.load(itemView.getContext(),R.raw.reward1,1));
        soundID.put(2,mSoundPool.load(itemView.getContext(),R.raw.reward2,1));
        soundID.put(3,mSoundPool.load(itemView.getContext(),R.raw.winwinwin,1));
        soundID.put(4,mSoundPool.load(itemView.getContext(),R.raw.nice,1));
        soundID.put(5,mSoundPool.load(itemView.getContext(),R.raw.high,1));

        final Data app=(Data)Data.getMyData();
        default_audio=app.getDA();
    }

    public void bind(final Note note) {
        contentText.setText(note.getContent());
        dateText.setText(SIMPLE_DATE_FORMAT.format(note.getDate()));

        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(note.getState() == State.DONE);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                note.setState(isChecked ? State.DONE : State.TODO);
                operator.updateNote(note);
                mSoundPool.play(soundID.get(default_audio),1,1,0,0,1);
            }
        });
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                operator.deleteNote(note);
            }
        });

        // 已完成内容设置字体颜色和划掉字的线
        if (note.getState() == State.DONE) {
            contentText.setTextColor(Color.GRAY);
            contentText.setPaintFlags(contentText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            contentText.setTextColor(Color.BLACK);
            contentText.setPaintFlags(contentText.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        //优先级标识
        itemView.setBackgroundColor(note.getPriority().color);
    }
}
