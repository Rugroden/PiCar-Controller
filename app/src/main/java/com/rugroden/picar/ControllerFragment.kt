package com.rugroden.picar

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.rugroden.R
import com.rugroden.picar.utils.findView
import timber.log.Timber


class ControllerFragment:
  Fragment(),
  View.OnClickListener,
  SensorEventListener
{

  //VARS
  //Views
  private lateinit var forwardButton:ImageButton
  private lateinit var leftButton:ImageButton
  private lateinit var rightButton:ImageButton
  private lateinit var backwardButton:ImageButton

  private var socket:BluetoothSocket? = null
  private var sensorManager:SensorManager? = null

  //region --------------- START Lifecycle Stuff ---------------

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_controller,container,false)
    forwardButton = view.findView(R.id.forward){ it.setOnClickListener(this) }
    leftButton = view.findView(R.id.left){ it.setOnClickListener(this) }
    rightButton = view.findView(R.id.right){ it.setOnClickListener(this) }
    backwardButton = view.findView(R.id.backward){ it.setOnClickListener(this) }

    val manager = view.context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
    sensorManager = manager

    return view
  }

  override fun onResume() {
    keepScreenOn(true)
    super.onResume()
  }

  override fun onPause() {
    keepScreenOn(false)
    super.onPause()
  }

  override fun onDestroyView() {
    sensorManager?.let {manager ->
      manager.unregisterListener(this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))
    }
    super.onDestroyView()
  }

  //endregion ---------- END Lifecycle Stuff ----------

  override fun onClick(v: View) {
    val data = when(v){
      forwardButton -> "w"
      leftButton -> "a"
      rightButton -> "d"
      backwardButton -> "s"
      else -> "q"
    }
    sendData(data)
  }
  //endregion ---------- END OnClickListener Stuff ----------


  //region --------------- START SensorEventListener Stuff ---------------

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    Timber.d("Sensor accuracy changed: ${sensor?.name} -> $accuracy")
  }

  //forward negative y
  //left positive x
  //right negative x
  //back positive y
  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type ==Sensor.TYPE_ACCELEROMETER){
      val x=event.values[0]
      val y=event.values[1]
      val z=event.values[2]

      Timber.e("sensor changed ($x, $y, $z)")
    }
  }
  //endregion ---------- END SensorEventListener Stuff ----------


  fun onBackPressed():Boolean{
    if(socket!=null){
      killSocket()
      return true
    }
    return false
  }
  private fun killSocket(){
    sendData("q")
    sendData("l")

    socket?.close()
    socket = null
  }

  fun setSocket(newSocket: BluetoothSocket){
    killSocket()
    socket = newSocket
  }

  private fun sendData(string:String){
    socket?.outputStream?.write(string.toByteArray())
  }

  private fun keepScreenOn(shouldKeepScreenOn:Boolean){
    activity?.window?.let {
      if(shouldKeepScreenOn) {
        it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
      else{
        it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }
}