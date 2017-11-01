package br.com.mirabilis.nearbyapipubsubkotlin

import android.databinding.BaseObservable
import android.databinding.Bindable
import android.os.Build
import com.google.android.gms.nearby.messages.Message
import com.google.gson.Gson
import java.io.Serializable
import java.nio.charset.Charset

/**
 * Created by rodrigosimeosrosa
 */
class User(private val model: String = Build.MODEL) : BaseObservable(), Serializable {

    companion object {
        fun build (uuid: String, username:String, nationality: String): User {
            val user = User()
            user.uuid = uuid
            user.username = username
            user.nationality = nationality
            return user
        }

        private val utf8 = "UTF-8"

        fun toUser(message: Message): User {
            val nearbyMessageString = String(message.content).trim { it <= ' ' }
            return Gson().fromJson(
                    String(nearbyMessageString.toByteArray(Charset.forName(utf8))),
                    User::class.java)
        }
    }

    private var uuid: String? = null

    @Bindable
    var username: String = String()
        set(value) {
            field = value
            notifyPropertyChanged(BR.username)
        }

    @Bindable
    var nationality: String = String()
        set(value) {
            field = value
            notifyPropertyChanged(BR.nationality)
        }

    fun toMessage() : Message = Message(Gson()
            .toJson(this)
            .toByteArray(Charset.forName(utf8)))

    override fun toString(): String {
        return "Model : $model \nUsername: $username \nNationality: $nationality"
    }
}

