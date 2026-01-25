package com.davesspot.drinklog

import android.app.Application
import com.davesspot.drinklog.data.DrinkDatabase

class DrinkApplication : Application() {
    val database by lazy { DrinkDatabase.getDatabase(this) }
}
