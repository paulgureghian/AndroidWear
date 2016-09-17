package com.example.android.sunshine.app.other;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.android.sunshine.app.R;

public class MyStubBroadcastActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = new Intent();
        i.setAction("com.example.android.sunshine.app.SHOW_NOTIFICATION");
        i.putExtra(MyPostNotificationReceiver.CONTENT_KEY, getString(R.string.title));
        sendBroadcast(i);
        finish();
    }
}
