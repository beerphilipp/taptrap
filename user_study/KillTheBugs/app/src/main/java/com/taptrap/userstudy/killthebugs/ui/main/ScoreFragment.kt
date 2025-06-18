package com.taptrap.userstudy.killthebugs.ui.main

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.taptrap.userstudy.killthebugs.LevelActivity
import com.taptrap.userstudy.killthebugs.R

/**
 * Shows the level number at the beginning of each game leve.
 */
class ScoreFragment : Fragment() {

    companion object {
        fun newInstance() = ScoreFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_score, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val level = arguments?.getInt("level") ?: 1
        val adminMode = arguments?.getBoolean("adminMode") ?: false
        var c = 0
        when (level){
            1 -> c = resources.getColor(R.color.level1_background)
            2 -> c = resources.getColor(R.color.level2_background)
            3 -> c = resources.getColor(R.color.level3_background)
            4 -> {
                c = resources.getColor(R.color.black)
            }
        }
        view.findViewById<TextView>(R.id.levelTextView).text = "Level $level"

        view.setBackgroundColor(c)

        // wait 2 seconds, then startActivity
        view.postDelayed({
            val intent: Intent = Intent(requireContext(), LevelActivity::class.java)
            intent.putExtra("adminMode", adminMode)
            intent.putExtra("level", level)
            // set an entry animation for the activity
            val options = ActivityOptions.makeCustomAnimation(requireContext(), R.anim.regular_fade_in, R.anim.regular_fade_out).toBundle()
            startActivity(intent, options)
        }, 2000)
    }
}