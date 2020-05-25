package com.rugroden.picar

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.rugroden.R
import com.rugroden.picar.utils.findView
import timber.log.Timber


class ControllerFragment:
  Fragment(),
  SensorEventListener
{

  //VARS
  //Views
  private lateinit var debugTextView:TextView
  private lateinit var calibrateButton:Button

  private var socket:BluetoothSocket? = null
  private var sensorManager:SensorManager? = null


  private var calibrated:Boolean = false
  private var startX:Float = 0f
  private var startY:Float = 0f
  private var deadZone:Float = 1f

  //region --------------- START Lifecycle Stuff ---------------

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_controller,container,false)
    debugTextView = view.findView(R.id.debug)
    calibrateButton = view.findView(R.id.calibrater){ it.setOnClickListener{ calibrated = false } }

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


  //region --------------- START SensorEventListener Stuff ---------------

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    Timber.d("Sensor accuracy changed: ${sensor?.name} -> $accuracy")
  }

  //forward negative y
  //left positive x
  //right negative x
  //back positive y
  override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER){
      val x=event.values[0]
      val y=event.values[1]
      if(!calibrated){
        //debugTextView.text = "calibrated to ($x,$y)"
        startX = x
        startY = y
        calibrated = true
      }

      var cmdString = ""
      if(y-startY < -deadZone){ cmdString += "w" }
      else if(y-startY > deadZone){ cmdString += "s" }
      if(x-startX > deadZone){ cmdString += "a" }
      else if(x-startX < -deadZone){ cmdString += "d" }
      if(cmdString.isEmpty()){ cmdString += "q" }

      sendData(cmdString)
//      Timber.i("sensor changed ($startX, $startY) ->($x, $y)")
    }
  }
  //endregion ---------- END SensorEventListener Stuff ----------


  fun onBackPressed():Boolean{
    return if(socket != null){
      endConnection()
      true
    }
    else{ false }
  }

  private fun endConnection(){
    sendData("q")
    sendData("l")
    calibrated = false

    socket?.close()
    socket = null
  }

  fun setSocket(newSocket: BluetoothSocket){
    endConnection()
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