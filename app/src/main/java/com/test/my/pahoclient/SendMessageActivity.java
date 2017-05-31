package com.test.my.pahoclient;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SendMessageActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        findViewById(R.id.button).setOnClickListener(this);
        EventBus.getDefault().register(this);
        startService();
    }

    private void startService() {
        startService(new Intent(this, MqttService.class));
    }

    private void sendMsg(String message) {
        sendEventBus(message);
    }

    private void sendEventBus(String message) {
        MessageEvent event = new MessageEvent("service", message);
        EventBus.getDefault().post(event);
    }

    private void sendBroad(String message) {
        Intent intent = new Intent();
        intent.setAction(MqttService.BROARCAST_RECEIVE_MSG);
        intent.putExtra(MqttService.KEY_RECEIVE_MSG, message);
        sendBroadcast(intent);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.button:
                String str = ((EditText) findViewById(R.id.editText)).getText().toString();
                if (TextUtils.isEmpty(str)) return;
                sendMsg(str);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (event.getType().equals("activity")) {
            ((TextView) findViewById(R.id.textView4)).setText(event.getMessage());
        }
    }

    ;

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
