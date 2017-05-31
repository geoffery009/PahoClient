# PahoClient
Eclipse Paho客户端测试

服务端搭建参照这个地址：
http://www.itnose.net/detail/6652162.html

APP端
MqttService.java 连接服务，订阅topic，分发消息（这里采用EventBus通知UI界面且弹出Notification提示消息）
SendMessageActivity.java 通过EventBus发布消息
MainActivity.java Notification点击弹出的界面
