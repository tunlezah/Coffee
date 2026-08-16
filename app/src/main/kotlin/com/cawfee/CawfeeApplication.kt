package com.cawfee

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point; Hilt's dependency graph root. */
@HiltAndroidApp
class CawfeeApplication : Application()
