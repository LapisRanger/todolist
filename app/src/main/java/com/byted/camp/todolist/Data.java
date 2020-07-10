package com.byted.camp.todolist;

import android.app.Application;
import android.content.Context;

public class Data extends Application{
    private static Data instance;//单例
    private Integer default_audio;

    public Integer getDA(){
        return this.default_audio;
    }

    public void setDA(Integer integer){
        this.default_audio=integer;
    }

    @Override
    public void onCreate(){
        default_audio=1;
        instance=this;
        super.onCreate();
    }

    public static Context getMyData(){
        return instance;
    }
}
