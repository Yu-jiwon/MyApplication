package com.example.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

import android.os.Handler;

public class MainActivity extends AppCompatActivity {

    private BluetoothSPP bt;
    MainHandler mainHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt = new BluetoothSPP(this); //Initializing

        if (!bt.isBluetoothAvailable()) { //블루투스 사용 불가
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }

        TextView temp1 = findViewById(R.id.temp);
        Button button1 = findViewById(R.id.button2);
        button1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                BackgroundThread thread = new BackgroundThread();
                thread.start();
            }
        });
        mainHandler = new MainHandler();
        // ------------------------------ 데이터 수신부 ----------------------------- //
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() { //데이터 수신
            TextView temp = findViewById(R.id.temp);
            TextView humd = findViewById(R.id.humd);

            public void onDataReceived(byte[] data, String message) {
                String[] array = message.split(",");
                temp.setText(array[0].concat("C"));
                humd.setText(array[1].concat("%") );
            }
        });
        // ------------------------------ 데이터 수신부 ----------------------------- //

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() { //연결됐을 때
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() { //연결해제
                Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() { //연결실패
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnConnect = findViewById(R.id.button1); //연결시도
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                    bt.disconnect();
                } else {
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                }
            }
        });
    }

    class BackgroundThread extends Thread{
        int value = 0;
        public void run(){
            for(int i = 0; i<100;i++){
                try{
                    Thread.sleep(1000);
                }catch (Exception e){}
                value +=1;
                Log.d("Thread","value : " +value);

                Message message = mainHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putInt("value", value);
                message.setData(bundle);
                mainHandler.sendMessage(message);
            }
        }
    }

    class MainHandler extends  Handler{
        @Override
        public  void handleMessage(@NonNull Message msg){
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            int value = bundle.getInt("value:");
            temp1.setText("value:" + value);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        bt.stopService(); //블루투스 중지
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) { //
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER); //DEVICE_ANDROID는 안드로이드 기기 끼리
                setup();
            }
        }
    }

    public void setup() {
        Button btnSend = findViewById(R.id.button2); //데이터 전송
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bt.send("Text", true);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}