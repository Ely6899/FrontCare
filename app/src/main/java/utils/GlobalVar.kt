package utils

import android.content.Intent

object GlobalVar {
    var userId: String? = null
    var userType: Int? = null // 0 - donor, 1 - soldier
    var serverIP: String = "10.0.2.2"

    var navigateCallback: ((Intent) -> Unit)? = null

    fun navigateToPage(intent: Intent) {
        navigateCallback?.invoke(intent)
    }
}
