package com.htrtan.mqtttest;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

public class MainActivity extends AppCompatActivity {

    List<String> aArray = new ArrayList<String>();
    Boolean stachange = false;
    String DATA = null;
    String DATAtemp = null;
    MQTT mqtt = new MQTT();
    CallbackConnection connectionL;

    LinearLayout LL_Login;
    EditText ET_SendID;
    EditText ET_GetID;
    EditText ET_IP;
    EditText ET_DuanKou;
    TextView TV_Ent;
    LinearLayout LL_Main;
    TextView TV_Main_ShowText;

    EditText ET_Main_SendText;
    TextView TV_Main_Send;

    private String IP;
    private String DuanKou;
    private String SendID;
    private String GetID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //定义控件
        LL_Login = (LinearLayout) findViewById(R.id.LL_Login);
        ET_SendID = (EditText) findViewById(R.id.ET_SendID);
        ET_GetID = (EditText) findViewById(R.id.ET_GetID);
        TV_Ent = (TextView) findViewById(R.id.TV_Ent);
        LL_Main = (LinearLayout) findViewById(R.id.LL_Main);
        TV_Main_ShowText = (TextView) findViewById(R.id.TV_Main_ShowText);
        ET_Main_SendText = (EditText) findViewById(R.id.ET_Main_SendText);
        TV_Main_Send = (TextView) findViewById(R.id.TV_Main_Send);
        ET_IP = (EditText) findViewById(R.id.ET_IP);
        ET_DuanKou = (EditText) findViewById(R.id.ET_DuanKou);

        //初始化
        LL_Login.setVisibility(View.VISIBLE);
        LL_Main.setVisibility(View.GONE);
        TV_Main_ShowText.setText("初始化中....");


        //监控事件
        TV_Ent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if((!"".equals(ET_SendID.getText().toString().trim())) && (!"".equals(ET_GetID.getText().toString().trim())) && (!"".equals(ET_IP.getText().toString().trim())) && (!"".equals(ET_DuanKou.getText().toString().trim()))){
                    SendID = ET_SendID.getText().toString().trim();
                    GetID = ET_GetID.getText().toString().trim();
                    IP = ET_IP.getText().toString().trim();
                    DuanKou = ET_DuanKou.getText().toString().trim();
                    LL_Login.setVisibility(View.GONE);
                    LL_Main.setVisibility(View.VISIBLE);
                    try {
                        ConnectMQTT();
//                        ConnectMQTT_Thread();
                    } catch (URISyntaxException e) {
                        TV_Main_ShowText.setText(TV_Main_ShowText.getText()+"\n"+"发送错误 原因:"+e.toString());
                    }
                }
            }
        });
        TV_Main_Send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!"".equals(ET_Main_SendText.getText().toString().trim())){
                    sendMsg(SendID,ET_Main_SendText.getText().toString().trim());
                }
            }
        });
    }
    
    private void ConnectMQTT() throws URISyntaxException {
        TV_Main_ShowText.setText(TV_Main_ShowText.getText()+"\n"+"配置中....");
        String user = env("APOLLO_USER", "admin");
        String password = env("APOLLO_PASSWORD", "password");
        String host = env("APOLLO_HOST", IP);
        int port = Integer.parseInt(env("APOLLO_PORT",DuanKou));
        TV_Main_ShowText.setText(TV_Main_ShowText.getText()+"\n"+"正在连接"+host+":"+Integer.toString(port));

        final String destinationL = GetID;

        mqtt.setHost(host, port);
        mqtt.setUserName(user);
        mqtt.setPassword(password);

        mqtt.setWillRetain(true);
        mqtt.setWillTopic(SendID);
        mqtt.setWillMessage("We are out!");
        TV_Main_ShowText.setText(TV_Main_ShowText.getText()+"\n"+"发布主题:"+SendID);
        TV_Main_ShowText.setText(TV_Main_ShowText.getText()+"\n"+"订阅主题:"+GetID);

        connectionL = mqtt.callbackConnection();
        connectionL.listener(new Listener() {

            public void onConnected() {
                Message msg = Handler_message.obtainMessage();
                msg.what = 100;
                msg.obj = "MQTT连接成功";
                Handler_message.sendMessage(msg);
            }

            public void onDisconnected() {
                Message msg = Handler_message.obtainMessage();
                msg.what = 100;
                msg.obj = "MQTT连接断开";
                Handler_message.sendMessage(msg);
            }

            public void onFailure(Throwable value) {
                TV_Main_ShowText.setText(TV_Main_ShowText.getText()+"\n"+"MQTT连接失败 原因："+value.toString());
                Message msg = Handler_message.obtainMessage();
                msg.what = 100;
                msg.obj = "MQTT连接失败 原因："+value.toString();
                Handler_message.sendMessage(msg);
            }

            public void onPublish(UTF8Buffer topic, Buffer msg, Runnable ack) {
                Message msg2 = Handler_message.obtainMessage();
                msg2.what = 100;
                String usertopic = topic.toString();

                String body = msg.utf8().toString();
                if (body.length() >= 6) {
                    if (body.substring(0, 6).toString().equals("conGet")) {
                        aArray.add(body.substring(6));
                        stachange = true;
                        msg2.obj = usertopic + " join in!";
                    }
                    else {
                        msg2.obj = usertopic+"："+body;
                    }
                }
                else {
                    msg2.obj = usertopic+"："+body;
                }
                Handler_message.sendMessage(msg2);
            }
        });

        connectionL.connect(new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Topic[] topics = {new Topic(destinationL, QoS.AT_LEAST_ONCE)};
                connectionL.subscribe(topics, new Callback<byte[]>() {
                    public void onSuccess(byte[] qoses) {
                        Message msg = Handler_message.obtainMessage();
                        msg.what = 100;
                        msg.obj = "服务器连接成功";
                        Handler_message.sendMessage(msg);
                    }

                    public void onFailure(Throwable value) {
                        Message msg = Handler_message.obtainMessage();
                        msg.what = 100;
                        msg.obj = "服务器连接失败 原因："+value.toString();
                        Handler_message.sendMessage(msg);
                    }
                });
            }

            @Override
            public void onFailure(Throwable value) {
                Message msg = Handler_message.obtainMessage();
                msg.what = 100;
                msg.obj = "连接失败 原因："+value.toString();
                Handler_message.sendMessage(msg);
            }
        });
        // connection.disconnect().await();
    }


    private String env(String key, String defaultValue) {
        String rc = System.getenv(key);
        if (rc == null)
            return defaultValue;
        return rc;
    }

    private void sendMsg(String TemSendID , String TemText) {

        connectionL.publish(TemSendID, (TemText).getBytes(), QoS.AT_LEAST_ONCE, true, new Callback<Void>() {
            @Override
            public void onSuccess(Void arg0) {
                // TODO 自动生成的方法存根
                Message msg = Handler_message.obtainMessage();
                msg.what = 100;
                msg.obj = "消息发送成功--√";
                Handler_message.sendMessage(msg);
            }

            @Override
            public void onFailure(Throwable arg0) {
                // TODO 自动生成的方法存根
                Message msg = Handler_message.obtainMessage();
                msg.what = 100;
                msg.obj = "消息发送失败--×";
                Handler_message.sendMessage(msg);
                connectionL.disconnect(null);
            }
        });
    }

    final MyHandler Handler_message=new MyHandler(this);
    static class MyHandler extends Handler{
        private final WeakReference<MainActivity> mActivity;
        public MyHandler(MainActivity activity) {
            mActivity=new WeakReference<MainActivity>(activity);
        }
        public void handleMessage(android.os.Message msg) {
            MainActivity activity=mActivity.get();
            if(activity!=null){
                // TODO Auto-generated method stub
                switch (msg.what){
                    case 100:{
                        activity.TV_Main_ShowText.setText(activity.TV_Main_ShowText.getText()+"\n"+(String)msg.obj);
                        break;
                    }
                }
            }
        };
    };

}
