package com.dabataxi

import android.content.Context
import android.os.Bundle
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
        splashScreen = installSplashScreen()  // Initialize the splash screen
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Enables edge-to-edge support (making use of the whole screen)
        setContentView(R.layout.activity_main)  // Set the activity's layout

        // Apply window insets to adjust padding according to system bars
        val mainView = findViewById<View>(R.id.nav_host_fragment)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the NavController with the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up the exit animation for the splash screen
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenViewProvider.view,
                "translationY",
                0f,
                -splashScreenViewProvider.view.height.toFloat()
            ).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 1000L  // Slide-up animation duration
                doOnEnd {
                    splashScreenViewProvider.remove()  // Remove splash screen after the animation
                }
            }
            slideUp.start()  // Start the animation
        }

        // Check user login status and navigate accordingly
        navigateToAppropriateScreen()
    }

    // Navigate to the appropriate screen based on user authentication and onboarding status
    private fun navigateToAppropriateScreen() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is logged in
            if (onBoardingIsFinished()) {
                navigateToHome()  // Navigate to the home screen if onboarding is finished
            } else {
                navigateToOnBoarding()  // Otherwise, show the onboarding screen
            }
        } else {
            navController.navigate(R.id.home2)
        }
    }

    // Navigate to the home screen (customer's home)
    private fun navigateToHome() {
        navController.navigate(R.id.homeAppCST)  // Navigate to customer home
    }

    // Navigate to the onboarding screen
    private fun navigateToOnBoarding() {
        navController.navigate(R.id.onBoarding)  // Navigate to onboarding screen
    }

    // Check if the onboarding process has been finished
    private fun onBoardingIsFinished(): Boolean {
        val sharedPreferences = getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("finished", false)  // Return true if finished, false otherwise
    }
}
