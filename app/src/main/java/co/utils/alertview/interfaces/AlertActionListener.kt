package co.utils.alertview.interfaces

import co.utils.alertview.objects.AlertAction

/**
 * Created by hammad.akram on 3/14/18.
 */

interface AlertActionListener {
    fun onActionClick(action: AlertAction)
}
