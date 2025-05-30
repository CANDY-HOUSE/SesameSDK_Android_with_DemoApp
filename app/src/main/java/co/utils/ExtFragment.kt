package co.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import co.candyhouse.sesame.utils.L

private fun NavController.hasAction(actionId: Int): Boolean {
    val action = this.currentDestination?.getAction(actionId)
    return action != null || this.graph.getAction(actionId) != null
}

fun Fragment.safeNavigate(actionId: Int) {
    this.safeNavigate(actionId, null)
}

fun Fragment.safeNavigateBack() {
    if (!isAdded) return
    findNavController().navigateUp()
}

fun Fragment.safeNavigate(actionId: Int, bundle: Bundle?) {
    try {
        if (!isAdded) return
        val navController = findNavController()
        val hasAction = navController.hasAction(actionId)
        L.d("sf", "hasAction=$hasAction")
        if (hasAction) {
            navController.navigate(actionId, bundle)
        } else {
            L.d("sf", "navigation cannot be found actionId=$actionId")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

