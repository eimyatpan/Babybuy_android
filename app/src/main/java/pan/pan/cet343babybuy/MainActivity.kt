package pan.pan.cet343babybuy

import android.app.Application
import pan.pan.cet343babybuy.utils.FirebaseDatabaseHelper

// This class will be used to initialize Firebase
class MainActivity : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabaseHelper.initialize()
    }
}