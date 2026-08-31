package io.github.hcisme.vaultme.navigation

import androidx.navigation.NavController

fun NavController.navigateToEditCredential(id: Long? = null) {
    val route = if (id != null) {
        "${NavigationName.EDIT_CREDENTIAL_PAGE}?id=$id"
    } else {
        NavigationName.EDIT_CREDENTIAL_PAGE
    }
    navigate(route)
}
