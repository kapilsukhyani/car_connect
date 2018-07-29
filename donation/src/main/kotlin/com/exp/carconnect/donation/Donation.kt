package com.exp.carconnect.donation

import android.app.Activity
import android.app.Application
import android.support.design.widget.BottomSheetDialog
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.donation.state.hasUserDonated
import com.exp.carconnect.donation.state.isDonationStateLoaded
import com.exp.carconnect.donation.store.DonationStore
import io.reactivex.Single
import kotlinx.android.synthetic.main.donation_bottom_sheet.view.*
import redux.api.Store

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


interface DonationAppContract {
    val donationStore: DonationStore
}


interface ShowDonationCriteria {
    fun isFulfilled(): Single<Boolean>
}

class InstallationTimeBasedShowDonationCriteria(private val app: Application,
                                                private val store: Store<AppState>) : ShowDonationCriteria {
    companion object {
        const val THREE_DAYS_IN_MILLS = 3 * 24 * 60 * 60 * 1000
    }

    override fun isFulfilled(): Single<Boolean> {
        return store.asCustomObservable()
                .filter { it.isDonationStateLoaded() }
                .take(1)
                .map { it.hasUserDonated() }
                .filter { it }
                .map {
                    (System.currentTimeMillis() - app
                            .packageManager
                            .getPackageInfo(app.packageName, 0)
                            .firstInstallTime) > THREE_DAYS_IN_MILLS
                }
                .single(false)

    }


}