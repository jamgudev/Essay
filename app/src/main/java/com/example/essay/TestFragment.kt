package com.example.essay

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jamgu.home.Schemes.HomePage
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.krouter.annotation.MethodRouter
import com.jamgu.krouter.core.router.IRouterInterceptor
import com.jamgu.krouter.core.router.IRouterMonitor
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters
import com.jamgu.krouter.core.router.KRouters.openForResult
import com.jamgu.krouter.core.router.KRouters.registerGlobalInterceptor


@KRouter(["test_fragment"], intParams = ["intParam"])
class TestFragment : Fragment() {

    companion object {

        private const val TAG = "TestFragment"

        @MethodRouter("show_testFragment")
        @JvmStatic
        fun show(map: Map<Any, Any>) {
            Log.d(TAG, "map in $TAG = $map")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            view?.findViewById<TextView>(R.id.tv_test_fragment)?.text =
                getInt("intParam").toString()
        }
        openForResult(
            requireContext(), KRouterUriBuilder("helper")
                    .appendAuthority(HomePage.HOST_NAME)
                    .with(HomePage.USER_ID, "12345")
                    .with(HomePage.GAME_ID, "10001")
                    .with(HomePage.USER_NAME, "jamgu")
                    .build(), 10
        )

        KRouters.open(requireContext(), "uri", null, object : IRouterMonitor {
            override fun beforeOpen(context: Context, uri: Uri): Boolean {
                return super.beforeOpen(context, uri)
            }

            override fun afterOpen(context: Context, uri: Uri) {
                super.afterOpen(context, uri)
            }

            override fun onError(context: Context, msg: String, e: Throwable?) {
                super.onError(context, msg, e)
            }
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

}