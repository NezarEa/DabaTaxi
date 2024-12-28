package com.dabataxi

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ObjectAnimator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.dabataxi.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var splashScreen: androidx.core.splashscreen.SplashScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splashScreen = installSplashScreen()
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        val mainView = findViewById<View>(R.id.nav_host_fragment)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                "translationY",
                0f,
                -splashScreenViewProvider.view.height.toFloat()
            ).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 1000L
                doOnEnd {
                    splashScreenViewProvider.remove()
                }
            }
            slideUp.start()
        }

        navigateToAppropriateScreen()
    }

    private fun navigateToAppropriateScreen() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isOnboardingFinished = onBoardingIsFinished()

        Log.d("MainActivity", "User: $currentUser, Onboarding Finished: $isOnboardingFinished")

        when {
            currentUser != null && isOnboardingFinished -> {
                navigateToHome()
            }
            currentUser != null -> {
                navigateToOnBoarding()
            }
            else -> {
                navController.navigate(R.id.home2)
            }
        }
    }

    private fun navigateToHome() {
        navController.navigate(R.id.homeAppCST)
    }

    private fun navigateToOnBoarding() {
        navController.navigate(R.id.onBoarding)
    }
    private fun onBoardingIsFinished(): Boolean {
        val sharedPreferences = getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        val isFinished = sharedPreferences.getBoolean("finished", false)
        Log.d("MainActivity", "Onboarding finished: $isFinished")
        return isFinished
    }
}
