package cr.ac.una.googlelocationservice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.gson.Gson
import cr.ac.una.googlelocationservice.entity.Page
import cr.ac.una.googlelocationservice.entity.WikipediaResponse
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private var contNotificacion = 2
    private val gson = Gson()

    private var lastPlace = ""// Variable para almacenar el último lugar visitado, SE INICIA EN BLANCO

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(applicationContext, "AIzaSyBLiFVeg7U_Ugu5bMf7EQ_TBEfPE3vOSF4")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        this.startForeground(1, createNotification("Service running"))

        requestLocationUpdates()
    }

    private fun createNotificationChannel() {

        val serviceChannel = NotificationChannel(
            "locationServiceChannel",
            "Location Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(serviceChannel)

    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, "locationServiceChannel")
            .setContentTitle("Location Service")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun requestLocationUpdates() {

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000
        ).apply {
            setMinUpdateIntervalMillis(50000)
        }.build()


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                getPlaceName(location.latitude, location.longitude)// Se obtiene el nombre del lugar
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun getPlaceName(latitude: Double, longitude: Double) {

        val placeFields: List<Place.Field> = listOf(Place.Field.NAME)


        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)
        val placesClient: PlacesClient = Places.createClient(this)


        val placeResponse = placesClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val response = task.result
                val topPlaces = response.placeLikelihoods
                    .sortedByDescending { it.likelihood }
                    .take(1)
                topPlaces.forEach { placeLikelihood ->

                    Log.d(
                        "AAA",
                        "Lugar: ${placeLikelihood.place.name}, Probabilidad: ${placeLikelihood.likelihood}"
                    )// Se imprime el lugar y la probabilidad de que sea el lugar correcto

                    if (lastPlace != placeLikelihood.place.name) {// Se verifica si el lugar actual es diferente al último lugar visitado
                        lastPlace = placeLikelihood.place.name!!//lo reemplaza por el lugar actual
                        placeLikelihood.place.name?.let { checkIfPlaceHasArticles(it) }//en caso de que sea distinto se verifica si el lugar tiene artículos,
                    } else {// si son iguales se imprime un mensaje en el log
                        Log.e("AAA", "Mismo lugar o no se encontraron artículos")
                        Log.e(
                            "AAA",
                            "Lugar anterior: $lastPlace, lugar actual: ${placeLikelihood.place.name}"
                        )
                    }

                }
            } else {
                val exception = task.exception
                if (exception is ApiException) {
                    Log.e(TAG, "Lugar no encontrado: ${exception.statusCode}")
                }
            }
        }
    }

    private fun sendNotification(place: String, title: String?) {//recibe el nombre del lugar y el título del artículo
        contNotificacion++

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("DESDE_NOTIFICACION", title)//crea un intent con el título del artículo
        }

        val pendingIntent = PendingIntent.getActivity(//configura un pending intent para abrir la actividad principal
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "locationServiceChannel")//crea una notificación
            .setContentTitle("Articulo relacionado disponible")
            .setContentText("Para el lugar: $place")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)//setea el pending intent
            .setAutoCancel(true)
            .build()
        notificationManager.notify(contNotificacion, notification)//envía la notificación
    }

    private suspend fun getPagesForNotification(title: String): ArrayList<Page>? {

        val url = "https://en.wikipedia.org/api/rest_v1/page/related/$title"

        val res = Fuel.get(url).body

        val wikipediaResponse = gson.fromJson(res, WikipediaResponse::class.java)

        return wikipediaResponse.pages
    }

    private fun checkIfPlaceHasArticles(placeName: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pages = getPagesForNotification(placeName)//se llama la api para obtener los artículos relacionados al lugar

                if (pages != null) {
                    if (pages.isNotEmpty()) {
                        sendNotification(placeName, pages[0].title)//si se encuentran artículos se envía una notificación
                    } else {
                        Toast.makeText(
                            this@LocationService,
                            "No se encontraron artículos",
                            Toast.LENGTH_SHORT//en caso de que no se encuentren artículos se imprime un mensaje en pantalla
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("AAA", "Error al buscar páginas", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}