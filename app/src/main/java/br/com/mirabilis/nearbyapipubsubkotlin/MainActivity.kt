package br.com.mirabilis.nearbyapipubsubkotlin

import android.content.Context
import android.content.SharedPreferences
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.widget.ArrayAdapter
import br.com.mirabilis.nearbyapipubsubkotlin.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.messages.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(),
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private val recovery: String = "USER"

    /**
     * Tag for logs
     */
    private val tag = MainActivity::class.java.simpleName

    /**
     * One minutes.
     */
    private val ttlInSeconds = 60

    /**
     * Key used in writing to and reading from SharedPreferences.
     */
    private val keyUUID = "key_uuid"

    /**
     * Sets the time in seconds for a published message or a subscription to live. Set to three
     * minutes in this sample.
     */
    private val pubSubStrategy = Strategy.Builder().setTtlSeconds(ttlInSeconds).build()

    private fun getUUID(sharedPreferences: SharedPreferences): String {
        var uuid = sharedPreferences.getString(keyUUID, "")
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(keyUUID, uuid).apply()
        }
        return uuid
    }

    /**
     * The entry point to Google Play Services.
     */
    private var googleApiClient: GoogleApiClient? = null

    /**
     * The [Message] object used to broadcast information about the device to nearby devices.
     */
    private var pubMessage: Message? = null

    /**
     * A [MessageListener] for processing messages from nearby devices.
     */
    private var messageListener: MessageListener? = null

    /**
     * Adapter for working with messages from nearby publishers.
     */
    private var nearbyDevicesArrayAdapter: ArrayAdapter<String>? = null

    /**
     * User
     */
    private lateinit var user: User

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(recovery, user)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = savedInstanceState?.getSerializable(recovery) as User? ?:
                User.build(getUUID(getSharedPreferences(applicationContext.packageName,
                        Context.MODE_PRIVATE)),
                        "rodrigosimoesrosa",
                        "Brazil")

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.user = user

        messageListener = object : MessageListener() {
            override fun onFound(message: Message) {
                /**
                 * Called when a new message is found.
                 */
                nearbyDevicesArrayAdapter!!.add(
                        User.toUser(message).toString())
            }

            override fun onLost(message: Message) {
                /**
                 * Called when a message is no longer detectable nearby.
                 */
                nearbyDevicesArrayAdapter!!.remove(
                        User.toUser(message).toString())
            }
        }

        subscribeSwitch.setOnCheckedChangeListener( { _, isChecked ->

            updateInfo()

            /**
             * If GoogleApiClient is connected, perform sub actions in response to user action.
             * If it isn't connected, do nothing, and perform sub actions when it connects
             * (see onConnected()).
             */
            if (googleApiClient != null && googleApiClient!!.isConnected) {
                if (isChecked) {
                    subscribe()
                } else {
                    unsubscribe()
                }
            }
        })

        publishSwitch.setOnCheckedChangeListener( { _, isChecked ->

            updateInfo()

            /**
             * If GoogleApiClient is connected, perform pub actions in response to user action.
             * If it isn't connected, do nothing, and perform pub actions when it connects
             * (see onConnected()).
             */
            if (googleApiClient != null && googleApiClient!!.isConnected) {
                if (isChecked) {
                    publish()
                } else {
                    unpublish()
                }
            }
        })

        val nearbyDevicesArrayList = ArrayList<String>()
        nearbyDevicesArrayAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                nearbyDevicesArrayList)
        nearbyDevicesListView.adapter = nearbyDevicesArrayAdapter

        buildGoogleApiClient()
    }

    private fun updateInfo() {
        pubMessage = user.toMessage()
    }

    /**
     * Builds [GoogleApiClient], enabling automatic lifecycle management using
     * [GoogleApiClient.Builder.enableAutoManage]. I.e., GoogleApiClient connects in
     * [AppCompatActivity.onStart], or if onStart() has already happened, it connects
     * immediately, and disconnects automatically in [AppCompatActivity.onStop].
     */
    private fun buildGoogleApiClient() {
        if (googleApiClient != null) {
            return
        }
        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build()
    }

    override fun onConnected(bundle: Bundle?) {
        Log.i(tag, "GoogleApiClient connected")
        /**
         * We use the Switch buttons in the UI to track whether we were previously doing pub/sub
         * (switch buttons retain state on orientation change). Since the GoogleApiClient disconnects
         * when the activity is destroyed, foreground pubs/subs do not survive device rotation. Once
         * this activity is re-created and GoogleApiClient connects, we check the UI and pub/sub
         * again if necessary.
         */
        if (publishSwitch.isChecked) {
            publish()
        }
        if (subscribeSwitch.isChecked) {
            subscribe()
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        publishSwitch.isEnabled = false
        subscribeSwitch.isEnabled = false
        showSnackbar("Exception while connecting to Google Play services: ${connectionResult.errorMessage!!}")
    }

    override fun onConnectionSuspended(code: Int) {
        showSnackbar("Connection suspended. Error code: $code")
    }

    /**
     * Subscribes to messages from nearby devices and updates the UI if the subscription either
     * fails or TTLs.
     */
    private fun subscribe() {
        Log.i(tag, "Subscribing")
        nearbyDevicesArrayAdapter!!.clear()
        val options = SubscribeOptions.Builder()
                .setStrategy(pubSubStrategy)
                .setCallback(object : SubscribeCallback() {
                    override fun onExpired() {
                        super.onExpired()
                        Log.i(tag, "No longer subscribing")
                        runOnUiThread { subscribeSwitch.isChecked = false }
                    }
                }).build()

        Nearby.Messages
                .subscribe(googleApiClient, messageListener, options)
                .setResultCallback { status ->
                    if (status.isSuccess) {
                        Log.i(tag, "Subscribed successfully.")
                    } else {
                        showSnackbar("Could not subscribe, status = " + status)
                        subscribeSwitch.isChecked = false
                    }
                }
    }

    /**
     * Publishes a message to nearby devices and updates the UI if the publication either fails or
     * TTLs.
     */
    private fun publish() {
        Log.i(tag, "Publishing")
        val options = PublishOptions.Builder()
                .setStrategy(pubSubStrategy)
                .setCallback(object : PublishCallback() {
                    override fun onExpired() {
                        super.onExpired()
                        Log.i(tag, "No longer publishing")
                        runOnUiThread { publishSwitch.isChecked = false }
                    }
                }).build()

        Nearby.Messages.publish(googleApiClient, pubMessage, options)
                .setResultCallback { status ->
                    if (status.isSuccess) {
                        Log.i(tag, "Published successfully.")
                    } else {
                        showSnackbar("Could not publish, status = " + status)
                        publishSwitch.isChecked = false
                    }
                }
    }

    /**
     * Stops subscribing to messages from nearby devices.
     */
    private fun unsubscribe() {
        Log.i(tag, "Unsubscribing.")
        Nearby.Messages.unsubscribe(googleApiClient, messageListener)
    }

    /**
     * Stops publishing message to nearby devices.
     */
    private fun unpublish() {
        Log.i(tag, "Unpublishing.")
        Nearby.Messages.unpublish(googleApiClient, pubMessage)
    }

    /**
     * Logs a message and shows a [Snackbar] using `text`;
     *
     * @param text The text used in the Log message and the SnackBar.
     */
    private fun showSnackbar(text: String) {
        Log.w(tag, text)
        Snackbar.make(container, text, Snackbar.LENGTH_LONG).show()
    }
}
