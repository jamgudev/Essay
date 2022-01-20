package com.jamgu.home;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.jamgu.base.util.JLog;
import com.jamgu.common.Common;
import com.jamgu.home.Schemes.SettingPage;
import com.jamgu.krouter.annotation.KRouter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@KRouter(value = SettingPage.HOME_NAME, intParams = {SettingPage.USER_ID, SettingPage.GAME_ID}, stringParams = {SettingPage.USER_NAME})
public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
    }
}