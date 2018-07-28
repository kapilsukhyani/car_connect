package com.exp.carconnect.donation.store

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.exp.carconnect.donation.state.DonationState
import com.google.gson.Gson
import io.reactivex.Single


interface DonationStore {
    fun getDonationState(): Single<DonationState> {
        return Single.just(DonationState.NotDonated)
    }

    fun saveDonationState(donated: DonationState.Donated): Single<DonationState.Donated>
}

class DonationStoreImpl(val preferences: SharedPreferences,
                        val gson: Gson) : DonationStore {

    companion object {
        private const val HAS_DONATED_PROPERTY_KEY = "has_donated"
        private const val DONATION_PROPERTY_KEY = "donation"
    }

    @SuppressLint("ApplySharedPref")
    override fun saveDonationState(donated: DonationState.Donated): Single<DonationState.Donated> {
        return Single.fromCallable {
            preferences
                    .edit()
                    .putString(DONATION_PROPERTY_KEY, gson.toJson(donated))
                    .commit()
            donated
        }
    }

    override fun getDonationState(): Single<DonationState> {
        return Single.fromCallable {
            if (!preferences.getBoolean(HAS_DONATED_PROPERTY_KEY, false)) {
                return@fromCallable DonationState.NotDonated
            }
            val donation = preferences.getString(DONATION_PROPERTY_KEY, "")
            if (donation.isEmpty()) {
                return@fromCallable DonationState.NotDonated
            }
            gson.fromJson(donation, DonationState.Donated::class.java)
        }
    }


}