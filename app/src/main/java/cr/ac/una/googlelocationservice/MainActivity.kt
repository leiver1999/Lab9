package cr.ac.una.googlelocationservice


import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import cr.ac.una.googlelocationservice.fragments.WebViewFragment


class MainActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.hasExtra("DESDE_NOTIFICACION")) {//si el intent tiene un extra DESDE_NOTIFICACION
            val place = intent.getStringExtra("DESDE_NOTIFICACION")//guarda el valor en place
            openWebViewFragment(place)//y luego se lo manda a este metodo
        }

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationService()
        }
    }

    private fun openWebViewFragment(place: String?) {//recibe un place de tipo String
        val fragment = WebViewFragment().apply {//se crea un fragmento de tipo WebViewFragment
            arguments = Bundle().apply {//se crea un argumento de tipo Bundle
                putString("pageTitle", place)//se le asigna el valor de place a pageTitle
            }
        }

        supportFragmentManager.beginTransaction()//cambio de vista
            .replace(R.id.nav_host_fragment_content_main, fragment)// le llega a webViewFragment
            .commit()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}