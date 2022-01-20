package com.example.essay;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.jamgu.home.Schemes.HomePage;
import com.jamgu.home.Schemes.MainPage;
import com.jamgu.home.Schemes.MainPage2;
import com.jamgu.krouter.annotation.KRouter;
import com.jamgu.krouter.core.method.IAsyncMethodCallback;
import com.jamgu.krouter.core.method.MethodMapBuilder;
import com.jamgu.krouter.core.method.MethodRouters;
import com.jamgu.krouter.core.router.IRouterInterceptor;
import com.jamgu.krouter.core.router.KRouterUriBuilder;
import com.jamgu.krouter.core.router.KRouters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@KRouter(MainPage.HOST_NAME)
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate ");

        findViewById(R.id.btn2Home).setOnClickListener(v -> {
//                KRouters.openForResult(MainActivity.this, new KRouterUriBuilder("helper")
//                        .appendAuthority(MainPage.HOST_NAME)
//                        .with(HomePage.USER_ID, "12345")
//                        .with(HomePage.GAME_ID, "10001")
//                        .with(HomePage.USER_NAME, "jamgu")
//                        .build(), 10);
                    MainActivity context = MainActivity.this;
                    Intent intent = new Intent(context, MainActivity2.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);
                }
        );

        Fragment testFragment = KRouters.createFragment(this, new KRouterUriBuilder("helper")
                .appendAuthority("test_fragment")
                .with("intParam", 123).build());
//        int intParam = testFragment.getArguments().getInt("intParam");
//        Log.d(TAG, "testFragment = " + testFragment + "args = " + intParam);

        KRouters.registerGlobalInterceptor(new IRouterInterceptor() {
            @Override
            public boolean intercept(@NotNull Uri uri, @Nullable Bundle bundle) {
                Log.d(TAG, "uri = " + uri + " bundle = " + bundle);
                return false;
            }
        });

        MethodRouters.invoke("get_some_state2");
        MethodRouters.invoke("get_some_state2", new MethodMapBuilder()
                .with("userId", "12345")
                .with("userName", "jamgu")
                .build());
        MethodRouters.invoke("get_some_state2", new MethodMapBuilder()
                .with("userId", "12345")
                .with("userName", "jamgu")
                .build(), new IAsyncMethodCallback<Integer>() {
            @Override
            public void onInvokeFinish(Integer integer) {
                // 供其他地方指定的回调
            }
        });

//        Test.analysisString("199901,10;200001,11.5;200101,12;200201,11.5;200301,9.5;200401,10.0;200501,12;200601,12.5;200701,12.7;200801,-5;200901,10");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
