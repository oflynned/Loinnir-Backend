package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.syzible.loinnir.R;

/**
 * Created by ed on 07/05/2017.
 */

public class RouletteFrag extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.roulette_frag, container, false);
        getActivity().setTitle(getResources().getString(R.string.app_name));

        return view;
    }
}
