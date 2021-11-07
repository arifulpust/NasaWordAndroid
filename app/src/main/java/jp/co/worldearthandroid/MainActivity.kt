package jp.co.worldearthandroid

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import gov.nasa.worldwind.NavigatorListener
import gov.nasa.worldwind.WorldWind
import gov.nasa.worldwind.WorldWind.NavigatorAction
import gov.nasa.worldwind.WorldWindow
import gov.nasa.worldwind.geom.Camera
import gov.nasa.worldwind.geom.LookAt
import gov.nasa.worldwind.globe.BasicElevationCoverage
import gov.nasa.worldwind.layer.BackgroundLayer
import gov.nasa.worldwind.layer.BlueMarbleLandsatLayer
import jp.co.worldearthandroid.experimental.AtmosphereLayer

class MainActivity : AppCompatActivity() {

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
        val fadeOut = ObjectAnimator.ofFloat(crosshairs, "alpha", 0f).setDuration(100)
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
            if (eventAction == WorldWind.NAVIGATOR_STOPPED || elapsedTime > 40) {

                // Get the current navigator state to apply to the overlays
                event.navigator.getAsLookAt(wwd.globe, lookAt)
                event.navigator.getAsCamera(wwd.globe, camera)

                // Update the overlays
                updateOverlayContents(lookAt, camera)
                updateOverlayColor(eventAction)
                lastEventTime = currentTime
            }

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
}