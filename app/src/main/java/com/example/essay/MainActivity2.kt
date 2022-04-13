package com.example.essay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jamgu.home.Schemes
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters

@KRouter(value = [Schemes.MainPage2.HOST_NAME])
class MainActivity2 : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        Log.d(TAG, "onCreate")
        findViewById<View>(R.id.btnJump).setOnClickListener { v: View? ->
//            KRouters.openForResult(
//                this@MainActivity2, KRouterUriBuilder("helper")
//                        .appendAuthority(Schemes.MainPage.HOST_NAME)
//                        .with(HomePage.USER_ID, "12345")
//                        .with(HomePage.GAME_ID, "10001")
//                        .with(HomePage.USER_NAME, "jamgu")
//                        .build(), 10
//            )

            KRouters.open(this, KRouterUriBuilder("app_name")
                        .appendAuthority(Schemes.ViewEventPage.HOST_NAME3).build())

//            val intent = Intent(this, MainActivity::class.java)
//            this.startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}