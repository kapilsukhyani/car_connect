package com.exp.carconnect.basic.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.exp.carconnect.basic.compass.CompassLiveData


class CompassVM(app: Application) : AndroidViewModel(app) {
    val compassLiveData = CompassLiveData(app)
}