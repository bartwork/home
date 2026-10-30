package ru.home.corridorpanel.sip

/** SIP / камера двери — те же дефолты, что iOS HomePanel / docs/domofon_asterisk.md. */
data class SipConfig(
    val domain: String = DEFAULT_DOMAIN,
    val username: String = DEFAULT_USERNAME,
    val password: String = DEFAULT_PASSWORD,
    val transport: String = DEFAULT_TRANSPORT,
    val doorExtension: String = DEFAULT_DOOR_EXT,
    val cameraHost: String = DEFAULT_CAMERA_HOST,
    val cameraUser: String = DEFAULT_CAMERA_USER,
    val cameraPassword: String = DEFAULT_CAMERA_PASSWORD,
) {
    val identity: String get() = "sip:$username@$domain"

    fun normalized(): SipConfig = copy(
        domain = domain.trim().ifEmpty { DEFAULT_DOMAIN },
        username = username.trim().ifEmpty { DEFAULT_USERNAME },
        transport = transport.trim().ifEmpty { DEFAULT_TRANSPORT },
        doorExtension = doorExtension.trim().ifEmpty { DEFAULT_DOOR_EXT },
        cameraHost = cameraHost.trim().ifEmpty { DEFAULT_CAMERA_HOST },
        cameraUser = cameraUser.trim().ifEmpty { DEFAULT_CAMERA_USER },
    )

    companion object {
        const val DEFAULT_DOMAIN = "192.168.1.9"
        const val DEFAULT_USERNAME = "102"
        const val DEFAULT_PASSWORD = "soft102test"
        const val DEFAULT_TRANSPORT = "udp"
        const val DEFAULT_DOOR_EXT = "100"
        const val DEFAULT_CAMERA_HOST = "192.168.1.253"
        const val DEFAULT_CAMERA_USER = "admin"
        const val DEFAULT_CAMERA_PASSWORD = "258258zzz"
    }
}

enum class SipRegistration {
    None,
    Progress,
    Ok,
    Failed,
    Cleared,
    ;

    val title: String
        get() = when (this) {
            Ok -> "SIP online"
            Progress -> "SIP…"
            Failed -> "SIP ошибка"
            Cleared -> "SIP сброшен"
            None -> "SIP выкл"
        }
}

enum class SipCallPhase {
    Idle,
    Outgoing,
    Incoming,
    Connected,
}
