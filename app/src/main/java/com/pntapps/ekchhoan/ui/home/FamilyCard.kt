package com.pntapps.ekchhoan.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.data.FamilyMember
import com.pntapps.ekchhoan.ui.common.cardShadow
import com.pntapps.ekchhoan.ui.common.cardSurface
import com.pntapps.ekchhoan.ui.common.elderClickable
import com.pntapps.ekchhoan.ui.common.FitText
import com.pntapps.ekchhoan.ui.theme.LocalHomeIcons
import com.pntapps.ekchhoan.ui.theme.LocalPalette
import com.pntapps.ekchhoan.ui.theme.AvatarTints
import com.pntapps.ekchhoan.ui.theme.Border
import com.pntapps.ekchhoan.ui.theme.CallGreen
import com.pntapps.ekchhoan.ui.theme.Dimens
import com.pntapps.ekchhoan.ui.theme.ElderSize
import com.pntapps.ekchhoan.ui.theme.GalleryOrange
import com.pntapps.ekchhoan.ui.theme.Ink
import com.pntapps.ekchhoan.ui.theme.InkSoft
import com.pntapps.ekchhoan.ui.theme.Surface
import com.pntapps.ekchhoan.ui.theme.elderStyle
import androidx.compose.ui.platform.LocalContext
import com.pntapps.ekchhoan.container
import java.io.File

/**
 * One family member (§4.3): round photo or tinted initial, green call badge (warning badge when the number
 * is missing), and the name. Tap calls; long-press deliberately does nothing.
 */
@Composable
fun FamilyCard(
    member: FamilyMember,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.FamilyCardCorner + LocalPalette.current.card.cornerExtra)
    val description = if (member.hasValidPhone) {
        stringResource(R.string.home_call_cd, member.name)
    } else {
        stringResource(R.string.home_no_number_cd, member.name)
    }
    Column(
        modifier = modifier
            .heightIn(min = Dimens.FamilyCardHeight)
            .cardShadow(shape)
            .elderClickable(shape, onClick = onClick)
            .semantics { contentDescription = description }
            .cardSurface(shape, Surface, Border)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Box {
            Avatar(member, index)
            if (member.hasValidPhone) {
                Badge(Icons.Rounded.Phone, CallGreen, Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 4.dp))
            } else {
                Badge(Icons.Rounded.PriorityHigh, GalleryOrange, Modifier.align(Alignment.BottomEnd).offset(x = 8.dp, y = 4.dp))
            }
        }
        if (LocalHomeIcons.current.showLabels) {
            FitText(
                text = member.name,
                style = elderStyle(ElderSize.FamilyName, bold = true),
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/** Big dashed "add family members" card shown when the list is empty (§7); opens the lock → settings. */
@Composable
fun AddFamilyCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Dimens.FamilyCardCorner)
    val dashColor = InkSoft
    Column(
        modifier = modifier
            .heightIn(min = Dimens.FamilyCardHeight)
            .elderClickable(shape, onClick = onClick)
            .background(Surface, shape)
            .drawBehind {
                val stroke = 3.dp.toPx()
                val radius = Dimens.FamilyCardCorner.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 14f))),
                )
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        val (bg, fg) = AvatarTints[0]
        Box(Modifier.size(Dimens.Avatar * LocalHomeIcons.current.scale).background(bg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = fg, modifier = Modifier.size(40.dp))
        }
        Text(
            text = stringResource(R.string.home_add_family),
            style = elderStyle(ElderSize.FamilyName, bold = true),
            color = Ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Avatar(member: FamilyMember, index: Int) {
    val (bg, fg) = AvatarTints[index % AvatarTints.size]
    val scale = LocalHomeIcons.current.scale
    Box(
        modifier = Modifier.size(Dimens.Avatar * scale).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        // Initial sits underneath, so it still shows if the photo is missing or fails to load.
        Text(initialOf(member.name), style = elderStyle(32f * scale, bold = true), color = fg)
        val path = member.photoPath
        if (path != null) {
            val context = LocalPlatformContext.current
            val photos = LocalContext.current.container.photos
            val file = remember(path) { photos.resolve(path) ?: File(path) }
            // Key the memory cache on mtime so a replaced photo shows up immediately.
            val stamp = remember(path, member) { file.lastModified() }
            val request = remember(path, stamp) {
                ImageRequest.Builder(context).data(file).memoryCacheKey("$path@$stamp").build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Badge(icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    val scale = LocalHomeIcons.current.scale
    Box(
        modifier = modifier
            .size(Dimens.CallBadge * scale)
            .background(color, CircleShape)
            .border(2.dp, Surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Surface, modifier = Modifier.size(18.dp * scale))
    }
}

private fun initialOf(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    return String(Character.toChars(trimmed.codePointAt(0))).uppercase()
}
