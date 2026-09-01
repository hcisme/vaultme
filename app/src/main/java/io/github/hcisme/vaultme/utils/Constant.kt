package io.github.hcisme.vaultme.utils

object Constant {
    const val ROOM_DATABASE_NAME = "VaultMe"
    const val ROOM_DATABASE_VERSION = 1

    const val DATASTORE_NAME = "vaultme_data"
    const val DATASTORE_KEY_JIANGUOYUN_ACCOUNT = "jianguoyun_account"
    const val DATASTORE_KEY_JIANGUOYUN_APP_PASSWORD = "jianguoyun_app_password"
    const val DATASTORE_KEY_JIANGUOYUN_WEBDAV_URL = "jianguoyun_webdav_url"

    const val DEFAULT_WEBDAV_URL = "https://dav.jianguoyun.com/dav/"

    const val SYNC_REMOTE_PATH = "$ROOM_DATABASE_NAME/credentials"

    const val GITHUB_API_URL = "https://api.github.com/repos/hcisme/vaultme/releases/latest"

    const val GITHUB_RELEASE_APK_NAME = "app-release.apk"
}
