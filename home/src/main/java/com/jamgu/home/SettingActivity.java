package com.jamgu.home;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.jamgu.home.Schemes.SettingPage;
import com.jamgu.krouter.annotation.KRouter;

@KRouter(value = SettingPage.HOME_NAME, intParams = {SettingPage.USER_ID, SettingPage.GAME_ID}, stringParams = {SettingPage.USER_NAME})
public class SettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
    }
}