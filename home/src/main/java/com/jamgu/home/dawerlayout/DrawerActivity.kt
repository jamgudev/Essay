package com.jamgu.home.dawerlayout

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jamgu.base.widget.dp2px
import com.jamgu.base.widget.getScreenHeight
import com.jamgu.base.widget.getScreenWidth
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.statusbar.StatusBarUtil
import com.jamgu.home.R
import com.jamgu.home.Schemes
import com.jamgu.home.databinding.ActivityDrawerBinding
import com.jamgu.home.dawerlayout.custom.CustomDrawerLayout
import com.jamgu.krouter.annotation.KRouter
import kotlin.math.roundToInt


/**
 * QQ 主页抽屉动效实现
 */
@KRouter(value = [Schemes.DrawerPage.HOME_NAME])
class DrawerActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "DrawerActivity"
    }

    private lateinit var binding: ActivityDrawerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.title = "标题"
        setSupportActionBar(binding.toolbar)

        binding.toolbar.setNavigationIcon(R.drawable.icon_head)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.LEFT, true)
        }

        setStatusBar()
        setDrawerLayout()
    }

    private fun setDrawerLayout() {
        val factor = getScreenWidth(this) * 1f / getScreenHeight(this)
        val topMargin = 36.dp2px(this)
        val leftMargin = (topMargin + 6 ) * factor
        val radius = 20.dp2px(this)
        val toolbarDrawable = GradientDrawable()
        toolbarDrawable.shape = GradientDrawable.RECTANGLE
        toolbarDrawable.setColor(Color.parseColor("#1ABDE6"))
        binding.toolbar.background = toolbarDrawable

        val contentDrawable = GradientDrawable()
        contentDrawable.shape = GradientDrawable.RECTANGLE
        contentDrawable.setColor(Color.WHITE)
        binding.contentContainer.background = contentDrawable

        binding.drawerLayout.addDrawerListener(object : CustomDrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

                JLog.d(TAG, "offset = $slideOffset")
//                binding.mainContainer.translationX = (drawerView.width * slideOffset)

                val windowRadius = radius * slideOffset
                val tDb = binding.toolbar.background as? GradientDrawable
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    tDb?.cornerRadii = floatArrayOf(windowRadius, windowRadius, windowRadius, windowRadius, 0f, 0f, 0f, 0f)
                    binding.toolbar.background = tDb
                }

                val cDb = binding.contentContainer.background as? GradientDrawable
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    cDb?.cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, windowRadius, windowRadius, windowRadius, windowRadius)
                    binding.contentContainer.background = cDb
                }

                val layoutParams = binding.mainContainer.layoutParams as? CustomDrawerLayout.LayoutParams
                layoutParams?.let {
                    it.topMargin = (topMargin * slideOffset).roundToInt()
                    it.bottomMargin = (topMargin * slideOffset).roundToInt()
                    it.leftMargin = (leftMargin * slideOffset).roundToInt()
                    it.rightMargin = (leftMargin * slideOffset).roundToInt()
                }
                binding.mainContainer.layoutParams = layoutParams
            }
        })
    }

    private fun setStatusBar() {
        StatusBarUtil.fitStatusLayout(this, binding.toolbar, true)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.repeatCount == 0) {
            // 抽屉如果是打开状态，先关闭抽屉
            if (binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
                binding.drawerLayout.closeDrawer(Gravity.LEFT)
                return false
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}