package com.syzible.loinnir.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.syzible.loinnir.R;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragment;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

/**
 * Created by ed on 11/06/2017.
 */

public class IntroductionActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(introductionSlide());
        addSlide(locationSlide());
        addSlide(chatSlide());
        addSlide(permissionsSlide());
        addSlide(getStartedSlide());
    }

    @Override
    public void onFinish() {
        super.onFinish();
        startActivity(new Intent(this, AuthenticationActivity.class));
    }

    private SlideFragment introductionSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.amber500)
                .buttonsColor(R.color.blue500)
                .image(R.drawable.logo_small)
                .title("Fáilte go dtí Loinnir")
                .description("An chéad aip shóisialta don Ghaeilge.\nFionn. Nasc. Braith.")
                .build();
    }

    private SlideFragment locationSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.blue500)
                .buttonsColor(R.color.amber500)
                .image(R.drawable.ic_map_pin_white)
                .title("Ceantar")
                .description("Baintear úsáid as an gceantar garbh chun a fheiceáil ar an léarscáil cá bhfuil úsáideoirí eile lonnaithe")
                .build();
    }

    private SlideFragment chatSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.amber500)
                .buttonsColor(R.color.blue500)
                .image(R.drawable.ic_conversation_white)
                .title("I mBun Allagair")
                .description("Labhair le h-úsáideoirí atá gar duit sa cheantar céanna, nó trí rúiléid chun bualadh le daoine fánacha.")
                .build();
    }

    private SlideFragment permissionsSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.blue500)
                .buttonsColor(R.color.amber500)
                .image(R.drawable.ic_permissions_white)
                .title("Ceadanna")
                .description("Iarraimid ort glacadh le ceadanna an cheantair ionas go mbeidh an eispéireas is fearr agat don aip.")
                .neededPermissions(new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                })
                .build();
    }

    private SlideFragment getStartedSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.amber500)
                .buttonsColor(R.color.blue500)
                .image(R.drawable.ic_facebook_white)
                .title("Cúntas Facebook")
                .description("Tá cúntas Facebook riachtanach le síniú isteach ar sheirbhísí Loinnir.")
                .build();
    }

}
