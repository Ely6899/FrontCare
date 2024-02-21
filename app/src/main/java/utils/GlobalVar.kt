package utils

import android.content.Intent

object GlobalVar {
    var userId: String? = null
    var userType: Int? = null // 0 - donor, 1 - soldier
    var serverIP: String = "192.168.1.7"

    var navigateCallback: ((Intent) -> Unit)? = null

    fun navigateToPage(intent: Intent) {
        navigateCallback?.invoke(intent)
    }
}
