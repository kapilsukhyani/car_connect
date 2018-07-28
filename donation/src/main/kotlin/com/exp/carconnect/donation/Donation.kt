package com.exp.carconnect.donation

import android.app.Activity
import android.support.design.widget.BottomSheetDialog
import kotlinx.android.synthetic.main.donation_bottom_sheet.view.*

fun showDonationBottomSheet(activity: Activity,
                            onDonationClickedListener: () -> Unit = {},
                            onLaterClickedListener: () -> Unit = {}) {
    val bottomSheetDialog = BottomSheetDialog(activity)
    bottomSheetDialog.setCancelable(true)
    val donateSheet = activity.layoutInflater.inflate(R.layout.donation_bottom_sheet, null)
    donateSheet.donateButton.setOnClickListener {
        bottomSheetDialog.dismiss()
        onDonationClickedListener()
    }
    donateSheet.laterButton.setOnClickListener {
        bottomSheetDialog.dismiss()
        onLaterClickedListener()
    }
    bottomSheetDialog.setContentView(donateSheet)
    bottomSheetDialog.show()
}