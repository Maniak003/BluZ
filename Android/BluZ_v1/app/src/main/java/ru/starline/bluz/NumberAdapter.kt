package ru.starline.bluz

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Created by ed on 18,июнь,2024
 */
class NumberAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): kotlin.Int = 5
    override fun createFragment(position: kotlin.Int): androidx.fragment.app.Fragment {
        val fragment = NumberFragment()
        fragment.arguments = Bundle().apply {
            putInt(ARG_OBJECT, position)
            pagerFrame = position
        }
        return fragment
    }

}

