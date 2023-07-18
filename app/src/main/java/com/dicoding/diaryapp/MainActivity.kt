package com.dicoding.diaryapp

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.dicoding.diaryapp.navigation.Screen
import com.dicoding.diaryapp.navigation.SetupNavGraph
import com.dicoding.diaryapp.ui.theme.DiaryAppTheme
import com.dicoding.diaryapp.util.Constants.APP_ID
import io.realm.kotlin.mongodb.App

class MainActivity : ComponentActivity() {

    var keepSplashOpened = true
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition{
            keepSplashOpened
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)


        setContent {
            DiaryAppTheme {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController,
                    onDataLoaded = {
                        keepSplashOpened = false
                    }
                )
            }
        }
    }
}

private fun getStartDestination(): String {
    val user = App.create(APP_ID).currentUser
    return  if(user != null && user.loggedIn) Screen.Home.route
    else Screen.Authentication.route
}
