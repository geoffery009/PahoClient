package com.test.my.pahoclient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mqtt连接service；和消息Notification提醒；SendMessageActivity与service通过EventBus交互消息(发布信息广播测试[不用])
 * 服务器部署见：http://www.itnose.net/detail/6652162.html
 */
public class MqttService extends Service {
    public static final String BROARCAST_PUBLIC_MSG = "public_server_mssage";
    public static final String BROARCAST_RECEIVE_MSG = "receice_mssage";
    public static final String KEY_RECEIVE_MSG = "key_receice_mssage";

    private final static String TAG = MqttService.class.getName();

    private MqttClient mqttClient;
    private static String HOST = "tcp://192.168.5.161:61613";//局域网
    private static String clientId = "client02";
    private static String username = "admin";
    private static String password = "password";
    private static String topic = "topic";


    private MqttConnectOptions options;
    private ScheduledExecutorService executors;

    public MqttService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        init();
        startConnect();
//        testReceiveMessageFromClient();
        EventBus.getDefault().register(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void init() {

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
                //重连
                connect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String str = new String(message.getPayload());
                publicMessage(str);
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
        options.setUserName(username);
        options.setPassword(password.toCharArray());
    }

    private void startConnect() {
        Log.e(TAG, "开始连接");

        new Thread() {
            public void run() {

                executors = Executors.newSingleThreadScheduledExecutor();
                executors.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        connect();
                    }
                }, 0 * 1000, 10 * 1000, TimeUnit.MILLISECONDS);
            }
        }.start();
    }

    private void connect() {
        try {//没有异常即为成功
            mqttClient.connect(options);
            subscribe();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe() throws MqttException {
        mqttClient.subscribe(topic, 1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //重新启动
        if (executors != null && mqttClient != null) {
            try {
                executors.shutdown();
                mqttClient.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            startConnect();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (executors != null && mqttClient != null) {
            try {
                executors.shutdown();
                mqttClient.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    private void publicMessage(String message) {

        initNotification(message);
        initUI(message);
    }

    private void initUI(String message) {
        MessageEvent event = new MessageEvent("activity", message);
        EventBus.getDefault().post(event);
    }

    private void initNotification(String message) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("收到广播消息");
        builder.setContentText(message);

        Intent intent = new Intent(this, MainActivity.class);

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setAutoCancel(true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this
                , 1, intent, 0);
        builder.setContentIntent(pendingIntent);
        builder.setFullScreenIntent(pendingIntent, true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(1, builder.getNotification());
    }

    private void sendBroadcast() {
        Intent intent = new Intent();
        intent.setAction(BROARCAST_PUBLIC_MSG);
        sendBroadcast(intent);
    }

    private void testReceiveMessageFromClient() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROARCAST_RECEIVE_MSG);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String string = intent.getStringExtra(KEY_RECEIVE_MSG);
                try {
                    if (TextUtils.isEmpty(string)) return;
                    MqttMessage message = new MqttMessage();
                    message.setQos(0);
                    message.setPayload(string.getBytes());
                    mqttClient.publish("topic", message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, filter);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (event.getType().equals("service")) {
            String string = event.getMessage();
            try {
                if (TextUtils.isEmpty(string)) return;
                MqttMessage message = new MqttMessage();
                message.setQos(0);
                message.setPayload(string.getBytes());
                mqttClient.publish("topic", message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
