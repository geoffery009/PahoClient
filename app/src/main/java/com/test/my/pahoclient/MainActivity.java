package com.test.my.pahoclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private MqttClient mqttClient;
    private static String HOST = "tcp://192.168.5.161:61613";//局域网
    private static String clientId = "client02";
    private static final String TAG = MainActivity.class.getName();
    private EditText tvTopic;
    private EditText tvPublicMessage;
    private MqttConnectOptions options;
    private ScheduledExecutorService executors;
    private Button btnReconnect;
    private boolean reconnect;
    private Button btnSubcribe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EditText tvName = (EditText) findViewById(R.id.editText1);
        EditText tvPassword = (EditText) findViewById(R.id.editText2);
        tvTopic = (EditText) findViewById(R.id.editText3);
        tvPublicMessage = (EditText) findViewById(R.id.editText4);

        btnSubcribe = (Button) findViewById(R.id.button1);
        btnSubcribe.setOnClickListener(this);
        Button btnSendMessage = (Button) findViewById(R.id.button2);
        btnSendMessage.setOnClickListener(this);
        btnReconnect = (Button) findViewById(R.id.btnReconnect);
        btnReconnect.setOnClickListener(this);

        //模拟器
//        HOST = "tcp://10.0.2.2:61613";

        init(tvName.getText().toString(), tvPassword.getText().toString());
//        connect();
    }

    private void init(String userName, String passWord) {
        reconnect = false;
        btnReconnect.setText("开始连接");
        btnSubcribe.setClickable(false);

        try {
            mqttClient = new MqttClient(HOST, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "初始化失败");
        }
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e(TAG, "connect lost");
                Toast.makeText(MainActivity.this, "失去连接", Toast.LENGTH_LONG).show();
                //重连
                connect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String str = new String(message.getPayload());
                Toast.makeText(MainActivity.this, "收到消息：" + str, Toast.LENGTH_LONG).show();
                Log.e(TAG, "收到消息：" + str);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setKeepAliveInterval(20);
        options.setConnectionTimeout(10);
        options.setUserName(userName);
        options.setPassword(passWord.toCharArray());
    }

    private void connect() {
        Log.e(TAG, "开始连接");
        executors = Executors.newSingleThreadScheduledExecutor();
        executors.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {//没有异常即为成功
                    mqttClient.connect(options);
                    btnSubcribe.setClickable(true);
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.e(TAG, "连接失败");
                    btnSubcribe.setClickable(false);
                }
                reconnect = true;
                btnReconnect.setText("重新连接");
            }
        }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            executors.shutdown();
            mqttClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.button1:
                try {
                    mqttClient.subscribe(tvTopic.getText().toString(), 1);
                    Toast.makeText(MainActivity.this, "订阅成功", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "订阅成功");
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "订阅失败", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "订阅失败");
                }
                break;
            case R.id.button2:
                try {
                    mqttClient.publish(tvTopic.getText().toString(), tvPublicMessage.toString().trim().getBytes(), 0, false);
                    Log.e(TAG, "发布消息成功");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "发布消息失败");
                }
                break;
            case R.id.btnReconnect:
                if (reconnect) {
                    try {
                        executors.shutdown();
                        mqttClient.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                connect();
                break;
        }
    }
}
