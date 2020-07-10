package com.byted.camp.todolist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.byted.camp.todolist.beans.State;
import com.byted.camp.todolist.beans.Priority;
import com.byted.camp.todolist.db.TodoContract;
import com.byted.camp.todolist.db.TodoDbHelper;
import com.byted.camp.todolist.JsonParser;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.cloud.util.ContactManager.ContactListener;


import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.myapache.commons.codec.binary.Base64;
import org.myapache.commons.codec.binary.Hex;
import org.myapache.commons.codec.digest.DigestUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class NoteActivity extends AppCompatActivity {

    // webapi接口地址
    private static final String SA_URL = "https://ltpapi.xfyun.cn/v2/sa";
    private static final String KE_URL = "https://ltpapi.xfyun.cn/v1/ke";
    // 应用ID
    private static final String APPID = "5ef2afbe";
    // 接口密钥
    private static final String API_KEY = "f0d3b3821ed5f12f46dfa0de95464c2a";

    private static final String TYPE = "dependent";

    private EditText editText;
    private Button addBtn;
    private Button voiceBtn;
    private Button saBtn;
    private Button keBtn;
    //增加控件和数据库对象
    private RadioGroup radioGroup;
    private AppCompatRadioButton lowRadio;

    private TodoDbHelper dbHelper;
    private SQLiteDatabase database;

    //HashMap存储听写结果
    private HashMap<String,String> mIatResults=new LinkedHashMap<String, String>();

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        setTitle(R.string.take_a_note);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        dbHelper = new TodoDbHelper(this);
        database = dbHelper.getWritableDatabase();

        editText = findViewById(R.id.edit_text);
        editText.setFocusable(true);
        editText.requestFocus();
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null) {
            inputManager.showSoftInput(editText, 0);
        }

        radioGroup = findViewById(R.id.radio_group);
        lowRadio = findViewById(R.id.btn_low);
        lowRadio.setChecked(true);

        addBtn = findViewById(R.id.btn_add);

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence content = editText.getText();
                if (TextUtils.isEmpty(content)) {
                    Toast.makeText(NoteActivity.this,
                            "No content to add", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean succeed = saveNote2Database(content.toString().trim(),getSelectedPriority());
                if (succeed) {
                    Toast.makeText(NoteActivity.this,
                            "Note added", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                } else {
                    Toast.makeText(NoteActivity.this,
                            "Error", Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        });

        voiceBtn=findViewById(R.id.button_input);
        voiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpeechClick(view);
            }
        });

        saBtn=findViewById(R.id.button_sa);
        saBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String TextContent=editText.getText().toString();
                if(TextContent.isEmpty()){
                    showTip("内容为空");
                }else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, String> header = null;
                            String result=null;
                            try {
                                header = buildHttpHeader();
                                result = HttpUtil.doPost1(SA_URL, header, "text=" + URLEncoder.encode(TextContent, "utf-8"));
                                JSONObject jsonObject=new JSONObject(result);
                                String code=jsonObject.getString("code");
                                if(code.equals("0")){
                                    JSONObject data=jsonObject.getJSONObject("data");
                                    int sentiment= Integer.parseInt(data.getString("sentiment"));
                                    if(sentiment==0) showTip("neutral");
                                    else if(sentiment==1) showTip("positive");
                                    else if(sentiment==-1) showTip("negative");
                                }else {
                                    showTip("请求失败");
                                }
                            } catch (UnsupportedEncodingException | JSONException e) {
                                e.printStackTrace();
                            }
                            Log.d("debughttp","result="+result);
                        }
                    }).start();
                }
            }
        });

        keBtn=findViewById(R.id.button_ke);
        keBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String TextContent=editText.getText().toString();
                if(TextContent.isEmpty()){
                    showTip("内容为空");
                }else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, String> header = null;
                            String result=null;
                            try {
                                header = buildHttpHeader();
                                result = HttpUtil.doPost1(KE_URL, header, "text=" + URLEncoder.encode(TextContent, "utf-8"));
                                JSONObject jsonObject=new JSONObject(result);
                                String code=jsonObject.getString("code");
                                if(code.equals("0")){
                                    JSONObject data=jsonObject.getJSONObject("data");
                                    JSONArray ke=data.getJSONArray("ke");
                                    StringBuilder keyword= new StringBuilder();
                                    //遍历数组
                                    for(int i=0;i<ke.length();i++){
                                        JSONObject ke_item=ke.getJSONObject(i);
                                        String word=ke_item.getString("word");
                                        keyword.append(word).append(";");
                                    }
                                    showTip(keyword.toString());
                                }else {
                                    showTip("请求失败");
                                }
                            } catch (UnsupportedEncodingException | JSONException e) {
                                e.printStackTrace();
                            }
                            Log.d("debughttp","result="+result);
                        }
                    }).start();
                }
            }
        });

        SpeechUtility.createUtility(this, SpeechConstant.APPID+"=5ef2afbe");
    }

    /**
     * 组装http请求头
     */
    private static Map<String, String> buildHttpHeader() throws UnsupportedEncodingException {
        String curTime = System.currentTimeMillis() / 1000L + "";
        String param = "{\"type\":\"" + TYPE +"\"}";
        String paramBase64 = new String(Base64.encodeBase64(param.getBytes("UTF-8")));
        String checkSum = new String(Hex.encodeHex(DigestUtils.md5(API_KEY + curTime + paramBase64)));
        Map<String, String> header = new HashMap<String, String>();
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        header.put("X-Param", paramBase64);
        header.put("X-CurTime", curTime);
        header.put("X-CheckSum", checkSum);
        header.put("X-Appid", APPID);
        return header;
    }

    public void startSpeechClick(View view){
        //初始化识别无UI识别对象
        //使用SpeechRecognizer对象，可根据回调消息自定义界面；
        SpeechRecognizer mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
        //设置参数
        mIat.setParameter(SpeechConstant.PARAMS, "iat");      //应用领域
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn"); //语音
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin"); //普通话
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);//引擎
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");//返回结果格式
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS,"1000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
        //开始听写
        mIat.startListening(mRecogListener);
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("TAG", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecogListener= new RecognizerListener() {

        //音量0-30
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        //开始录音
        @Override
        public void onBeginOfSpeech() {
            showTip("开始说话");
        }

        //结束录音
        @Override
        public void onEndOfSpeech() {
            showTip("结束说话");
        }

        //返回结果
        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            System.out.println(recognizerResult.getResultString());
            printResult(recognizerResult);
        }

        @Override
        public void onError(SpeechError speechError) {

        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    //输出结果
    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }


        editText.setText(resultBuffer.toString());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
        database = null;
        dbHelper.close();
        dbHelper = null;
    }

    private boolean saveNote2Database(String content,Priority priority) {
        // 插入一条新数据，返回是否插入成功
        if (database == null || TextUtils.isEmpty(content)) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(TodoContract.TodoNote.COLUMN_CONTENT, content);
        values.put(TodoContract.TodoNote.COLUMN_STATE, State.TODO.intValue);
        values.put(TodoContract.TodoNote.COLUMN_DATE, System.currentTimeMillis());
        values.put(TodoContract.TodoNote.COLUMN_PRIORITY, priority.intValue);
        // rowId 插入的行号
        long rowId = database.insert(TodoContract.TodoNote.TABLE_NAME, null, values);
        return rowId != -1;
    }

    private Priority getSelectedPriority() {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.btn_high:
                return Priority.High;
            case R.id.btn_medium:
                return Priority.Medium;
            default:
                return Priority.Low;
        }
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }
}
