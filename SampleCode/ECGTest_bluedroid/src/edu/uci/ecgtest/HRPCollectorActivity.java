/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package edu.uci.ecgtest;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HRPCollectorActivity extends Activity {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int STATE_READY = 10;
    public static final String TAG = "HRPCollector";
    private static final int HRP_PROFILE_CONNECTED = 20;
    private static final int HRP_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    private Context mContext = null;
    public int mState = HRP_PROFILE_DISCONNECTED;

    private HRPService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    Button mbtnHrmNoti,mReadBsl,mResetEE,mdisableHrcp,mRead2a29,mRead2a2a
    ,mRead2a25,mSelectDevice,btn_conn;
    
	private ImageView animatedView;
	private AnimationDrawable mAnimation;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mContext = getApplicationContext();
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        init();
        mSelectDevice = (Button)findViewById(R.id.btn_select);
        mbtnHrmNoti = (Button)findViewById(R.id.btn_write_HRM_Noty);
        mReadBsl = (Button)findViewById(R.id.btn_BSL);
        mResetEE= (Button)findViewById(R.id.btn_HRCP);
        mResetEE.setVisibility(View.GONE);
        mdisableHrcp= (Button)findViewById(R.id.btn_Disable_HRCP);
        mdisableHrcp.setVisibility(View.GONE);
        mRead2a29= (Button)findViewById(R.id.btn_uuid_read_0x2A29);
        mRead2a29.setVisibility(View.GONE);
        mRead2a2a= (Button)findViewById(R.id.btn_uuid_read_0x2A2A);
        mRead2a2a.setVisibility(View.GONE);
        mRead2a25= (Button)findViewById(R.id.btn_uuid_read_0x2A25);
        mRead2a25.setVisibility(View.GONE);
        btn_conn = (Button) findViewById(R.id.btn_connect_or_disconnect);
        
        ((LinearLayout) findViewById(R.id.linearLayout5)).setVisibility(View.GONE);
		animatedView = (ImageView) findViewById(R.id.animated_view);
		animatedView.setBackgroundResource(R.drawable.animateion_loader);
		mAnimation = (AnimationDrawable) animatedView.getBackground();
		animatedView.setVisibility(View.GONE);

        ((Button) findViewById(R.id.btn_select)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    Intent newIntent = new Intent(HRPCollectorActivity.this, DeviceListActivity.class);
                    startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                }
            }
        });

        ((Button) findViewById(R.id.btn_connect_or_disconnect)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mState == STATE_READY || (mState == HRP_PROFILE_CONNECTED)) {
                    mService.disableNotification(mDevice);//disable HR notifications, if enabled
                    mService.disconnect(mDevice);
                } else if (mState == HRP_PROFILE_DISCONNECTED) {
                    mService.connect(mDevice, false);
                }

            }
        });
        ((Button) findViewById(R.id.btn_write_HRM_Noty)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(!mAnimation.isRunning()){
                	mService.startECGRecording(mDevice);
                	// mService.startACCService(mDevice);
	            	animatedView.setVisibility(View.VISIBLE);
	            	mAnimation.start();
            	}
                //mService.enableHRNotification(mDevice);
            }
        });

        ((Button) findViewById(R.id.btn_Disable_HRCP)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {                
            	mService.disableHRNotification(mDevice);
            }
        });

        ((Button) findViewById(R.id.btn_BSL)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(mAnimation.isRunning()){
                	mService.stopECGRecording(mDevice);
                	// mService.stopACCService(mDevice);
	            	animatedView.setVisibility(View.GONE);
	            	mAnimation.stop();
            	}
            	//mService.getBodySensorLoc(mDevice);
            }
        });
        ((Button) findViewById(R.id.btn_uuid_read_0x2A25)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.read_uuid_read_25(mDevice);
            }
        });
        ((Button) findViewById(R.id.btn_HRCP)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.ResetEnergyExpended(mDevice);
            }
        });
        ((Button) findViewById(R.id.btn_remove_bond)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.removeBond(mDevice);
            }
        });

        ((Button) findViewById(R.id.btn_uuid_read_0x2A29)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.read_uuid_read_29(mDevice);
            }
        });
        ((Button) findViewById(R.id.btn_uuid_read_0x2A2A)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.read_uuid_read_2A(mDevice);
            }
        });
    }

    private BroadcastReceiver deviceStateListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Intent mIntent = intent;
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = mIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int devState = mIntent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                Log.d(TAG, "BluetoothDevice.ACTION_BOND_STATE_CHANGED");
                setUiState();
                if (device.equals(mDevice) && devState == BluetoothDevice.BOND_NONE) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mDevice = null;
                            setUiState();
                        }
                    });
                }
            }
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = mIntent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED" + "state is" + state);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if ( state == STATE_OFF) {
                                mDevice=null;
                                mState = HRP_PROFILE_DISCONNECTED;
                                setUiStateForBTOff();
                            }
                        }
                    });
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((HRPService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            mService.setActivityHandler(mHandler);
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HRPService.HRP_CONNECT_MSG:
                Log.d(TAG, "mHandler.HRP_CONNECT_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = HRP_PROFILE_CONNECTED;
                        setUiState();
                    }
                });
                break;

            case HRPService.HRP_DISCONNECT_MSG:
                Log.d(TAG, "mHandler.HRP_DISCONNECT_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = HRP_PROFILE_DISCONNECTED;
                        setUiState();
                    }
                });
                break;

            case HRPService.HRP_READY_MSG:
                Log.d(TAG, "mHandler.HRP_READY_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = STATE_READY;
                        setUiState();
                    }
                });

            case HRPService.HRP_VALUE_MSG:
                Log.d(TAG, "mHandler.HRP_VALUE_MSG");
                Bundle data1 = msg.getData();
                final byte[] bslval = data1.getByteArray(HRPService.BSL_VALUE);
                final int hrmval = data1.getInt(HRPService.HRM_VALUE, 0);
                final int eeval = data1.getInt(HRPService.HRM_EEVALUE, 0);
                final String serialno = data1.getString(HRPService.SERIAL_STRING);
                final byte[] manfname = data1.getByteArray(HRPService.MANF_NAME);
                final byte[] icdl = data1.getByteArray(HRPService.ICDL_VALUE);
                final ArrayList<Integer> rrval = data1.getIntegerArrayList(HRPService.HRM_RRVALUE);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (bslval != null) {
                            try {
                                Log.i(TAG, "BYTE BSL VAL =" + bslval[0]);
                                TextView bsltv = (TextView) findViewById(R.id.BodySensorLocation);
                                bsltv.setText("\t" + mContext.getString(R.string.BodySensorLocation)
                                        + getBodySensorLocation(bslval[0]));
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());

                            }

                        }
                        if (serialno != null) {
                            try {
                                TextView serialtxt = (TextView) findViewById(R.id.SerialNumberString);
                                serialtxt.setText("\t" + mContext.getString(R.string.SerialNumberString) + "::"
                                        + serialno);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }

                        }
                        if (manfname != null) {
                            try {
                                String m = new String(manfname, "UTF-8");
                                TextView manfnametxt = (TextView) findViewById(R.id.ManfName);
                                manfnametxt.setText("\t" + mContext.getString(R.string.ManfName) + "::" + m);
                            } catch (UnsupportedEncodingException  e) {
                                Log.e(TAG, e.toString());
                            }

                        }
                        if (icdl != null) {
                            try {
                                display_reg_data(icdl);
                                TextView icdl_txt = (TextView) findViewById(R.id.CertDataList);
                                icdl_txt.setText("\t" + mContext.getString(R.string.CertDataList)
                                        + ":: Hex Value :: 0x" + toHex(new String(icdl)));

                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }

                        }
                        if (hrmval >= 0) {
                            TextView hrmtv = (TextView) findViewById(R.id.HeartRateValue);
                            hrmtv.setText("\t" + mContext.getString(R.string.HeartRateValue) + hrmval
                                    + " beats per minute");
                        } else {
                            TextView hrmtv = (TextView) findViewById(R.id.HeartRateValue);
                            hrmtv.setText("\t" + mContext.getString(R.string.HeartRateValue) + hrmval
                                    + " beats per minute");
                        }
                        if (eeval > 0) {
                            TextView eevaltv = (TextView) findViewById(R.id.EnergyExpended);
                            eevaltv.setText("\t" + mContext.getString(R.string.EnergyExpendedValue) + eeval + " KJ");
                        } else {
                            TextView eevaltv = (TextView) findViewById(R.id.EnergyExpended);
                            eevaltv.setText("\t" + mContext.getString(R.string.EnergyExpendedValue));
                        }
                        if (rrval != null && rrval.size() > 0) {
                            String rrvalstring = "\t" + mContext.getString(R.string.RR_IntervalValue);
                            for (int i = 0; i < rrval.size(); i++) {
                                int temp = rrval.get(i).intValue();
                                rrvalstring = rrvalstring.concat(temp + "(1/1024) sec. ");
                            }
                            TextView rrvaltv = (TextView) findViewById(R.id.RRInterval);
                            rrvaltv.setText(rrvalstring);
                        } else {
                            TextView rrvaltv = (TextView) findViewById(R.id.RRInterval);
                            rrvaltv.setText("\t" + mContext.getString(R.string.RR_IntervalValue));
                        }

                    }
                });

            default:
                super.handleMessage(msg);
            }
        }
    };

    /* Func to Display IEEE Reg cert data */
    public void display_reg_data(byte[] value) {
        int offset = 0;
        int count = 0;
        int length = 0;
        int auth_body = 0;
        int auth_body_stype = 0;
        int auth_body_slength = 0;
        int auth_body_data_MajIG = 0;
        int auth_body_data_MinIG = 0;
        int auth_body_data_cdcl_cnt = 0;
        int auth_body_data_cdcl_length = 0;
        int[] g_class_entry = null;
        int cont_reg_struct_data = 0;
        int cont_reg_struct_length = 0;
        int cont_reg_bitFType = 0;
        int jump_count = 1;
        boolean flag = false;
        while (offset < value.length && !flag) {
            switch (offset) {
            case 0x0:
                count = (value[offset++] << 8) | value[offset++];
                break;
            case 0x02:
                length = (value[offset++] << 8) | value[offset++];
                break;
            case 0x04:
                auth_body = value[offset++];
                break;
            case 0x05:
                auth_body_stype = value[offset++];
                break;
            case 0x06:
                auth_body_slength = (value[offset++] << 8) | value[offset++];
                break;
            case 0x08:
                auth_body_data_MajIG = value[offset++];
                break;
            case 0x09:
                auth_body_data_MinIG = value[offset++];
                break;
            case 0x0A:
                auth_body_data_cdcl_cnt = (value[offset++] << 8) | value[offset++];
                break;
            case 0x0C:
                auth_body_data_cdcl_length = (value[offset++] << 8) | value[offset++];
                int[] class_entry = new int[auth_body_data_cdcl_cnt];
                for (int i = 0; i < auth_body_data_cdcl_cnt; i++) {
                    class_entry[i] = (value[offset++] << 8) | value[offset++];
                }
                g_class_entry = class_entry;
                flag = true;
                break;
            default:
                Log.e(TAG, "wrong offset");
                break;
            }

        }

        while (offset < value.length) {
            switch (jump_count) {
            case 1:
                cont_reg_struct_data = (value[offset++] << 8) | value[offset++];
                jump_count++;
                break;
            case 2:
                cont_reg_struct_length = (value[offset++] << 8) | value[offset++];
                jump_count++;
                break;
            case 3:
                cont_reg_bitFType = (value[offset++] << 8) | value[offset++];
                break;
            default:
                Log.i(TAG, "wrong count");
                break;
            }
        }

        Log.i(TAG, "------------------IEEE REG CERT DATA---------------");
        Log.i(TAG, "count = " + count);
        Log.i(TAG, "length = " + length);
        Log.i(TAG, "auth_body = " + auth_body);
        Log.i(TAG, "auth_body_stype = " + auth_body_stype);
        Log.i(TAG, "auth_body_slength = " + auth_body_slength);
        Log.i(TAG, "auth_body_data_MajIG = " + auth_body_data_MajIG);
        Log.i(TAG, "auth_body_data_MinIG = " + auth_body_data_MinIG);
        Log.i(TAG, "auth_body_data_cdcl_cnt = " + auth_body_data_cdcl_cnt);
        Log.i(TAG, "auth_body_data_cdcl_length = " + auth_body_data_cdcl_length);
        if (g_class_entry != null) {
            Log.i(TAG, "Certified device class entry = ");
            for (int i : g_class_entry) {
                Log.i(TAG, "Certified device class entry[] =" + i);
            }
        }
        Log.i(TAG, "cont_reg_struct_data = " + cont_reg_struct_data);
        Log.i(TAG, "cont_reg_struct_length = " + cont_reg_struct_length);
        Log.i(TAG, "cont_reg_bitFType = " + cont_reg_bitFType);

    }

    public String toHex(String arg) {
        String temp = null;
        try {
            temp = String.format("%x", new BigInteger(arg.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return temp;
    }

    private String getBodySensorLocation(short bodySensorLocationValue) {
        Log.d(TAG, "getBodySensorLocation");
        if (bodySensorLocationValue == 0x00)
            return "Other";
        else if (bodySensorLocationValue == 0x01)
            return "Chest";
        else if (bodySensorLocationValue == 0x02)
            return "Wrist";
        else if (bodySensorLocationValue == 0x03)
            return "Finger";
        else if (bodySensorLocationValue == 0x04)
            return "Hand";
        else if (bodySensorLocationValue == 0x05)
            return "Ear Lobe";
        else if (bodySensorLocationValue == 0x06)
            return "Foot";
        return "reserved for future use";
    }

    private void init() {
        Log.d(TAG, "init() mService= " + mService);
        Intent bindIntent = new Intent(this, HRPService.class);
        startService(bindIntent);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(deviceStateListener, filter);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() mService= " + mService);
    }

    @Override
    public void onDestroy() {
        unbindService(mServiceConnection);
        unregisterReceiver(deviceStateListener);
        stopService(new Intent(this, HRPService.class));
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

        case REQUEST_SELECT_DEVICE:
            if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                updateUi();
                mService.connect(mDevice, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        default:
            Log.e(TAG, "wrong requst Code");
            break;
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        updateUi();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void updateUi() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUiState();
            }
        });
    }

    private void setUiState() {
        Log.d(TAG, "... setUiState.mState" + mState);
        mSelectDevice.setEnabled(mState == HRP_PROFILE_DISCONNECTED);
        mbtnHrmNoti.setEnabled(mState == STATE_READY);
        mdisableHrcp.setEnabled(mState == STATE_READY);
        mRead2a25.setEnabled(mState == STATE_READY);
        mReadBsl.setEnabled(mState == STATE_READY);
        mRead2a29.setEnabled(mState == STATE_READY);
        mRead2a2a.setEnabled(mState == STATE_READY);
        mResetEE.setEnabled(mState == STATE_READY);
        switch (mState) {
        case HRP_PROFILE_CONNECTED:
            Log.i(TAG, "STATE_CONNECTED::device name"+mDevice.getName());
            btn_conn.setText(R.string.disconnect);
            btn_conn.setEnabled(false);
            break;
        case HRP_PROFILE_DISCONNECTED:
            Log.i(TAG, "disconnected");
            btn_conn.setText(R.string.connect);
            btn_conn.setEnabled(true);
            TextView bsltv = (TextView) findViewById(R.id.BodySensorLocation);
            bsltv.setText("\t" + mContext.getString(R.string.BodySensorLocation));
            TextView hrmtv = (TextView) findViewById(R.id.HeartRateValue);
            hrmtv.setText("\t" + mContext.getString(R.string.HeartRateValue));
            TextView eevaltv = (TextView) findViewById(R.id.EnergyExpended);
            eevaltv.setText("\t" + mContext.getString(R.string.EnergyExpendedValue));
            TextView rrvaltv = (TextView) findViewById(R.id.RRInterval);
            rrvaltv.setText("\t" + mContext.getString(R.string.RR_IntervalValue));
            TextView serialtxt = (TextView) findViewById(R.id.SerialNumberString);
            serialtxt.setText("\t" + mContext.getString(R.string.SerialNumberString));
            TextView manfnametxt = (TextView) findViewById(R.id.ManfName);
            manfnametxt.setText("\t" + mContext.getString(R.string.ManfName));
            TextView icdl_txt = (TextView) findViewById(R.id.CertDataList);
            icdl_txt.setText("\t" + mContext.getString(R.string.CertDataList));
            break;
        case STATE_READY:
            btn_conn.setText(R.string.disconnect);
            btn_conn.setEnabled(mDevice != null);
            break;
        default:
            Log.e(TAG, "wrong mState");
            break;
        }
        findViewById(R.id.btn_remove_bond).setEnabled(
                mDevice != null && mDevice.getBondState() == BluetoothDevice.BOND_BONDED);
        if (mDevice == null) {
            Log.i(TAG, "device null");
            btn_conn.setText(R.string.connect_or_disconnect);
            btn_conn.setEnabled(false);
            ((TextView) findViewById(R.id.deviceName)).setText(R.string.no_device);
        }
        else {
            ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName());
        }
    }

    private void setUiStateForBTOff() {
        mSelectDevice.setEnabled(true);
        mbtnHrmNoti.setEnabled(false);
        mdisableHrcp.setEnabled(false);
        mReadBsl.setEnabled(false);
        mRead2a29.setEnabled(false);
        mRead2a2a.setEnabled(false);
        mResetEE.setEnabled(false);
        mRead2a25.setEnabled(false);
        findViewById(R.id.btn_remove_bond).setEnabled(false);
        btn_conn.setEnabled(false);
        btn_conn.setText(R.string.connect_or_disconnect);
        ((TextView) findViewById(R.id.deviceName)).setText(R.string.no_device);
    }

    @Override
    public void onBackPressed() {
        if (mState == STATE_READY) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
        else {
            new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.popup_title)
            .setMessage(R.string.popup_message)
            .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setNegativeButton(R.string.popup_no, null)
            .show();
        }
    }
}
