package com.syamhad.bluepoint.ui

import android.Manifest
import android.animation.IntEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.syamhad.bluepoint.R
import com.syamhad.bluepoint.func.PinAdapter
import com.syamhad.bluepoint.func.PinInterface
import com.syamhad.bluepoint.func.PinModel
import kotlinx.android.synthetic.main.activity_maps.*
import org.json.JSONException
import org.json.JSONObject


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, PinInterface {

    lateinit var mMap: GoogleMap
    lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    var markerOptions = ArrayList<Marker>()

    private var pinList = ArrayList<PinModel>()
    private var mAdapter = PinAdapter(pinList, this)

    companion object {
        const val radius = 200
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    private fun setUpMap() {
        loadingbar.visibility = View.VISIBLE
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        mMap.isMyLocationEnabled = false
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
                mMap.addMarker(MarkerOptions().position(currentLatLng).title("You're here"))
                val circle = mMap.addCircle(CircleOptions().center(currentLatLng)
                    .strokeWidth(1f)
                    .strokeColor(0xFF0000)
                    .fillColor(0x55FF0000))

                val vAnimator = ValueAnimator()
                vAnimator.repeatCount = ValueAnimator.INFINITE
                vAnimator.repeatMode = ValueAnimator.RESTART
                vAnimator.setIntValues(0, 100)
                vAnimator.duration = 2000
                vAnimator.setEvaluator(IntEvaluator())
                vAnimator.interpolator = AccelerateDecelerateInterpolator()
                vAnimator.addUpdateListener { valueAnimator ->
                    val animatedFraction = valueAnimator.animatedFraction
                    circle.setRadius(animatedFraction * radius.toDouble())
                }
                vAnimator.start()

                getMarkers()
            }
            else{
                Toast.makeText(applicationContext, "Cant get current locations!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val mLayoutManager = LinearLayoutManager(applicationContext)
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL)
        recyclerView.setLayoutManager(mLayoutManager)
        recyclerView.setItemAnimator(DefaultItemAnimator())
        recyclerView.setAdapter(mAdapter)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
            }
        }
        createLocationRequest()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(baseContext, R.raw.map_style))

        setUpMap()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
    }

    private fun addMarker() {
        pinList.sortBy { pinModel -> pinModel.distance }
        for (i in 0..pinList.size - 1) {
            val latlng = LatLng(pinList[i].location.latitude, pinList[i].location.longitude)
            var iconmark = R.drawable.pinin
            if(pinList[i].distance > radius){
                iconmark = R.drawable.pinout
            }
            val currmarker = mMap.addMarker(MarkerOptions().position(latlng).title(pinList[i].name).icon(bitmapDescriptorFromVector(baseContext, iconmark)))
            markerOptions.add(currmarker)
        }
    }

    private fun getMarkers() {
        val lat = lastLocation.latitude.toString()
        val lng = lastLocation.longitude.toString()

        val endpoint =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$lat,$lng&radius=2000&key=${getString(R.string.google_maps_key)}&type=restaurant"
        val stringRequest = StringRequest(Request.Method.GET, endpoint,
            Response.Listener<String> { s ->
                try {
                    val obj = JSONObject(s)
                    val array = obj.getJSONArray("results")

                    for (i in 0..array.length() - 1) {
                        val objectResults = array.getJSONObject(i)
                        val objectLocations = objectResults.getJSONObject("geometry").getJSONObject("location")
                        val pinLocation = Location("")
                        pinLocation.latitude = objectLocations.getString("lat").toDouble()
                        pinLocation.longitude = objectLocations.getString("lng").toDouble()
                        val pin = PinModel(objectResults.getString("name"), objectResults.getString("vicinity"), lastLocation.distanceTo(pinLocation), pinLocation)
                        pinList.add(pin)
                    }
                    mAdapter.notifyDataSetChanged()
                    addMarker()

                    loadingbar.visibility = View.GONE

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }, Response.ErrorListener { volleyError -> Toast.makeText(applicationContext, volleyError.message, Toast.LENGTH_LONG).show() })

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

    override fun onItemClick(view: View, position: Int){
        val latlng = LatLng(pinList[position].location.latitude, pinList[position].location.longitude)
        markerOptions[position].showInfoWindow()
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 19f))
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
}