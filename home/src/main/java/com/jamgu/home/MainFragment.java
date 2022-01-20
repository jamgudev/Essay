package com.jamgu.home;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.jamgu.krouter.annotation.KRouter;
import com.jamgu.krouter.annotation.MethodRouter;
import com.jamgu.krouter.core.method.IAsyncMethodCallback;
import com.jamgu.krouter.core.router.KRouters;
import java.util.Map;

@KRouter(value = "main_fragment", intParams = "col_id")
public class MainFragment extends Fragment {

    @MethodRouter(value = "get_some_state2")
    public static boolean getSomeState(Map<String, Object> map, IAsyncMethodCallback<Integer> callback) {
        // do something
        String userName = (String) map.get("userName");
        long userId = Long.parseLong(map.get("userId").toString());
        Log.d("jamgu", "hhhh userID = " + userId);
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }
}