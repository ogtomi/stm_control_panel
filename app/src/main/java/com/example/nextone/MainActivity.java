package com.example.nextone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Spinner menuSpinner;
    ListView pairedDevListView;
    Switch bluetoothSwitch;
    BluetoothAdapter myBluetoothAdapter;
    Intent bluetoothEnablingIntent;
    TextView connectionStatusText, encoderText, speed1Text, torque1Text, voltage1Text, current1Text, power1Text, overload1Text, speed2Text, torque2Text, voltage2Text, current2Text, power2Text, overload2Text, gyroXText, gyroYText, gyroZText, msgCountText;
    SeekBar rightEnSeekBar, leftEnSeekBar;
    LinearLayout steerLayout, connectLayout, buttonLayout;
    Button redLed, orangeLed, blueLed, greenLed, stopButton;

    SendReceive sendReceive;
    BluetoothDevice[] bluetoothPairedDevArray;

    ArrayList<String> discoverDeviceArrayList = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;
    BluetoothAdapter myAdapter = BluetoothAdapter.getDefaultAdapter();

    //SEEKBARS MESSAGES
    String leftBarMsg = "50";
    String rightBarMsg = "50";
    String messageToSend = "5050";

    //LED MESSAGES
    String redLedMsg = "1";
    String blueLedMsg = "1";
    String orangeLedMsg = "1";
    String greenLedMsg = "1";
    String ledMsg = "1111";

    String redLedPrev = "0";
    String blueLedPrev = "0";
    String orangeLedPrev = "0";
    String greenLedPrev = "0";

    String messageToSendPrev = messageToSend;
    String ledMsgPrev = "0000";

    int REQUEST_ENABLE_BLUETOOTH = 1;
    int requestCodeForEnable;

    //HANDLER STATES
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;

    private static final String APP_NAME = "STM32";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what){
                case STATE_CONNECTING:
                    connectionStatusText.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    connectionStatusText.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    connectionStatusText.setText("Connection failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) message.obj;
                    String tempMsg = new String(readBuff, 0 , message.arg1);
                    String[] splitMsg = tempMsg.split(" ");

                    if(splitMsg.length == 17) {
                        msgCountText.setText("Msg: " + splitMsg[0]);
                        encoderText.setText(splitMsg[1]);
                        voltage1Text.setText("U1: " + String.valueOf(Integer.parseInt(splitMsg[2])/100) + " [V]");
                        voltage2Text.setText("U2: " + String.valueOf(Integer.parseInt(splitMsg[3])/100) + " [V]");
                        current1Text.setText("I1: " + String.valueOf(Integer.parseInt(splitMsg[4])/100) + " [A]");
                        current2Text.setText("I2: " + String.valueOf(Integer.parseInt(splitMsg[5])/100) + " [A]");
                        power1Text.setText("P1: " + String.valueOf(Integer.parseInt(splitMsg[6])/100) + " [W]");
                        power2Text.setText("P2: " + String.valueOf(Integer.parseInt(splitMsg[7])/100) + " [W]");
                        speed1Text.setText("V1: " + splitMsg[8] + " [obr/min]");
                        speed2Text.setText("V2: " + splitMsg[9] + " [obr/min]");
                        torque1Text.setText("T1: " + String.valueOf(Integer.parseInt(splitMsg[10])/100) + " [Nm]");
                        torque2Text.setText("T2: " + String.valueOf(Integer.parseInt(splitMsg[11])/100) + " [Nm]");
                        overload1Text.setText("OVERLOAD1: " + splitMsg[12]);
                        overload2Text.setText("OVERLOAD1: " + splitMsg[13]);
                        gyroXText.setText(splitMsg[14]);
                        gyroYText.setText(splitMsg[15]);
                        gyroZText.setText(splitMsg[16]);
                    }
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //setting activity_main as window

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //getting device's bluetooth adapter
        bluetoothEnablingIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        requestCodeForEnable = 1;

        if(!myBluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //if bluetooth is not enabled
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH); //start enabling bluetooth
        }

        findViewByIds();
        implementListeners();

        //SPINNER - MENU
        ArrayList<String> menuList = new ArrayList<>();
        menuList.add("Connection");
        menuList.add("Steering");
        menuList.add("Buttons");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, menuList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); //adding menuList to adapter
        menuSpinner.setAdapter(adapter);
    }

    private void findViewByIds() {
        pairedDevListView = findViewById(R.id.pairedDevicesListView);
        bluetoothSwitch = findViewById(R.id.btSwitch);
        connectionStatusText = findViewById(R.id.statusTextView);
        encoderText = findViewById(R.id.encoderTextView);
        rightEnSeekBar = findViewById(R.id.rightSeekBar);
        leftEnSeekBar = findViewById(R.id.leftSeekBar);
        menuSpinner = findViewById(R.id.spinner);
        steerLayout = findViewById(R.id.steeringLayout);
        connectLayout = findViewById(R.id.connectionLayout);
        buttonLayout = findViewById(R.id.buttonLayout);
        redLed = findViewById(R.id.redLedButton);
        blueLed = findViewById(R.id.blueLedButton);
        orangeLed = findViewById(R.id.orangeLedButton);
        greenLed = findViewById(R.id.greenLedButton);
        stopButton = findViewById(R.id.stopButton);
        voltage1Text = findViewById(R.id.voltage1TextView);
        voltage2Text = findViewById(R.id.voltage2TextView);
        torque1Text = findViewById(R.id.torque1TextView);
        torque2Text = findViewById(R.id.torque2TextView);
        speed1Text = findViewById(R.id.speed1TextView);
        speed2Text = findViewById(R.id.speed2TextView);
        current1Text = findViewById(R.id.current1TextView);
        current2Text = findViewById(R.id.current2TextView);
        power1Text = findViewById(R.id.power1TextView);
        power2Text = findViewById(R.id.power2TextView);
        overload1Text = findViewById(R.id.overload1TextView);
        overload2Text = findViewById(R.id.overload2TextView);
        gyroXText = findViewById(R.id.XTextView);
        gyroYText = findViewById(R.id.YTextView);
        gyroZText = findViewById(R.id.ZTextView);
        msgCountText = findViewById(R.id.messageNumberTextView);
    }

    private void implementListeners() {

        //CHANGING MENUS
        menuSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                switch(i){
                    case 0:
                        steerLayout.setVisibility(View.GONE);
                        connectLayout.setVisibility(View.VISIBLE);
                        buttonLayout.setVisibility(View.GONE);
                        break;

                    case 1:
                        steerLayout.setVisibility(View.VISIBLE);
                        connectLayout.setVisibility(View.GONE);
                        buttonLayout.setVisibility(View.GONE);
                        break;

                    case 2:
                        steerLayout.setVisibility(View.GONE);
                        connectLayout.setVisibility(View.GONE);
                        buttonLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });;

        //BLUETOOTH ENABLING SWITCH
        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    if(myBluetoothAdapter == null){
                        Toast.makeText(getApplicationContext(),"Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
                    }
                    else{
                        if(!myBluetoothAdapter.isEnabled()){
                            startActivityForResult(bluetoothEnablingIntent, requestCodeForEnable); //enabling bluetooth when switch is checked
                        }
                    }
                }
                else{
                    if(myBluetoothAdapter.isEnabled()){
                        myBluetoothAdapter.disable(); //disabling bluetooth when switch is not checked
                        Toast.makeText(getApplicationContext(), "Bluetooth is disabled", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        //CLICKING ON DEVICE FROM PAIRED DEVICE LIST
        pairedDevListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //connecting with device from paired devices list
                ClientClass clientClass = new ClientClass(bluetoothPairedDevArray[i]);
                clientClass.start();
                connectionStatusText.setText("Connecting");
            }
        });

        //SEEKBARS
        rightEnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if(i == 100)
                    i = 99; // values 0-99

                rightBarMsg = String.valueOf(i);

                try {
                    if(i < 10){
                        if(leftEnSeekBar.getProgress() < 10)
                            messageToSend = "0" + rightBarMsg + "0" + leftBarMsg;
                        else
                            messageToSend = "0" + rightBarMsg + leftBarMsg;

                        sendReceive.write((messageToSend + ledMsgPrev).getBytes("ASCII"));

                    }else {
                        if (leftEnSeekBar.getProgress() < 10)
                            messageToSend = rightBarMsg + "0" + leftBarMsg;
                        else
                            messageToSend = rightBarMsg + leftBarMsg;

                        sendReceive.write((messageToSend + ledMsgPrev).getBytes("ASCII"));
                    }
                        messageToSendPrev = messageToSend;
                        messageToSend = messageToSend.substring(0, 2);

                }catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }catch (NullPointerException re) {
                    re.printStackTrace();
                    //Message is sent when stm device is not connected
                    Toast.makeText(getApplicationContext(), "Connect STM device", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        leftEnSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if(i == 100)
                    i = 99;

                leftBarMsg = String.valueOf(i);

               try {
                    if(i < 10){
                        if(rightEnSeekBar.getProgress() < 10)
                            messageToSend = "0" + rightBarMsg + "0" + leftBarMsg;
                        else
                            messageToSend = rightBarMsg + "0" + leftBarMsg;

                        sendReceive.write((messageToSend + ledMsgPrev).getBytes("ASCII"));

                    }else {
                        if (rightEnSeekBar.getProgress() < 10)
                            messageToSend = "0" + rightBarMsg + leftBarMsg;
                        else
                            messageToSend = rightBarMsg + leftBarMsg;

                        sendReceive.write((messageToSend + ledMsgPrev).getBytes("ASCII"));
                    }
                        messageToSendPrev = messageToSend;
                        messageToSend = messageToSend.substring(2);

                }catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }catch (NullPointerException re) {
                    re.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Connect STM device", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        redLed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(redLedMsg == "1"){
                    try {
                        ledMsg = blueLedPrev + redLedMsg + orangeLedPrev + greenLedPrev;
                        ledMsgPrev = ledMsg;
                        sendReceive.write((messageToSendPrev + ledMsg).getBytes("ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    redLedPrev = redLedMsg;
                    redLedMsg = "0";
                }else{
                    try {
                        ledMsg = blueLedPrev + redLedMsg + orangeLedPrev + greenLedPrev;
                        ledMsgPrev = ledMsg;
                        sendReceive.write((messageToSendPrev + ledMsgPrev).getBytes("ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    redLedPrev = redLedMsg;
                    redLedMsg = "1";
                }
            }
        });

        blueLed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(blueLedMsg == "1"){
                    try {
                        ledMsg = blueLedMsg + redLedPrev + orangeLedPrev + greenLedPrev;
                        ledMsgPrev = ledMsg;
                        sendReceive.write((messageToSendPrev + ledMsgPrev).getBytes("ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    blueLedPrev = blueLedMsg;
                    blueLedMsg = "0";
                }else{
                    try {
                        ledMsg = blueLedMsg + redLedPrev + orangeLedPrev + greenLedPrev;
                        ledMsgPrev = ledMsg;
                        sendReceive.write((messageToSendPrev + ledMsgPrev).getBytes("ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    blueLedPrev = blueLedMsg;
                   blueLedMsg = "1";
                }
            }
        });

        orangeLed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(orangeLedMsg == "1"){
                    try {
                        ledMsg = blueLedPrev + redLedPrev + orangeLedMsg + greenLedPrev;
                        ledMsgPrev = ledMsg;
                        sendReceive.write((messageToSendPrev + ledMsg).getBytes("ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    orangeLedPrev = orangeLedMsg;
                    orangeLedMsg = "0";
                }else{
                    try {
                        ledMsg = blueLedPrev + redLedPrev + orangeLedMsg + greenLedPrev;
                        ledMsgPrev = ledMsg;
                        sendReceive.write((messageToSendPrev + ledMsg).getBytes("ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    orangeLedPrev = orangeLedMsg;
                    orangeLedMsg = "1";
                }
            }
        });

        greenLed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(greenLedMsg == "1"){
                    try {
                        ledMsg = blueLedPrev + redLedPrev + orangeLedPrev + greenLedMsg;
                        ledMsgPrev = ledMsg;
                        sendReceive.write((messageToSendPrev + ledMsg).getBytes("ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    greenLedPrev = greenLedMsg;
                    greenLedMsg = "0";
                }else{
                    try {
                        ledMsg = blueLedPrev + redLedPrev + orangeLedPrev + greenLedMsg;
                        ledMsgPrev = ledMsg;
                        sendReceive.write((messageToSendPrev + ledMsg).getBytes("ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    greenLedPrev = greenLedMsg;
                    greenLedMsg = "1";
                }
            }
        });

        // STOP BUTTON
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageToSend = "50500000";

                try {
                    sendReceive.write(messageToSend.getBytes("ASCII"));
                    Toast.makeText(getApplicationContext(), "Engines have been stopped", Toast.LENGTH_SHORT).show();
                    leftEnSeekBar.setProgress(50); //resetting seekbars
                    rightEnSeekBar.setProgress(50);
                    redLedMsg = blueLedMsg = orangeLedMsg = greenLedMsg = redLedPrev = blueLedPrev = orangeLedPrev = greenLedPrev = "0";
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }catch (NullPointerException re) {
                    re.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Connect STM device", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //SHOWING PAIRED DEVICES WHEN SWITCH IS CHECKED
    private void showPairedDevices() {
        Set<BluetoothDevice> btDevices = myBluetoothAdapter.getBondedDevices(); //getting paired devices
        bluetoothPairedDevArray = new BluetoothDevice[btDevices.size()]; //setting array size
        String[] deviceNames = new String[btDevices.size()];
        int index = 0;

        if(btDevices.size() > 0){
            for(BluetoothDevice device: btDevices){
                deviceNames[index] = device.getName(); //getting name of each device
                bluetoothPairedDevArray[index] = device; //putting device in array
                index++;
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,  deviceNames);
            pairedDevListView.setAdapter(arrayAdapter); //adding adapter to device list view
        }
    }

    //SHOWING POP UP MESSAGE
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == requestCodeForEnable){
            if(resultCode == RESULT_OK){
                Toast.makeText(getApplicationContext(), "Bluetooth is enabled", Toast.LENGTH_LONG).show();
                showPairedDevices();
            }
            else if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext(), "Bluetooth enabling cancelled", Toast.LENGTH_LONG).show();
            }
        }
    }

    //CLIENT CONNECTION CLASS
    private class ClientClass extends Thread{
        private BluetoothSocket socket;
        private BluetoothDevice device;

        public ClientClass(BluetoothDevice device1){
            this.device = device1; //associating device var with device1
            try {
                //communicating with device using UUID address
                socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            myBluetoothAdapter.cancelDiscovery();
            try {
                socket.connect(); //connecting with device using bluetooth socked
                Message message = Message.obtain();
                message.what = STATE_CONNECTED; //setting handler's state as connected
                handler.sendMessage(message); //sending message to handler

                //creating sendReceive instance to enable sending/ receiving messages
                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            } catch (NullPointerException re) {
                re.printStackTrace();
            }
        }
    }

    //SENDING/ RECEIVING MESSAGES CLASS
    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer); //reading received messages
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes); //sending out messages as byte array
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

