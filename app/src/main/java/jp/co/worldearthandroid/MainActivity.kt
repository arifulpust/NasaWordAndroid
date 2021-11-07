package jp.co.worldearthandroid

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.chip.ChipGroup
import gov.nasa.worldwind.NavigatorListener
import gov.nasa.worldwind.WorldWind
import gov.nasa.worldwind.WorldWind.NavigatorAction
import gov.nasa.worldwind.WorldWindow
import gov.nasa.worldwind.geom.Camera
import gov.nasa.worldwind.geom.Location
import gov.nasa.worldwind.geom.LookAt
import gov.nasa.worldwind.geom.Position
import gov.nasa.worldwind.globe.BasicElevationCoverage
import gov.nasa.worldwind.layer.BackgroundLayer
import gov.nasa.worldwind.layer.BlueMarbleLandsatLayer
import gov.nasa.worldwind.layer.BlueMarbleLayer
import gov.nasa.worldwind.layer.RenderableLayer
import jp.co.worldearthandroid.experimental.AtmosphereLayer
import java.util.HashMap

class MainActivity : AppCompatActivity() , FrameCallback {
    //views
    private val renderableLayer //layer in which we can render our satellites and locations
            : RenderableLayer? = null

    //private Placemark lastplackmark;
    private val scaleUp = false

    private val mSatelliteChipGroup: ChipGroup? = null

    //day night Animation settings
    private val sunLocation = Location(1.6, 18.6)

    //protected double cameraDegreesPerSecond = 2.0;
    protected var lightLatDegreesPerSec =
        (0 + 15 / 60 + 0.1 / 3600) / 60 //dif 0° 15' 00.1" direction west

    protected var lightLngDegreesPerSec = ((0 + 0 / 60 + 1 / 3600) / 60 //dif 0° 00' 01.0" south
            ).toDouble()
    protected var lastFrameTimeNanos: Long = 0
    private val atmosphereLayer: AtmosphereLayer? = null
    protected var fragmentPaused = false

    /**
     * This protected member allows derived classes to override the resource used in setContentView.
     */
    protected var layoutResourceId: Int = R.layout.activity_main

    /**
     * The WorldWindow (GLSurfaceView) maintained by this activity
     */
    protected var wwd: WorldWindow? = null

    // UI elements
    protected var latView: TextView? = null
    protected var lonView: TextView? = null
    protected var altView: TextView? = null
    protected var crosshairs: ImageView? = null
    protected var overlay: ViewGroup? = null
    protected var toolbar: Toolbar? = null

    // Use pre-allocated navigator state objects to avoid per-event memory allocations
    private val lookAt = LookAt()
    private val camera = Camera()

    // Track the navigation event time so the overlay refresh rate can be throttled
    private var lastEventTime: Long = 0

    // Animation object used to fade the overlays
    private var animatorSet: AnimatorSet? = null
    private var crosshairsActive = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establish the activity content

        // Establish the activity content
        setContentView(layoutResourceId)


        // Create the WorldWindow (a GLSurfaceView) which displays the globe.

        // Create the WorldWindow (a GLSurfaceView) which displays the globe.
        wwd = WorldWindow(this)
      //  wwd.RenderableLayer


        // Add the WorldWindow view object to the layout that was reserved for the globe.

        // Add the WorldWindow view object to the layout that was reserved for the globe.
        val globeLayout = findViewById(R.id.globe) as FrameLayout
        globeLayout.addView(wwd)

        // Setup the WorldWindow's layers.

        // Setup the WorldWindow's layers.
        wwd!!.layers.addLayer(BackgroundLayer())
        wwd!!.layers.addLayer(BlueMarbleLandsatLayer())
        wwd!!.layers.addLayer(AtmosphereLayer())

        // Setup the WorldWindow's elevation coverages.

        // Setup the WorldWindow's elevation coverages.
       wwd!!.globe.elevationModel.addCoverage(BasicElevationCoverage())


        // Initialize the UI elements that we'll update upon the navigation events
        crosshairs = findViewById(R.id.globe_crosshairs) as ImageView
        overlay = findViewById(R.id.globe_status) as ViewGroup
        crosshairs!!.visibility = View.VISIBLE
        overlay!!.visibility = View.VISIBLE
        latView = findViewById(R.id.lat_value) as TextView
        lonView = findViewById(R.id.lon_value) as TextView
        altView = findViewById(R.id.alt_value) as TextView
        toolbar = findViewById(R.id.toolbar) as Toolbar
        toolbar?.setTitle("NASA World ")
        val fadeOut = ObjectAnimator.ofFloat(crosshairs, "alpha", 0f).setDuration(1500)
        fadeOut.startDelay = 500.toLong()
        animatorSet = AnimatorSet()
        animatorSet!!.play(fadeOut)

        // Create a simple Navigator Listener that logs navigator events emitted by the WorldWindow.

        // Create a simple Navigator Listener that logs navigator events emitted by the WorldWindow.
        val listener = NavigatorListener { wwd, event ->
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastEventTime
            val eventAction = event.action
            val receivedUserInput =
                eventAction == WorldWind.NAVIGATOR_MOVED && event.lastInputEvent != null

            // Update the status overlay views whenever the navigator stops moving,
            // and also it is moving but at an (arbitrary) maximum refresh rate of 20 Hz.
            if (eventAction == WorldWind.NAVIGATOR_STOPPED || elapsedTime > 50) {

                // Get the current navigator state to apply to the overlays
                event.navigator.getAsLookAt(wwd.globe, lookAt)
                event.navigator.getAsCamera(wwd.globe, camera)

                // Update the overlays
                updateOverlayContents(lookAt, camera)
                updateOverlayColor(eventAction)
                lastEventTime = currentTime
            }
            Log.e("zome",wwd.navigator.latitude.toString()+"  "+wwd.navigator.longitude+" \n "+wwd.navigator.altitude.toString())

            // Show the crosshairs while the user is gesturing and fade them out after the user stops
            if (receivedUserInput) {
                showCrosshairs()
            } else {
                fadeCrosshairs()
            }
        }

        // Register the Navigator Listener with the activity's WorldWindow.

        // Register the Navigator Listener with the activity's WorldWindow.

        getWorldWindow()!!.addNavigatorListener(listener)
        var location=getLocation()
        var position :Position?=null
        if(location!=null) {

             position = Position()
            position.latitude=location.lat
            position.longitude=location.long
        }else {
            position = Position()
            position.latitude=23.545
            position.longitude=90.432
        }
        //position.latitude=23.54587765
        //position.longitude=90.43288765
//            moveCamera(
//                position!!,
//                1000,
//                "sat",
//                10.0,
//                05.0
//            ) //as data difference is 60 sec

            wwd!!.getNavigator().setLatitude( position.latitude)
            wwd!!.getNavigator().setLongitude( position.longitude)
            wwd!!.getNavigator().setAltitude(2.6214752977566265E7) //converts to meter agian
    }
    var startSatAnimation = false //after camera moved to activated satellite position start moving the satellite

    var  activeCameraValueAnimator :ValueAnimator?=null
    fun moveCamera(
        position: Position,
        moveDuration: Long,
        mode: String,
        prevV: Double,
        currentV: Double
    ) {
        //debug
        // position.altitude = 1e7*2.1;
        val currentCameraLat: Double = wwd?.getNavigator()!!.getLatitude()
        val currentCameraLng: Double = wwd?.getNavigator()!!.getLongitude()
        val currentCameraHeight: Double =
            wwd!!.getNavigator().getAltitude() / 1000 //converts to km
        activeCameraValueAnimator = ValueAnimator.ofFloat(
            currentCameraLat.toFloat(),
            position.latitude.toFloat()
        )
        activeCameraValueAnimator?.setInterpolator(LinearInterpolator())
        activeCameraValueAnimator?.addUpdateListener(AnimatorUpdateListener { animation ->
            if (fragmentPaused) return@AnimatorUpdateListener
            val percent = animation.animatedFraction
            //Log.w(TAG," sat lat percent :"+percent);
            val lat = (1 - percent) * currentCameraLat + percent * position.latitude
            val lng = (1 - percent) * currentCameraLng + percent * position.longitude
            val altitude = (1 - percent) * currentCameraHeight + percent * position.altitude
            val velocity = (1 - percent) * prevV + percent * currentV
            wwd!!.getNavigator().setLatitude(lat)
            wwd!!.getNavigator().setLongitude(lng)
            wwd!!.getNavigator().setAltitude(altitude * 1000) //converts to meter agian
            if (mode == "sat") {
                val dataMap: MutableMap<String, Any> = HashMap()
                dataMap["satName"] =""
                dataMap["lat"] = lat
                dataMap["lng"] = lng
                dataMap["height"] = altitude
                dataMap["velocity"] = velocity
                dataMap["timestamp"] = System.currentTimeMillis()
                val currentSatPos = Position(lat, lng, position.altitude * 1000)
                // removeSatellite(lastplackmark);
                //lastplackmark = GlobeUtils.addSatelliteToRenderableLayer(renderableLayer,currentSatPos,R.drawable.satellite_one);
               // mapsActivity.updateUI(dataMap)
            }
        })
        activeCameraValueAnimator!!.setDuration(moveDuration)
        activeCameraValueAnimator!!.start()
    }
    /**
     * Makes the crosshairs visible.
     */
    protected fun showCrosshairs() {
        if (animatorSet!!.isStarted) {
            animatorSet!!.cancel()
        }
        crosshairs!!.alpha = 1.0f
        crosshairsActive = true
    }

    /**
     * Fades the crosshairs using animation.
     */
    protected fun fadeCrosshairs() {
        if (crosshairsActive) {
            crosshairsActive = false
            if (!animatorSet!!.isStarted) {
                animatorSet!!.start()
            }
        }
    }

    /**
     * Displays navigator state information in the status overlay views.
     *
     * @param lookAt Where the navigator is looking
     * @param camera Where the camera is positioned
     */
    protected fun updateOverlayContents(lookAt: LookAt, camera: Camera) {
        latView!!.text = formatLatitude(lookAt.latitude)
        lonView!!.text = formatLongitude(lookAt.longitude)
        altView!!.text = formatAltitude(camera.altitude)
    }
    fun createWorldWindow(): WorldWindow? {
        this.wwd = WorldWindow(this)
        wwd?.getLayers()?.addLayer(BlueMarbleLayer())
        wwd?.getLayers()?.addLayer(BlueMarbleLandsatLayer())
        return wwd
    }
    /**
     * Brightens the colors of the overlay views when when user input occurs.
     *
     * @param eventAction The action associated with this navigator event
     */
    protected fun updateOverlayColor(@NavigatorAction eventAction: Int) {
        val color =
            if (eventAction == WorldWind.NAVIGATOR_STOPPED) -0x5f000100 /*semi-transparent yellow*/ else Color.YELLOW
        latView!!.setTextColor(color)
        lonView!!.setTextColor(color)
        altView!!.setTextColor(color)
    }

    protected fun formatLatitude(latitude: Double): String? {
        val sign = Math.signum(latitude).toInt()
        return String.format("%6.3f°%s", latitude * sign, if (sign >= 0.0) "N" else "S")
    }

    protected fun formatLongitude(longitude: Double): String? {
        val sign = Math.signum(longitude).toInt()
        return String.format("%7.3f°%s", longitude * sign, if (sign >= 0.0) "E" else "W")
    }

    protected fun formatAltitude(altitude: Double): String? {
        return String.format(
            "Eye: %,.0f %s",
            if (altitude < 100000) altitude else altitude / 1000,
            if (altitude < 100000) "m" else "km"
        )
    }
    override fun onPause() {
        super.onPause()
        wwd!!.onPause() // pauses the rendering thread
    }

    override fun onResume() {
        super.onResume()
        wwd!!.onResume() // resumes a paused rendering thread
    }


    fun getWorldWindow(): WorldWindow? {
        return wwd
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (this.lastFrameTimeNanos != 0L) {
Log.e("globe","called")
            // Compute the frame duration in seconds.
            val frameDurationSeconds: Double = (frameTimeNanos - this.lastFrameTimeNanos) * 1.0e-9
            // double cameraDegrees = (frameDurationSeconds * this.cameraDegreesPerSecond);

            //find lat and lng difference in degree
            val lightLatDiffDegrees: Double = frameDurationSeconds * this.lightLatDegreesPerSec
            val lightLngDiffDegrees: Double = frameDurationSeconds * this.lightLngDegreesPerSec
            this.sunLocation.latitude -= lightLatDiffDegrees
            val lat: Double =
                if (this.sunLocation.latitude < -90) -this.sunLocation.latitude - 90 else this.sunLocation.latitude
            this.sunLocation.longitude -= lightLngDiffDegrees
            val lng: Double =
                if (this.sunLocation.longitude < -180) -this.sunLocation.longitude - 180 else this.sunLocation.longitude

            // Move the navigator to simulate the Earth's rotation about its axis.
            val navigator = getWorldWindow()!!.navigator
            //navigator.setLongitude(navigator.getLongitude() - cameraDegrees);

            // Move the sun location to simulate the Sun's rotation about the Earth.
            this.sunLocation.set(lat, lng)
            this.atmosphereLayer?.setLightLocation(this.sunLocation)

            // Redraw the WorldWindow to display the above changes.
            getWorldWindow()!!.requestRedraw()
        }

        // if the fragment is not stopped then call the function again to
        // continue the animation

        // if the fragment is not stopped then call the function again to
        // continue the animation
        if (!this.fragmentPaused) {
            Choreographer.getInstance().postFrameCallback(this)
        } else {
            Choreographer.getInstance().removeFrameCallback(this)
        }

        //after satellite data loaded

        //after satellite data loaded
//        if (activeSatDataList.size > 0 && !this.fragmentPaused) {
//            animateSatellite()
//        }


        this.lastFrameTimeNanos = frameTimeNanos
    }


    /**
     * locate the satellite and navigator camera accordingly after satellite data is initialized
     * called from fetchSatDataFromSSE method
     */
}