package io.github.hcisme.vaultme.utils

object Constant {
    const val ROOM_DATABASE_NAME = "VaultMe"
    const val ROOM_DATABASE_VERSION = 1

    // DataStore（应用级键值存储，不只是设置）
    const val DATASTORE_NAME = "vaultme_data"
    const val DATASTORE_KEY_JIANGUOYUN_ACCOUNT = "jianguoyun_account"
    const val DATASTORE_KEY_JIANGUOYUN_APP_PASSWORD = "jianguoyun_app_password"
    const val DATASTORE_KEY_JIANGUOYUN_WEBDAV_URL = "jianguoyun_webdav_url"

    // 坚果云
    const val DEFAULT_WEBDAV_URL = "https://dav.jianguoyun.com/dav/"
}
