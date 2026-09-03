package co.candyhouse.app.util.alertview.interfaces

import co.candyhouse.app.util.alertview.objects.AlertAction

/**
 * Created by hammad.akram on 3/14/18.
 */

interface AlertActionListener {
    fun onActionClick(action: AlertAction)
}
