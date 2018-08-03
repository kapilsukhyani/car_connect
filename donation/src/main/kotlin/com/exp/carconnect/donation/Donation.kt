package com.exp.carconnect.donation

import android.app.Activity
import android.app.Application
import android.support.design.widget.BottomSheetDialog
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.CommonAppAction
import com.exp.carconnect.donation.state.DonationScreen
import com.exp.carconnect.donation.state.DonationScreenState
import com.exp.carconnect.donation.state.hasUserDonated
import com.exp.carconnect.donation.state.isDonationStateLoaded
import com.exp.carconnect.donation.store.DonationStore
import io.reactivex.Single
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.donation_bottom_sheet.view.*
import redux.api.Store

fun showDonationBottomSheet(activity: Activity,
                            store: Store<AppState>,
                            app: Application) {
    InstallationTimeBasedShowDonationCriteria(app, store)
            .isFulfilled()
            .subscribe(Consumer {
                if(it) {
                    val bottomSheetDialog = BottomSheetDialog(activity)
                    bottomSheetDialog.setCancelable(true)
                    val donateSheet = activity.layoutInflater.inflate(R.layout.donation_bottom_sheet, null)
                    donateSheet.donateButton.setOnClickListener {
                        bottomSheetDialog.dismiss()
                        store.dispatch(CommonAppAction.PushViewToBackStack(DonationScreen(DonationScreenState.ShowLoading)))
                    }
                    donateSheet.laterButton.setOnClickListener {
                        bottomSheetDialog.dismiss()
                    }
                    bottomSheetDialog.setContentView(donateSheet)
                    bottomSheetDialog.show()
                }
            })

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
                .map {
                    !it && (System.currentTimeMillis() - app
                            .packageManager
                            .getPackageInfo(app.packageName, 0)
                            .firstInstallTime) > THREE_DAYS_IN_MILLS
                }
                .single(false)

    }


}