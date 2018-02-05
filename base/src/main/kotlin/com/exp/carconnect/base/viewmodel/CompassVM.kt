package com.exp.carconnect.base.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.exp.carconnect.base.compass.CompassLiveData


class CompassVM(app: Application) : AndroidViewModel(app) {
    val compassLiveData = CompassLiveData(app)
}