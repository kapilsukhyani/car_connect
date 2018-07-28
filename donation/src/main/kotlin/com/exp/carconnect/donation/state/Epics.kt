package com.exp.carconnect.donation.state

import com.exp.carconnect.base.AppState
import com.exp.carconnect.donation.store.DonationStore
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import redux.INIT
import redux.api.Store
import redux.observable.Epic

class DonationStateLoadingEpic(private val ioScheduler: Scheduler,
                               private val mainThreadScheduler: Scheduler,
                               private val donationStore: DonationStore) : Epic<AppState> {

    override fun map(actions: Observable<out Any>, store: Store<AppState>): Observable<out Any> {
        return actions
                .filter { it == INIT }
                .take(1)
                .flatMap {
                    Single.concat(
                            Single.just(DonationAction.LoadingDonationState),
                            donationStore
                                    .getDonationState()
                                    .map<DonationAction> {
                                        DonationAction.LoadedDonationPersistedState(DonationPersistedState(it))
                                    }
                                    .onErrorReturn {
                                        DonationAction.DonationStateLoadingError(it)
                                    }
                                    .subscribeOn(ioScheduler))
                            .observeOn(mainThreadScheduler)
                            .toObservable()
                }

    }


}

class UpdateDonationEpic(private val ioScheduler: Scheduler,
                         private val mainThreadScheduler: Scheduler,
                         private val donationStore: DonationStore) : Epic<AppState> {

    override fun map(actions: Observable<out Any>, store: Store<AppState>): Observable<out Any> {
        return actions
                .filter { it is DonationAction.StoreDonation }
                .flatMap {
                    val action = it as DonationAction.StoreDonation
                    Single.concat(
                            Single.just(DonationAction.UpdatingDonationStatus),
                            donationStore
                                    .saveDonationState(DonationState.Donated(action.productId, action.orderId,
                                            action.purchaseTime, action.purchaseToken))
                                    .map<DonationAction> {
                                        DonationAction.UpdateStateToDonated(it)
                                    }
                                    .onErrorReturn {
                                        DonationAction.UpdateDonationError(it)
                                    }
                                    .subscribeOn(ioScheduler))
                            .observeOn(mainThreadScheduler)
                            .toObservable()
                }

    }


}