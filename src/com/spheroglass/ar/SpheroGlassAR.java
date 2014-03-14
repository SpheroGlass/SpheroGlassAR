package com.spheroglass.ar;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.PersistentOptionFlags;
import orbotix.sphero.SensorControl;
import orbotix.sphero.SensorFlag;
import orbotix.sphero.SensorListener;
import orbotix.sphero.Sphero;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.glass.widget.SliderView;
import com.spheroglass.ar.R;

public class SpheroGlassAR extends Activity implements CustomCameraView.Listener {

	public static final double MAX_SPEED_DIAMETER = 30.0;
	public static final double STOP_DIAMETER = 20.0;

	protected static final String TAG = "SpheroGlass";

	private Sphero mRobot;
	private SensorListener sensorListener;

	private float speed = 0f;
	private int direction = 0;
	private int offset = 0;
	private float turn = 0;
	
	private SensorManager mSensorManager;
	private float mAccel;
	private float mAccelCurrent;
	private float mAccelLast;
	private final SensorEventListener mSensorListener = getSensorListener();

	private Timer loopCommandsTimer;
	boolean disconnecting = false;

	private SliderView mIndeterm;

	private ARView arView;
	private CustomCameraView cv;

	private WakeLock wakeLock;

	int mode = -1; // 0 = drive, 1=align

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu);
		
		PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
		
		initState();
	}

	private void changeView() {
		FrameLayout fl = new FrameLayout(this.getApplicationContext());
		setContentView(fl);
		cv = new CustomCameraView(this.getApplicationContext());
		arView = new ARView(getApplicationContext());
		cv.addListener(arView);
		cv.addListener(this);
		fl.addView(cv);
		fl.addView(arView);
		
	}

	private void initState() {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccel = 0.00f;
		mAccelCurrent = SensorManager.GRAVITY_EARTH;
		mAccelLast = SensorManager.GRAVITY_EARTH;
		speed = 0f;
		direction = 0;
		turn = 0;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		wakeLock.acquire();

		connectToSphero();
		//initSensors();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		wakeLock.release();
		
		disconnectFromSphero();
		//stopSensors();
	}

	private void initSensors() {
		mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	}

	private void stopSensors() {
		mSensorManager.unregisterListener(mSensorListener);
	}

	private void connectToSphero() {
		
		mIndeterm = (SliderView) findViewById(R.id.indeterm_slider);
		mIndeterm.startIndeterminate();
		
		RobotProvider.getDefaultProvider().removeConnectionListeners();
		RobotProvider.getDefaultProvider().addConnectionListener(new ConnectionListener() {
			@Override
			public void onConnected(Robot robot) {
				mIndeterm.stopIndeterminate();
				mRobot = (Sphero) robot;
				SpheroGlassAR.this.connected();
				updateConnectionStatus(R.string.connection_ok);
			}

			@Override
			public void onConnectionFailed(Robot sphero) {
				mIndeterm.stopIndeterminate();
				if(!disconnecting) {
					Log.d(TAG, "Connection Failed: " + sphero);
					updateConnectionStatus(R.string.connection_failed);
				}
			}

			@Override
			public void onDisconnected(Robot robot) {
				Log.d(TAG, "Disconnected: " + robot);
				updateConnectionStatus(R.string.connection_disconnected);
				mRobot = null;
			}
		});

		RobotProvider.getDefaultProvider().addDiscoveryListener(new DiscoveryListener() {
			@Override
			public void onBluetoothDisabled() {
				Log.d(TAG, "Bluetooth Disabled");
				updateConnectionStatus(R.string.bluetooth_disabled);
			}

			@Override
			public void discoveryComplete(List<Sphero> spheros) {
				Log.d(TAG, "Found " + spheros.size() + " robots");
			}

			@Override
			public void onFound(List<Sphero> sphero) {
				Log.d(TAG, "Found: " + sphero);
				RobotProvider.getDefaultProvider().connect(sphero.iterator().next());
			}
		});

		boolean success = RobotProvider.getDefaultProvider().startDiscovery(this);
		if (!success) {
			Log.d(TAG, "Unable To start Discovery!");
		}
	}

	private void disconnectFromSphero() {
		Log.d(TAG, "Disconnecting (1): " + Thread.currentThread().getName());
		disconnecting = true;
		loopCommandsStop();
		Log.d(TAG, "Disconnecting (2): " + Thread.currentThread().getName());
		
		if (mRobot != null) {
//			final SensorControl control = mRobot.getSensorControl();
//			control.removeSensorListener(sensorListener);

//			BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
			mRobot.disconnect();
		}
//		RobotProvider.getDefaultProvider().removeDiscoveryListeners();
//		RobotProvider.getDefaultProvider().removeAllControls();
//		RobotProvider.getDefaultProvider().shutdown();
	}
	
	private void connected() {
		Log.d(TAG, "Connected On Thread: " + Thread.currentThread().getName());
		Log.d(TAG, "Connected: " + mRobot);

		final SensorControl control = mRobot.getSensorControl();
		sensorListener = new SensorListener() {
			@Override
			public void sensorUpdated(DeviceSensorsData sensorDataArray) {
				Log.d(TAG, sensorDataArray.toString());
			}
		};
		control.addSensorListener(sensorListener, SensorFlag.ACCELEROMETER_NORMALIZED, SensorFlag.GYRO_NORMALIZED);

		control.setRate(1);
		mRobot.enableStabilization(true);
		mRobot.setBackLEDBrightness(.99f);
		mRobot.setColor(0, 255, 0);

		boolean preventSleepInCharger = mRobot.getConfiguration().isPersistentFlagEnabled(PersistentOptionFlags.PreventSleepInCharger);
		Log.d(TAG, "Prevent Sleep in charger = " + preventSleepInCharger);
		Log.d(TAG, "VectorDrive = " + mRobot.getConfiguration().isPersistentFlagEnabled(PersistentOptionFlags.EnableVectorDrive));

		mRobot.getConfiguration().setPersistentFlag(PersistentOptionFlags.PreventSleepInCharger, false);
		mRobot.getConfiguration().setPersistentFlag(PersistentOptionFlags.EnableVectorDrive, true);

		Log.d(TAG, "VectorDrive = " + mRobot.getConfiguration().isPersistentFlagEnabled(PersistentOptionFlags.EnableVectorDrive));
		Log.v(TAG, mRobot.getConfiguration().toString());

		loopCommandsStart();
	}

	private void loopCommandsStart() {
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				Sphero robot = mRobot;
				if(robot != null && mRobot.isConnected()) {
					if(mode == 0) {
						if(Math.abs(turn)>1.5) {
							direction += 5 * (turn > 0 ? 1 : -1);
						}
						if(Math.abs(speed)>0.1) {
							if(speed>0) {
								robot.drive(normalizeDirection(direction), normalizeSpeed(speed));
							} else {
								robot.drive(normalizeDirection(direction+180), normalizeSpeed(-speed));
							}
						} else {
							robot.drive(normalizeDirection(direction), 0);
						}
					} else if(mode == 1) {
						offset += 5;
						robot.drive(normalizeDirection(0), 0);
					} else {
						robot.drive(normalizeDirection(0), 0);
					}
				}
			}

			private int normalizeDirection(int direction) {
				return (((direction+offset)%360)+360)%360;
			}
			
			private float normalizeSpeed(float speed) {
				return Math.min(speed * 2, 1);
			}
		};
		loopCommandsTimer = new Timer();
		loopCommandsTimer.schedule(timerTask, 0, 100);
	}
	
	private void loopCommandsStop() {
		if(loopCommandsTimer != null) {
			loopCommandsTimer.cancel();
			loopCommandsTimer = null;
		}
	}

	private SensorEventListener getSensorListener() {
		return new SensorEventListener() {

			public void onSensorChanged(SensorEvent se) {
				float x = se.values[0];
				float y = se.values[1];
				float z = se.values[2];
				mAccelLast = mAccelCurrent;
				mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
				float delta = mAccelCurrent - mAccelLast;
				mAccel = mAccel * 0.9f + delta;
				
				speed = (float) (-z / SensorManager.GRAVITY_EARTH);
				turn = -x;
			}

			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}
	
	private void updateConnectionStatus(final int stringId) {
		
		this.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				setContentView(R.layout.menu);
				final TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
				connectionStatus.setText(stringId);
				connectionStatus.setVisibility(View.VISIBLE);
				
				View slider = findViewById(R.id.indeterm_slider);
				slider.setVisibility(View.INVISIBLE);
				
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						connectionStatus.setVisibility(View.INVISIBLE);
						if(stringId == R.string.connection_ok) {
							openOptionsMenu();
						}
					}
				}, 2000);
			}
		});
		
	}

	@Override
	public void setPoint(int x, int y) {
		speed = (float)Math.max(Math.min((Math.sqrt(x*x+y*y)-STOP_DIAMETER)/MAX_SPEED_DIAMETER, 1.0) / 5.0, 0.0);
		if(speed>0) {
			direction = ((int)(Math.atan2(-x, -y) * 180.0 / Math.PI)+360)%360;
		}
	}

	@Override
	public void setPoints(List<Pair<Integer, Integer>> points) {
		if(points.size()>0) {
			setPoint(points.get(0).first, points.get(0).second);
		} else {
			setPoint(0, 0);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.drive:
			mode = 0;
			changeView();
			break;
		case R.id.align:
			mode = 1;
			changeView();
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			mode = -1;
			openOptionsMenu();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public float getSpeed() {
		return speed;
	}

	public int getDirection() {
		return direction;
	}
}
