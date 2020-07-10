package com.byted.camp.todolist;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.byted.camp.todolist.beans.Note;
import com.byted.camp.todolist.beans.Priority;
import com.byted.camp.todolist.beans.State;
import com.byted.camp.todolist.db.TodoContract;
import com.byted.camp.todolist.db.TodoDbHelper;
import com.byted.camp.todolist.debug.DebugActivity;
import com.byted.camp.todolist.debug.SettingActivity;
import com.byted.camp.todolist.ui.NoteListAdapter;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {
    //所有在AndroidManifest中申请的用户权限
    private String[] mPermissions={Manifest.permission.INTERNET,Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.ACCESS_WIFI_STATE,
    Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};
    public static final int CODE=0x001;

    private static final int REQUEST_CODE_ADD = 1002;

    private RecyclerView recyclerView;
    private NoteListAdapter notesAdapter;

    // 增加数据库对象
    private TodoDbHelper dbHelper;
    private SQLiteDatabase database;

    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "xiaoyan";
    // 语音合成文本,所有代办事项内容
    String texts = "当前代办事项有:";
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    //播放按钮
    private FloatingActionButton play_btn;
    //是否正在播放
    private boolean isPlaying=false;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpeechUtility.createUtility(this, SpeechConstant.APPID+"=5ef2afbe");
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        if(EasyPermissions.hasPermissions(this,mPermissions)){
            //showTip("已授权");
        } else{
          EasyPermissions.requestPermissions(this,"申请权限",CODE,mPermissions);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //悬浮添加"+"号按钮,右下角,点击进入NoteActivity
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(MainActivity.this, NoteActivity.class),
                        REQUEST_CODE_ADD);
            }
        });

        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(MainActivity.this, mTtsInitListener);

        // 初始化
        dbHelper = new TodoDbHelper(this);
        database = dbHelper.getWritableDatabase();

        recyclerView = findViewById(R.id.list_todo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        notesAdapter = new NoteListAdapter(new NoteOperator() {
            @Override
            public void deleteNote(Note note) {
                MainActivity.this.deleteNote(note);
            }

            @Override
            public void updateNote(Note note) {
                MainActivity.this.updateNode(note);
            }
        });
        recyclerView.setAdapter(notesAdapter);

        notesAdapter.refresh(loadNotesFromDatabase());

        play_btn=findViewById(R.id.play_btn);
        play_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("texts=",texts);
                //防止播放时再次点击播放按钮
                if(!isPlaying){
                    if(texts=="当前代办事项有:") {
                        showTip("当前无待办事项");
                    }
                    else{
                        setParam();
                        int code=mTts.startSpeaking(texts,mTtsListener);
                        if (code != ErrorCode.SUCCESS) {
                            showTip("语音合成失败,错误码: " + code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
                        }
                        isPlaying=true;
                    }
                }
            }
        });
    }

    //所有的权限申请成功的回调
    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        //do something
        showTip("权限申请成功");
    }

    //权限获取失败的回调
    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        //存在被永久拒绝(拒绝&不再询问)的权限
        showTip("权限未申请成功,将导致程序不能正常运行");
    }

    //权限被拒绝后的显示提示对话框，点击确认的回调
    @Override
    public void onRationaleAccepted(int requestCode) {
        //会自动再次获取没有申请成功的权限
        //do something
    }

    //权限被拒绝后的显示提示对话框，点击取消的回调
    @Override
    public void onRationaleDenied(int requestCode) {
        //什么都不会做
        //do something
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //将结果传入EasyPermissions中
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    // 语音合成监听器
    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d("Text to Sound", "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码："+code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };
    /**
     * 参数设置
     * @return
     */
    private void setParam(){
        // 根据合成引擎设置相应参数
        if(mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //支持实时音频返回，仅在synthesizeToUri条件下支持
            mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY, "1");
            //	mTts.setParameter(SpeechConstant.TTS_BUFFER_TIME,"1");

            // 设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED, "50");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH, "50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME, "50");
        }else {
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");

        }
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "false");
    }
    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) { }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) { }

        @Override
        public void onCompleted(SpeechError error) {
            isPlaying=false;
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            //	 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            //	 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                Log.d("TAG", "session id =" + sid);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭数据库连接
        database.close();
        database=null;
        dbHelper.close();
        dbHelper=null;
        //关闭语音合成连接
        if( null != mTts ){
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

    //顶部导航栏右边...下拉菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingActivity.class));
                return true;
            case R.id.action_debug:
                startActivity(new Intent(this, DebugActivity.class));
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD
                && resultCode == Activity.RESULT_OK) {
            notesAdapter.refresh(loadNotesFromDatabase());
        }
    }

    private List<Note> loadNotesFromDatabase() {
        //从数据库中查询数据，并转换成 JavaBeans;
        //数据赋值给字符串text,语音合成参数
        if (database == null){
            return Collections.emptyList();
        }
        List<Note> notes = new ArrayList<>();
        Cursor cursor = null;
        int idx=1;
        texts = "当前代办事项有:";
        try {
            cursor = database.query(
                    TodoContract.TodoNote.TABLE_NAME,
                    null,null,null,null,null,
                    TodoContract.TodoNote.COLUMN_PRIORITY + " DESC"
            );
            while(cursor.moveToNext()){
                long id = cursor.getLong(cursor.getColumnIndex(TodoContract.TodoNote._ID));
                String content = cursor.getString((cursor.getColumnIndex(TodoContract.TodoNote.COLUMN_CONTENT)));
                long data = cursor.getLong(cursor.getColumnIndex(TodoContract.TodoNote.COLUMN_DATE));
                int priority = cursor.getInt(cursor.getColumnIndex(TodoContract.TodoNote.COLUMN_PRIORITY));
                int state = cursor.getInt(cursor.getColumnIndex(TodoContract.TodoNote.COLUMN_STATE));

                Note note = new Note(id);
                note.setContent(content);
                note.setDate(new Date(data));
                note.setPriority(Priority.from(priority));
                note.setState(State.from(state));

                notes.add(note);

                //语音合成字符串
                if(state==0){
                    texts+="第"+idx+"项"+content;
                    idx+=1;
                }

            }
        }
        catch(Exception e){
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return notes;
    }

    private void deleteNote(Note note) {
        // 删除数据
        if (database == null)
        {
            return ;
        }
        try {
            int rownum = database.delete(
                    TodoContract.TodoNote.TABLE_NAME,
                    TodoContract.TodoNote._ID + "=?",
                    new String[]{String.valueOf(note.id)}
            );
            if (rownum > 0)
            {
                notesAdapter.refresh(loadNotesFromDatabase());
            }
        }catch (Exception e)
        {
            Toast.makeText(MainActivity.this, "DeleteNote Error: "+e.getMessage(),Toast.LENGTH_SHORT);
        }
    }

    private void updateNode(Note note) {
        // 更新数据
        if (database == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(TodoContract.TodoNote.COLUMN_STATE, note.getState().intValue);
        try {
            int rows = database.update(TodoContract.TodoNote.TABLE_NAME, values,
                    TodoContract.TodoNote._ID + "=?",
                    new String[]{String.valueOf(note.id)});
            if (rows > 0) {
                notesAdapter.refresh(loadNotesFromDatabase());
            }
        }catch (Exception e)
        {
            Toast.makeText(MainActivity.this, "updateNode Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

}
