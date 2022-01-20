package com.jamgu.home.viwemodel

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.ViewModelProvider
import com.jamgu.home.R
import com.jamgu.krouter.annotation.KRouter

const val HOST_NAME = "view_model"

private const val TAG = "ViewModelActivity"

@KRouter([HOST_NAME])
class ViewModelActivity : AppCompatActivity() {

    private lateinit var vNumber: AppCompatTextView
    private lateinit var mViewModel: UserModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_model)
        Log.d(TAG, "onCreate called")
        val viewModelProvider = ViewModelProvider(this)
        mViewModel = viewModelProvider.get(UserModel::class.java)

        vNumber = findViewById(R.id.tv_number)

        findViewById<AppCompatButton>(R.id.btn_add).setOnClickListener {
            mViewModel.count++
            setCounter()
        }
        setCounter()
    }

    private fun setCounter() {
        vNumber.text = mViewModel.count.toString()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
    }

}