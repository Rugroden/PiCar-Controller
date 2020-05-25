package com.rugroden.picar

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.anychart.APIlib
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.SingleValueDataSet
import com.anychart.charts.LinearGauge
import com.anychart.enums.Layout
import com.anychart.enums.MarkerType
import com.anychart.scales.OrdinalColor
import com.rugroden.R
import com.rugroden.picar.utils.findView
import timber.log.Timber


class ControllerFragment:
  Fragment(),
  SensorEventListener
{

  //VARS
  //Views
  private lateinit var lrProgressBar:ProgressBar
  private lateinit var lrChartView:AnyChartView
  private lateinit var frProgressBar:ProgressBar
  private lateinit var frChartView:AnyChartView
  private lateinit var debugTextView:TextView
  private lateinit var calibrateButton:Button

  private var socket:BluetoothSocket? = null
  private var sensorManager:SensorManager? = null
  private var lrChart:LinearGauge? = null
  private var frChart:LinearGauge? = null

  private var calibrated:Boolean = false
  private var startX:Float = 0f
  private var startY:Float = 0f
  private val deadZone:Float = 1.5f

  //region --------------- START Lifecycle Stuff ---------------

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_controller,container,false)

    lrProgressBar = view.findView(R.id.lr_progress_bar)
    lrChartView = view.findView(R.id.lr_chart)
    lrChartView.setProgressBar(lrProgressBar)
    lrChart = setupChart(lrChartView, Layout.HORIZONTAL)

    frProgressBar = view.findView(R.id.fr_progress_bar)
    frChartView = view.findView(R.id.fr_chart)
    frChartView.setProgressBar(frProgressBar)
    frChart = setupChart(frChartView, Layout.VERTICAL)

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
    sensorManager?.let {manager ->
      manager.unregisterListener(this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))
    }
    super.onPause()
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
//        debugTextView.text = "calibrated to ($x,$y)"
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

      APIlib.getInstance().setActiveAnyChartView(lrChartView)
      lrChart?.data(SingleValueDataSet(listOf(-(x-startX))))
      APIlib.getInstance().setActiveAnyChartView(frChartView)
      frChart?.data(SingleValueDataSet(listOf(-(y-startY))))
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

  private fun setupChart(chartView:AnyChartView, direction:Layout) : LinearGauge{
    APIlib.getInstance().setActiveAnyChartView(chartView)
    val scaleBarColorScale = OrdinalColor.instantiate()
      .ranges(
        arrayOf(
          "{ from: -10, to: -$deadZone , color: ['#2A9D8F 1.0'] }",
          "{ from: -$deadZone, to: $deadZone, color: ['#E9c46A 1.0'] }",
          "{ from: $deadZone, to: 10, color: ['#2A9D8F 1.0'] }"
        )
    )

    val linearGauge = AnyChart.linear().apply {
      layout(direction)
      scale()
        .minimum(-10)
        .maximum(10)

      scaleBar(0)
        .colorScale(scaleBarColorScale)
        .width("80%")
        .zIndex(9)

      val marker:MarkerType =
        if(direction==Layout.HORIZONTAL){ MarkerType.TRIANGLE_UP }
        else{ MarkerType.TRIANGLE_LEFT }

      marker(0)
        .type(marker)
        .color("black")
        .zIndex(10)
        .offset("50%")
        .width("50%")
    }

    chartView.setChart(linearGauge)
    return linearGauge
  }
}