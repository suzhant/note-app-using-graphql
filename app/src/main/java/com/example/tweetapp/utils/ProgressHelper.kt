package com.example.tweetapp.utils

import android.app.Dialog
import android.content.Context
import com.example.tweetapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ProgressHelper {

     fun buildProgressDialog(context: Context) : Dialog {
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setView(R.layout.progress_bar)
        dialog.setCancelable(false)
        return dialog.create()
    }
}