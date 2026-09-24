package com.fishingcopilot.ui.components

import androidx.annotation.StringRes
import com.fishingcopilot.R
import com.fishingcopilot.data.spots.CoastalArea
import com.fishingcopilot.data.spots.MalaysianState

@get:StringRes
val CoastalArea.label: Int
    get() = when (this) {
        CoastalArea.KUALA_PERLIS -> R.string.area_kuala_perlis
        CoastalArea.KUALA_KEDAH -> R.string.area_kuala_kedah
        CoastalArea.PULAU_PINANG -> R.string.area_pulau_pinang
        CoastalArea.LUMUT_PANGKOR -> R.string.area_lumut_pangkor
        CoastalArea.KUALA_SELANGOR -> R.string.area_kuala_selangor
        CoastalArea.PELABUHAN_KLANG -> R.string.area_pelabuhan_klang
        CoastalArea.PORT_DICKSON -> R.string.area_port_dickson
        CoastalArea.MELAKA -> R.string.area_melaka
        CoastalArea.MUAR -> R.string.area_muar
        CoastalArea.KUKUP -> R.string.area_kukup
        CoastalArea.DESARU -> R.string.area_desaru
        CoastalArea.MERSING -> R.string.area_mersing
        CoastalArea.PULAU_TIOMAN -> R.string.area_pulau_tioman
        CoastalArea.KUANTAN -> R.string.area_kuantan
        CoastalArea.KEMAMAN -> R.string.area_kemaman
        CoastalArea.KUALA_TERENGGANU -> R.string.area_kuala_terengganu
        CoastalArea.TOK_BALI -> R.string.area_tok_bali
        CoastalArea.SANTUBONG -> R.string.area_santubong
        CoastalArea.MIRI -> R.string.area_miri
        CoastalArea.LABUAN -> R.string.area_labuan
        CoastalArea.KOTA_KINABALU -> R.string.area_kota_kinabalu
        CoastalArea.KUDAT -> R.string.area_kudat
        CoastalArea.SANDAKAN -> R.string.area_sandakan
        CoastalArea.SEMPORNA -> R.string.area_semporna
    }

@get:StringRes
val MalaysianState.label: Int
    get() = when (this) {
        MalaysianState.PERLIS -> R.string.state_perlis
        MalaysianState.KEDAH -> R.string.state_kedah
        MalaysianState.PULAU_PINANG -> R.string.state_pulau_pinang
        MalaysianState.PERAK -> R.string.state_perak
        MalaysianState.SELANGOR -> R.string.state_selangor
        MalaysianState.NEGERI_SEMBILAN -> R.string.state_negeri_sembilan
        MalaysianState.MELAKA -> R.string.state_melaka
        MalaysianState.JOHOR -> R.string.state_johor
        MalaysianState.PAHANG -> R.string.state_pahang
        MalaysianState.TERENGGANU -> R.string.state_terengganu
        MalaysianState.KELANTAN -> R.string.state_kelantan
        MalaysianState.SARAWAK -> R.string.state_sarawak
        MalaysianState.LABUAN -> R.string.state_labuan
        MalaysianState.SABAH -> R.string.state_sabah
    }
