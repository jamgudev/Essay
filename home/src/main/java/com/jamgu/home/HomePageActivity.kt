package com.jamgu.home

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jamgu.home.Schemes.HomePage.GAME_ID
import com.jamgu.home.Schemes.HomePage.HOST_NAME
import com.jamgu.home.Schemes.HomePage.USER_ID
import com.jamgu.home.Schemes.HomePage.USER_NAME
import com.jamgu.home.databinding.ActivityHomePageBinding
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.krouter.annotation.MethodRouter
import com.jamgu.krouter.core.router.KRouters

@KRouter(
    [HOST_NAME], longParams = [USER_ID, GAME_ID],
    stringParams = [USER_NAME]
)
class HomePageActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomePageActivity"

        @MethodRouter("showHomePageActivity")
        fun showHomePageActivity(map: Map<Any, Any>): Boolean {
            Log.d(TAG, "map in $TAG = $map")
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.extras?.apply {
            binding.tvGameId.text = getLong(GAME_ID, 0).toString()
            binding.tvUserId.text = getLong(USER_ID, 0).toString()
            binding.tvUserName.text = getString(USER_NAME, "")
        }

        binding.btn2Main.setOnClickListener {
            KRouters.open(this, Schemes.MainPage.HOST_NAME)
        }

    }
}
