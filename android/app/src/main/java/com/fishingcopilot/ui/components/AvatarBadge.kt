package com.fishingcopilot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fishingcopilot.data.profile.Avatar
import com.fishingcopilot.ui.theme.OceanMidnight

@Composable
fun AvatarBadge(
    avatar: Avatar,
    size: Dp,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(OceanMidnight)
            .then(
                if (selected) Modifier.border(3.dp, avatar.accent, CircleShape)
                else Modifier.border(1.dp, avatar.accent.copy(alpha = 0.35f), CircleShape)
            )
    ) {
        Image(
            painter = painterResource(avatar.icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}
