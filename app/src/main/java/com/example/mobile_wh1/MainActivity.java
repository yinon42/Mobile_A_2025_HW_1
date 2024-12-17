package com.example.mobile_wh1;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // UI Components
    private View dot1, dot2, dot3, dot4, dot5, dot6, dot7, dot8;
    private Button loginButton;

    // System Services
    private WifiReceiver wifiReceiver;
    private BluetoothReceiver bluetoothReceiver;
    private AudioManager audioManager;
    private CameraManager cameraManager;
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;

    // Sensor Data
    private float[] gravity, geomagnetic;

    // Dot Status Flags
    private boolean dot1Green, dot2Green, dot3Green, dot4Green, dot5Green, dot6Green, dot7Green, dot8Green;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views and components
        findViews();
        initializeSensors();
        registerReceivers();

        // Start Observers
        updateBluetoothDot();
        updateWifiDot();
        observeVolume();
        observeBrightness();
        observeFlashlight();
        observeBatteryStatus();
        observeTouchEvent();

        // Login Button Action
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Activity_Success.class);
            startActivity(intent);
        });
    }

    /**
     * Initialize Views from XML.
     */
    private void findViews() {
        dot1 = findViewById(R.id.dot1); // Bluetooth
        dot2 = findViewById(R.id.dot2); // Wi-Fi
        dot3 = findViewById(R.id.dot3); // Volume
        dot4 = findViewById(R.id.dot4); // Brightness
        dot5 = findViewById(R.id.dot5); // Flashlight
        dot6 = findViewById(R.id.dot6); // Touch Event
        dot7 = findViewById(R.id.dot7); // Compass
        dot8 = findViewById(R.id.dot8); // Battery Status
        loginButton = findViewById(R.id.loginButton);
        loginButton.setEnabled(false);
    }

    /**
     * Initialize Sensors for Compass.
     */
    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * Register Broadcast Receivers.
     */
    private void registerReceivers() {
        wifiReceiver = new WifiReceiver();
        bluetoothReceiver = new BluetoothReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    /**
     * Check if all dots are green.
     */
    private void checkAllDots() {
        if (dot1Green && dot2Green && dot3Green && dot4Green && dot5Green && dot6Green && dot7Green && dot8Green) {
            loginButton.setEnabled(true);
        } else {
            loginButton.setEnabled(false);
        }
    }

    /**
     * Bluetooth Check.
     */
    private void updateBluetoothDot() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        dot1Green = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        dot1.setBackgroundResource(dot1Green ? R.drawable.dot_active : R.drawable.dot_inactive);
        checkAllDots();
    }

    /**
     * Wi-Fi Check.
     */
    private void updateWifiDot() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        dot2Green = wifiManager != null && wifiManager.isWifiEnabled();
        dot2.setBackgroundResource(dot2Green ? R.drawable.dot_active : R.drawable.dot_inactive);
        checkAllDots();
    }

    /**
     * Volume Check.
     */
    private void observeVolume() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                dot3Green = currentVolume == maxVolume;
                dot3.setBackgroundResource(dot3Green ? R.drawable.dot_active : R.drawable.dot_inactive);
                checkAllDots();
            }
        });
    }

    /**
     * Brightness Check.
     */
    private void observeBrightness() {
        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                try {
                    int brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                    dot4Green = brightness == 255;
                    dot4.setBackgroundResource(dot4Green ? R.drawable.dot_active : R.drawable.dot_inactive);
                    checkAllDots();
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Flashlight Check.
     */
    private void observeFlashlight() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeChanged(String cameraId, boolean enabled) {
                    dot5Green = enabled;
                    dot5.setBackgroundResource(dot5Green ? R.drawable.dot_active : R.drawable.dot_inactive);
                    checkAllDots();
                }
            }, new Handler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Battery Check.
     */
    private void observeBatteryStatus() {
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                dot8Green = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                dot8.setBackgroundResource(dot8Green ? R.drawable.dot_active : R.drawable.dot_inactive);
                checkAllDots();
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }


    /**
     * Touch Event in Bottom Right.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void observeTouchEvent() {
        View mainLayout = findViewById(R.id.mainLayout);
        mainLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float touchX = event.getX();
                float touchY = event.getY();
                int screenWidth = v.getWidth();
                int screenHeight = v.getHeight();

                dot6Green = touchX > screenWidth * 0.75 && touchY > screenHeight * 0.75;
                dot6.setBackgroundResource(dot6Green ? R.drawable.dot_active : R.drawable.dot_inactive);
                checkAllDots();
            }
            return true;
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) gravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] rotationMatrix = new float[9];
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                if (azimuth < 0) azimuth += 360;

                dot7Green = azimuth >= 350 || azimuth <= 10;
                dot7.setBackgroundResource(dot7Green ? R.drawable.dot_active : R.drawable.dot_inactive);
                checkAllDots();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        unregisterReceiver(wifiReceiver);
        unregisterReceiver(bluetoothReceiver);
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiDot();
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBluetoothDot();
        }
    }
}
