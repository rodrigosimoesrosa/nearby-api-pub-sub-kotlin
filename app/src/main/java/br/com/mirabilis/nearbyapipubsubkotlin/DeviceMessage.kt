package br.com.mirabilis.nearbyapipubsubkotlin

import android.os.Build

import com.google.android.gms.nearby.messages.Message
import com.google.gson.Gson

import java.nio.charset.Charset

/**
 * Used to prepare the payload for a
 * [Nearby Message][com.google.android.gms.nearby.messages.Message]. Adds a unique id
 * to the Message payload, which helps Nearby distinguish between multiple devices with
 * the same model name.
 */
class DeviceMessage private constructor(private val mUUID: String) {

    val messageBody: String

    init {
        messageBody = Build.MODEL
        // TODO(developer): add other fields that must be included in the Nearby Message payload.
    }

    companion object {
        private val gson = Gson()

        /**
         * Builds a new [Message] object using a unique identifier.
         */
        fun newNearbyMessage(instanceId: String): Message {
            val deviceMessage = DeviceMessage(instanceId)
            return Message(gson.toJson(deviceMessage).toByteArray(Charset.forName("UTF-8")))
        }

        /**
         * Creates a `DeviceMessage` object from the string used to construct the payload to a
         * `Nearby` `Message`.
         */
        fun fromNearbyMessage(message: Message): DeviceMessage {
            val nearbyMessageString = String(message.content).trim { it <= ' ' }
            return gson.fromJson(
                    String(nearbyMessageString.toByteArray(Charset.forName("UTF-8"))),
                    DeviceMessage::class.java)
        }
    }
}