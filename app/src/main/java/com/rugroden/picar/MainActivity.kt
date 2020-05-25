package com.rugroden.picar

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.FrameLayout
import com.rugroden.BuildConfig
import com.rugroden.R
import timber.log.Timber

class MainActivity : AppCompatActivity() {

  private lateinit var fragmentContainer:FrameLayout
  private var homescreenFragment:HomescreenFragment = HomescreenFragment()
  private var controllerFragment:ControllerFragment = ControllerFragment()

  var btSocket:BluetoothSocket? = null
  var btDevice:BluetoothDevice? = null


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if(BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    setContentView(R.layout.activity_main)
    fragmentContainer = findViewById(R.id.fragment_container)

    supportFragmentManager.beginTransaction()
      .replace(R.id.fragment_container,homescreenFragment)
      .commit()
  }

  override fun onDestroy() {
    btDevice = null
    btSocket?.close()
    btSocket = null
    super.onDestroy()
  }

  override fun onBackPressed() {
    val handled = controllerFragment.onBackPressed()
    if(handled) {
      closeController()
    }
    else{
      super.onBackPressed()
    }
  }


  fun openController(){
    if(supportFragmentManager.fragments.firstOrNull() !is ControllerFragment){
      btSocket?.let {
        controllerFragment.setSocket(it)
        supportFragmentManager.beginTransaction()
          .replace(R.id.fragment_container, controllerFragment)
          .commit()
      }
    }
  }

  fun closeController(){
    if(supportFragmentManager.fragments.firstOrNull() is ControllerFragment){
      supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container,homescreenFragment)
        .commit()
    }
  }
}
