package com.fishingcopilot.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.fishingcopilot.R
import com.fishingcopilot.data.profile.Avatar
import com.fishingcopilot.data.profile.FishingStyle
import com.fishingcopilot.data.profile.Species

@get:DrawableRes
val Avatar.icon: Int
    get() = when (this) {
        Avatar.JETTY -> R.drawable.ic_avatar_jetty_3d
        Avatar.KELONG -> R.drawable.ic_avatar_kelong_3d
        Avatar.RAFT_HOUSE -> R.drawable.ic_avatar_raft_house_3d
        Avatar.BOAT -> R.drawable.ic_avatar_boat_3d
        Avatar.KAYAK -> R.drawable.ic_avatar_kayak_3d
        Avatar.ROD -> R.drawable.ic_avatar_rod_3d
        Avatar.FISH -> R.drawable.ic_avatar_fish_3d
        Avatar.ANCHOR -> R.drawable.ic_avatar_anchor_3d
    }

val Avatar.accent: Color
    get() = when (this) {
        Avatar.JETTY -> Color(0xFF00E5FF)
        Avatar.KELONG -> Color(0xFFFF80AB)
        Avatar.RAFT_HOUSE -> Color(0xFFFFAB40)
        Avatar.BOAT -> Color(0xFF00E676)
        Avatar.KAYAK -> Color(0xFFFFD600)
        Avatar.ROD -> Color(0xFFFF8A65)
        Avatar.FISH -> Color(0xFFB388FF)
        Avatar.ANCHOR -> Color(0xFF82B1FF)
    }

@get:StringRes
val Avatar.label: Int
    get() = when (this) {
        Avatar.JETTY -> R.string.avatar_jetty
        Avatar.KELONG -> R.string.avatar_kelong
        Avatar.RAFT_HOUSE -> R.string.avatar_raft_house
        Avatar.BOAT -> R.string.avatar_boat
        Avatar.KAYAK -> R.string.avatar_kayak
        Avatar.ROD -> R.string.avatar_rod
        Avatar.FISH -> R.string.avatar_fish
        Avatar.ANCHOR -> R.string.avatar_anchor
    }

// Style cards reuse the avatar and species artwork rather than shipping duplicate images.
@get:DrawableRes
val FishingStyle.icon: Int
    get() = when (this) {
        FishingStyle.SHORE -> Avatar.JETTY.icon
        FishingStyle.KELONG -> Avatar.KELONG.icon
        FishingStyle.RAFT -> Avatar.RAFT_HOUSE.icon
        FishingStyle.BOAT -> Avatar.BOAT.icon
        FishingStyle.ESTUARY -> Species.SIAKAP.imageRes
        FishingStyle.KAYAK -> Avatar.KAYAK.icon
    }

@get:StringRes
val FishingStyle.label: Int
    get() = when (this) {
        FishingStyle.SHORE -> R.string.style_shore
        FishingStyle.KELONG -> R.string.style_kelong
        FishingStyle.RAFT -> R.string.style_raft
        FishingStyle.BOAT -> R.string.style_boat
        FishingStyle.ESTUARY -> R.string.style_estuary
        FishingStyle.KAYAK -> R.string.style_kayak
    }

@get:StringRes
val FishingStyle.description: Int
    get() = when (this) {
        FishingStyle.SHORE -> R.string.style_shore_description
        FishingStyle.KELONG -> R.string.style_kelong_description
        FishingStyle.RAFT -> R.string.style_raft_description
        FishingStyle.BOAT -> R.string.style_boat_description
        FishingStyle.ESTUARY -> R.string.style_estuary_description
        FishingStyle.KAYAK -> R.string.style_kayak_description
    }

@get:StringRes
val FishingStyle.tag: Int
    get() = when (this) {
        FishingStyle.SHORE -> R.string.style_shore_tag
        FishingStyle.KELONG -> R.string.style_kelong_tag
        FishingStyle.RAFT -> R.string.style_raft_tag
        FishingStyle.BOAT -> R.string.style_boat_tag
        FishingStyle.ESTUARY -> R.string.style_estuary_tag
        FishingStyle.KAYAK -> R.string.style_kayak_tag
    }

@get:DrawableRes
val Species.imageRes: Int
    get() = when (this) {
        Species.SIAKAP -> R.drawable.ic_fish_siakap_3d
        Species.JENAHAK -> R.drawable.ic_fish_jenahak_3d
        Species.KERAPU -> R.drawable.ic_fish_kerapu_3d
        Species.TENGGIRI -> R.drawable.ic_fish_tenggiri_3d
        Species.PARI -> R.drawable.ic_fish_pari_3d
        Species.TALANG -> R.drawable.ic_fish_talang_3d
        Species.IKAN_MERAH -> R.drawable.ic_fish_ikan_merah_3d
        Species.BELUKANG -> R.drawable.ic_fish_belukang_3d
    }

@get:StringRes
val Species.label: Int
    get() = when (this) {
        Species.SIAKAP -> R.string.species_siakap
        Species.JENAHAK -> R.string.species_jenahak
        Species.KERAPU -> R.string.species_kerapu
        Species.TENGGIRI -> R.string.species_tenggiri
        Species.PARI -> R.string.species_pari
        Species.TALANG -> R.string.species_talang
        Species.IKAN_MERAH -> R.string.species_ikan_merah
        Species.BELUKANG -> R.string.species_belukang
    }

@get:StringRes
val Species.subtitle: Int
    get() = when (this) {
        Species.SIAKAP -> R.string.species_siakap_sub
        Species.JENAHAK -> R.string.species_jenahak_sub
        Species.KERAPU -> R.string.species_kerapu_sub
        Species.TENGGIRI -> R.string.species_tenggiri_sub
        Species.PARI -> R.string.species_pari_sub
        Species.TALANG -> R.string.species_talang_sub
        Species.IKAN_MERAH -> R.string.species_ikan_merah_sub
        Species.BELUKANG -> R.string.species_belukang_sub
    }

@get:StringRes
val Species.habitat: Int
    get() = when (this) {
        Species.SIAKAP -> R.string.species_siakap_habitat
        Species.JENAHAK -> R.string.species_jenahak_habitat
        Species.KERAPU -> R.string.species_kerapu_habitat
        Species.TENGGIRI -> R.string.species_tenggiri_habitat
        Species.PARI -> R.string.species_pari_habitat
        Species.TALANG -> R.string.species_talang_habitat
        Species.IKAN_MERAH -> R.string.species_ikan_merah_habitat
        Species.BELUKANG -> R.string.species_belukang_habitat
    }
