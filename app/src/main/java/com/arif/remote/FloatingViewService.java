package com.arif.remote;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class FloatingViewService extends Service implements View.OnClickListener {

    private static final int SYSTEM_ALERT_WINDOW_PERMISSION = 2084;

    private WindowManager mWindowManager;
    private View mFloatingView;
    private View collapsedView;
    private View expandedView;

    //private static final String TAG = "BlueTest5-Controlling-Service";
    private int mMaxChars = 50000;//Default//change this to string..........

    //private BluetoothSocket mBTSocket;
    /** socketConnect*/
    //SocketHelper helpMe = new SocketHelper();
    public BluetoothDevice mDevice;// = helpMe.getmDevice();
    public BluetoothSocket mBTSocket;// = helpMe.getSuperSocket();
    public UUID mDeviceUUID;// = helpMe.getmDeviceUUID();
    /** socketConnect*/
    private ReadInput mReadThread = null;
    //private final IBinder binder = new LocalBinder();

    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;



    final static String forward="F";//forward-F
    final static String backward="B";//backward-B
    final static String left="L";//left-L
    final static String right="R";//right-R
    final static String goUp="U";//goUp-U
    final static String goDown="D";//goDown-D
    final static String stop="S";//stop-S

    //private ProgressDialog progressDialog;
    ImageButton btnForward, btnBackward, btnLeft, btnRight, btnGoUp, btnGoDown, btnStop;

    public FloatingViewService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        /*Bundle b=intent.getExtras();
        mDevice=b.getParcelable("mDevice");
        mDeviceUUID=b.getParcelable("mDeviceUUID");
        wip welp=new wip();
        welp.setmDeviceUUID(mDeviceUUID);
        welp.setmDevice(mDevice);
        return binder;*/
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(),this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    /*public class LocalBinder extends Binder {
        FloatingViewService getService() {
            // Return this instance of LocalService so clients can call public methods
            return FloatingViewService.this;
        }
    }*/
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        super.onCreate();
        //Intent intent = getIntent();
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable("mDevice");
        mDeviceUUID = UUID.fromString(b.getString("mDeviceUUID"));

        /*return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();*/

        //new ReadInput();

        //setContentView(R.layout.layout_floating_widget);
        //button id with Button
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            askPermission();
        }

        if (mBTSocket == null || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        //getting the widget layout from xml using layout inflater
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);


        //ActivityHelper.initialize(this);
        // mBtnDisconnect = (Button) findViewById(R.id.btnDisconnect);

        btnForward =(ImageButton) mFloatingView.findViewById(R.id.forward);
        btnBackward =(ImageButton) mFloatingView.findViewById(R.id.backward);
        btnLeft =(ImageButton) mFloatingView.findViewById(R.id.left);
        btnRight =(ImageButton) mFloatingView.findViewById(R.id.right);
        btnGoUp =(ImageButton) mFloatingView.findViewById(R.id.goUp);
        btnGoDown =(ImageButton) mFloatingView.findViewById(R.id.goDown);
        btnStop =(ImageButton) mFloatingView.findViewById(R.id.stop);


        //https://github.com/ahmedwahba/cordova-plugin-drawoverapps/issues/11
        //setting the layout parameters
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            //getting windows services and adding the floating view to it
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mFloatingView, params);


            //getting the collapsed and expanded view from the floating view
            collapsedView = mFloatingView.findViewById(R.id.layoutCollapsed);
            expandedView = mFloatingView.findViewById(R.id.layoutExpanded);

            //adding click listener to close button and expanded view
            mFloatingView.findViewById(R.id.buttonClose).setOnClickListener(this);
            expandedView.setOnClickListener(this);

            //adding an touchlistener to make drag movement of the floating widget
            mFloatingView.findViewById(R.id.relativeLayoutParent).setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;

                        case MotionEvent.ACTION_UP:
                            //when the drag is ended switching the state of the widget
                            collapsedView.setVisibility(View.GONE);
                            expandedView.setVisibility(View.VISIBLE);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            //this code is helping the widget to move around the screen with fingers
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            mWindowManager.updateViewLayout(mFloatingView, params);
                            return true;
                    }
                    return false;
                }
            });}
        else{
            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            //getting windows services and adding the floating view to it
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mFloatingView, params);


            //getting the collapsed and expanded view from the floating view
            collapsedView = mFloatingView.findViewById(R.id.layoutCollapsed);
            expandedView = mFloatingView.findViewById(R.id.layoutExpanded);

            //adding click listener to close button and expanded view
            mFloatingView.findViewById(R.id.buttonClose).setOnClickListener(this);
            expandedView.setOnClickListener(this);

            //adding an touchlistener to make drag movement of the floating widget
            mFloatingView.findViewById(R.id.relativeLayoutParent).setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;

                        case MotionEvent.ACTION_UP:
                            //when the drag is ended switching the state of the widget
                            collapsedView.setVisibility(View.GONE);
                            expandedView.setVisibility(View.VISIBLE);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            //this code is helping the widget to move around the screen with fingers
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            mWindowManager.updateViewLayout(mFloatingView, params);
                            return true;
                    }
                    return false;
                }
            });}

        /////////////////////////////////////////////////////////////////////////////////////////////for Buttons
        btnForward.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(forward.getBytes());

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});

        btnBackward.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(backward.getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});

        btnLeft.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(left.getBytes());

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});

        btnRight.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(right.getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});

        btnGoUp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(goUp.getBytes());

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});

        btnGoDown.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(goDown.getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});

        btnStop.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(stop.getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});
        return Service.START_STICKY;
    }

    private void askPermission() {// permission for floating widget
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        //startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*if (mBTSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }*/
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layoutExpanded:
                //switching views
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
                break;

            case R.id.buttonClose:
                if (mBTSocket != null && mIsBluetoothConnected) {
                    new DisConnectBT().execute();
                }
                //closing the widget
                stopSelf();
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////copied from Controlling.java
    private class ReadInput implements Runnable {

        private boolean bStop = false;
        private Thread t;

        public ReadInput() {
            t = new Thread(this, "Input Thread");
            t.start();
        }

        public boolean isRunning() {
            return t.isAlive();
        }

        @Override
        public void run() {
            InputStream inputStream;

            try {
                inputStream = mBTSocket.getInputStream();
                while (!bStop) {
                    byte[] buffer = new byte[256];
                    if (inputStream.available() > 0) {
                        inputStream.read(buffer);
                        int i = 0;
                        /*
                         * This is needed because new String(buffer) is taking the entire buffer i.e. 256 chars on Android 2.3.4 http://stackoverflow.com/a/8843462/1287554
                         */
                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
                        }
                        final String strInput = new String(buffer, 0, i);

                        /*
                         * If checked then receive text, better design would probably be to stop thread if unchecked and free resources, but this is a quick fix
                         */
                    }
                    Thread.sleep(500);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        public void stop() {
            bStop = true;
        }
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {
            //progressDialog = ProgressDialog.show(FloatingViewService.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554
        }

        @SuppressLint("MissingPermission") ///permission suppressed
        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (mBTSocket == null || !mIsBluetoothConnected) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                }
            } catch (IOException e) {
                // Unable to connect to device`
                // e.printStackTrace();
                mConnectSuccessful = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if(mConnectSuccessful)
            {
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Could not connect to device.Please turn on your Hardware", Toast.LENGTH_LONG).show();
            }
            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Could not connect to device.Please turn on your Hardware", Toast.LENGTH_LONG).show();
                //finish();
            } else {
                msg("Connected to device");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }
            //progressDialog.dismiss();
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {//can't understand these dots

            if (mReadThread != null) {
                mReadThread.stop();
                while (mReadThread.isRunning())
                    ; // Wait until it stops
                mReadThread = null;

            }

            try {
                mBTSocket.close();
            } catch (IOException e) {
                 // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
            if (mIsUserInitiatedDisconnect) {
                //finish();
            }
        }
    }
}
