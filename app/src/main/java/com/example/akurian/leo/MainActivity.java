package com.example.akurian.leo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.widget.ListViewAutoScrollHelper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;


public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    TextView textViewLabel;
    EditText myTextbox;
    BluetoothDevice bluetoothDevice;
    InputStream mmInputStream;
    OutputStream mmOutputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    BluetoothSocket mmSocket;
    Vibrator vibrator;
    ListView pressureListView;
    ListView temperatureListView;
    ToneGenerator toneGen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textViewLabel = (TextView) findViewById(R.id.textView);
        Button moveButton = (Button) findViewById(R.id.moveButton);
        Button stopButton = (Button) findViewById(R.id.stopButton);
        Button leftButton = (Button) findViewById(R.id.leftButton);
        Button rightButton = (Button) findViewById(R.id.rightButton);
        Button calibrateButton = (Button) findViewById(R.id.calibrateButton);



        /*LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.header, pressureListView,
                false);
        pressureListView.addHeaderView(header,null,false);*/
        /*Vibrator vibrator;
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(10000);
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
        textViewLabel.setText("HI");
        Camera cam = Camera.open();
        Camera.Parameters p = cam.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        cam.setParameters(p);
        cam.startPreview();
        try{
            Thread.sleep(3000);
        }
        catch (InterruptedException e){}
        cam.stopPreview();
        cam.release();*/


        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    if (isChecked) {
                        findBT();
                        openBT();
                    } else {
                        closeBT();
                    }
                }
                catch (IOException e){}
            }
        });

        moveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendData("W");
                } catch (IOException ex) {
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    sendData("S");
                }
                catch (IOException ex) { }
            }
        });

        leftButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    sendData("A");
                }
                catch (IOException ex) { }
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendData("D");
                } catch (IOException ex) {
                }
            }
        });

        calibrateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendData("C");
                } catch (IOException ex) {
                }
            }
        });


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set pairedDevices = mBluetoothAdapter.getBondedDevices();
        Iterator iter = pairedDevices.iterator();
        while(iter.hasNext()) {
            bluetoothDevice = (BluetoothDevice) iter.next();
            if (bluetoothDevice.getName().equals("HC-05")) {
                Log.e("Entered", "func");
                break;
            }
        }
    }

    void openBT()throws IOException{
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        //myLabel.setText("Bluetooth Opened");
    }

    void sendData(String msg) throws IOException{
        //String msg = myTextbox.getText().toString();
        //msg += "\n";
        mmOutputStream.write(msg.getBytes());
        textViewLabel.setText("Data Sent");
    }

    void beginListenForData()
    {
        final Handler handler=new Handler();
        final byte delimiter = 10;
        final String emptyMsg=" ";
        textViewLabel = (TextView) findViewById(R.id.textView);
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        pressureListView = (ListView) findViewById(R.id.listView1);
        temperatureListView = (ListView) findViewById(R.id.listView2);
        final ArrayAdapter<String> adapter;
        ArrayList<String> pressureList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, pressureList);
        pressureListView.setAdapter(adapter);
        pressureListView.setSelection(adapter.getCount() - 1);
        final ArrayAdapter<String> adapter2;
        ArrayList<String> tempList = new ArrayList<String>();
        adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tempList);
        temperatureListView.setAdapter(adapter2);
        temperatureListView.setSelection(adapter2.getCount() - 1);
        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);


        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            if(data.startsWith("C")) {
                                                textViewLabel.setText(data);
                                                adapter.add("F-77");

                                                vibrator.vibrate(3000);

                                            }
                                            else {
                                                adapter.add("F-56");
                                                textViewLabel.setText("NO CRACK");
                                            }

                                            if(data.startsWith("H")) {
                                                textViewLabel.setText(data);
                                                toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 4000);
                                                textViewLabel.setText(emptyMsg);

                                            }
                                            else if(data.startsWith("T")){
                                                //textViewLabel.setText(data);
                                                adapter2.add(data);
                                            }
                                            pressureListView.setSelection(adapter.getCount() - 1);
                                            temperatureListView.setSelection(adapter2.getCount() - 1);

                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }


    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        //myLabel.setText("Bluetooth Closed");
    }

}
