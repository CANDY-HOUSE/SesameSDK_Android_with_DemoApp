package co.utils.alertview.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import co.candyhouse.app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertTheme
import co.utils.alertview.objects.AlertAction

import java.util.*

/**
 * Created by hammad.akram on 3/14/18.
 */
@SuppressLint("ValidFragment")
class IosDialogFragment(private val title: String, private val message: String, private val actions: ArrayList<AlertAction>, private val theme: AlertTheme) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // Inflate view according to theme selected. Default is AlertTheme.LIGHT
        var view: View? = null
        if (theme == AlertTheme.LIGHT)
            view = inflater.inflate(R.layout.alert_layout_light, container, false)
        else if (theme == AlertTheme.DARK)
            view = inflater.inflate(R.layout.alert_layout_dark, container, false)


        // Set up view
        initView(view)

        return view
    }

    private fun initView(view: View?) {
        view?.apply {
            view.findViewById<TextView>(R.id.tvTitle).text = title
            view.findViewById<TextView>(R.id.tvMessage).text = message

            // In case of title or message is empty
            if (title.isEmpty()) view.findViewById<TextView>(R.id.tvTitle).visibility = View.GONE
            if (message.isEmpty()) view.findViewById<TextView>(R.id.tvMessage).visibility = View.GONE

            view.findViewById<View>(R.id.tvCancel)       .visibility = View.GONE

            // Inflate action views 

            inflateActionsView(view.findViewById(R.id.actionsLayout), actions)
        
        }
        
    }

    /**
     * Inflate action views
     */
    private fun inflateActionsView(actionsLayout: LinearLayout, actions: ArrayList<AlertAction>) {
        for (action in actions) {

            // Inflate action view according to theme selected
            var view: View? = null
            if (theme == AlertTheme.LIGHT)
                view = LayoutInflater.from(context).inflate(R.layout.action_layout_light, null)
            else if (theme == AlertTheme.DARK)
                view = LayoutInflater.from(context).inflate(R.layout.action_layout_dark, null)
            
            view?.apply {
                view.findViewById<TextView>(R.id.tvAction).text = action.title
                view.findViewById<TextView>(R.id.tvAction).setOnClickListener {
                    dismiss()

                    // For Kotlin
                    action.action?.invoke(action)

                    // For Java
                    action.actionListener?.onActionClick(action)
                }

                // Action text color according to AlertActionStyle
                if (context != null) {
                    when (action.style) {
                        AlertActionStyle.POSITIVE -> {
                            view.findViewById<TextView>(R.id.tvAction).setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                        }
                        AlertActionStyle.NEGATIVE -> {
                            view.findViewById<TextView>(R.id.tvAction).setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                        }
                        AlertActionStyle.DEFAULT -> {
                            if (theme == AlertTheme.LIGHT)
                                view.findViewById<TextView>(R.id.tvAction).setTextColor(ContextCompat.getColor(requireContext(), R.color.darkGray))
                            else if (theme == AlertTheme.DARK)
                                view.findViewById<TextView>(R.id.tvAction).setTextColor(ContextCompat.getColor(requireContext(), R.color.lightWhite))
                        }

                    }
                }
                actionsLayout.addView(view)
            }

      

            // Click listener for action.
          

            // Add view to layout

        }
    }
}


