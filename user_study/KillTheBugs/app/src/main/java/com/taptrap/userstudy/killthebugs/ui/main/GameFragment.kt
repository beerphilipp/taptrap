package com.taptrap.userstudy.killthebugs.ui.main

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.taptrap.userstudy.killthebugs.CustomTabHelper
import com.taptrap.userstudy.killthebugs.LevelActivity

import com.taptrap.userstudy.killthebugs.R
import com.google.android.material.snackbar.Snackbar

/**
 * Displays the main game screen with options to start the game, view instructions, and enable admin mode.
 */
class GameFragment : Fragment() {

    companion object {
        fun newInstance() = GameFragment()
    }

    private var howToRead: Boolean = false
    private var adminMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bugImageView: ImageView = view.findViewById(R.id.bugImage)

        // wait 1 second before starting the animation
        val slamX = ObjectAnimator.ofFloat(bugImageView, "scaleX", 0f, 1f)
        val slamY = ObjectAnimator.ofFloat(bugImageView, "scaleY", 0f, 1f)

        // Combine the animations into a sequence
        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(
            AnimatorSet().apply { // Slam with bounce
                playTogether(slamX, slamY)
                interpolator = OvershootInterpolator(6f) // Adjust bounce intensity
            }
        )

        animatorSet.duration = 500 // Total duration of animation

        animatorSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {
                bugImageView.visibility = ImageView.VISIBLE
            }

            override fun onAnimationEnd(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })

        // wait 1 second
        val handler = android.os.Handler()
        handler.postDelayed({
            // Start the animation
            animatorSet.start()
        }, 1000)


        // Handle clicks on the 'How To Play' button
        view.findViewById<Button>(R.id.btnHowToPlay).setOnClickListener {
            val rawUrl = getString(R.string.webapp) + "/game.html"
            Log.d("URL", rawUrl)
            CustomTabHelper(requireContext(), null).openCustomTabHowTo(rawUrl)
            howToRead = true
        }

        // Handle clicks on the 'Start' button
        view.findViewById<Button>(R.id.btnStartGame).setOnClickListener {
            if (!howToRead && !adminMode) {
                // Show a snackbar with a message
                val snackbar = Snackbar.make(view, "Hold on, cowboy! Looks like you don't know how to play yet. Tap 'How To Play' to get started!", Snackbar.LENGTH_SHORT)
                snackbar.show()
                return@setOnClickListener
            }
            // launch the MainActivity
            val intent: Intent = Intent(requireContext(), LevelActivity::class.java)
            intent.putExtra("adminMode", adminMode)
            // set an entry animation for the activity
            val bundle = Bundle().apply {
                putBoolean("adminMode", adminMode)
                putInt("level", 1)
            }
            findNavController().navigate(R.id.action_gameFragment_to_scoreFragment, bundle)
        }

        // Handle clicks on the hidden 'Admin Mode' button in the top right corner
        view.findViewById<Button>(R.id.adminMode).setOnClickListener {
            // open a dialog with two options
            AlertDialog.Builder(requireContext())
                .setMessage("Do you want to enable admin mode? If you are a participant, please do not enable this mode.")
                .setPositiveButton("Enable admin mode") { _,_ ->
                    run {
                        adminMode = true
                        view.findViewById<Button>(R.id.adminMode).text = "Admin mode enabled"
                    }
                }
                .create().show()
        }
    }
}