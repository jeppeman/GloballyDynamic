package com.example.locallydynamic.installtimefeature

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.play.core.splitcompat.SplitCompat

class InstallTimeFragment : Fragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        SplitCompat.install(context)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_install_time_feature, container, false)
    }
}