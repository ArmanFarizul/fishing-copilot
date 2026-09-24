package com.fishingcopilot.data.profile

data class UserProfile(
    val nickname: String,
    val avatar: Avatar,
    val fishingStyle: FishingStyle,
    val targetSpecies: Set<Species>
)

enum class Avatar { JETTY, BOAT, KAYAK, ROD, FISH, ANCHOR }

enum class FishingStyle { SHORE, BOAT, ESTUARY, KAYAK }

enum class Species { SIAKAP, JENAHAK, KERAPU, TENGGIRI, PARI, TALANG, IKAN_MERAH, BELUKANG }
