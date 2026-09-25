package com.goalkeeper.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.goalkeeper.core.model.GoalIcon
import com.goalkeeper.core.model.JournalEntryType

/** The app's icon set. Screens use these instead of reaching into Material icons directly. */
object GkIcons {
    val Today: ImageVector get() = Icons.Rounded.Today
    val Goals: ImageVector get() = Icons.Rounded.TrackChanges
    val Insights: ImageVector get() = Icons.Rounded.BarChart
    val Journal: ImageVector get() = Icons.Rounded.Book
    val Settings: ImageVector get() = Icons.Rounded.Tune
    val Add: ImageVector get() = Icons.Rounded.Add
    val Check: ImageVector get() = Icons.Rounded.Check
    val Close: ImageVector get() = Icons.Rounded.Close
    val Back: ImageVector get() = Icons.AutoMirrored.Rounded.ArrowBack
    val Send: ImageVector get() = Icons.AutoMirrored.Rounded.Send
    val Edit: ImageVector get() = Icons.Rounded.Edit
    val Delete: ImageVector get() = Icons.Rounded.Delete
    val More: ImageVector get() = Icons.Rounded.MoreVert
    val Search: ImageVector get() = Icons.Rounded.Search
    val Flame: ImageVector get() = Icons.Rounded.Whatshot
    val Trophy: ImageVector get() = Icons.Rounded.EmojiEvents
    val Flag: ImageVector get() = Icons.Rounded.Flag
    val Skip: ImageVector get() = Icons.Rounded.SkipNext
    val DragHandle: ImageVector get() = Icons.Rounded.DragHandle
    val Pin: ImageVector get() = Icons.Rounded.PushPin
    val Bell: ImageVector get() = Icons.Rounded.Notifications
    val BellActive: ImageVector get() = Icons.Rounded.NotificationsActive
    val Alarm: ImageVector get() = Icons.Rounded.Alarm
    val Clock: ImageVector get() = Icons.Rounded.Schedule
    val Calendar: ImageVector get() = Icons.Rounded.Event
    val Archive: ImageVector get() = Icons.Rounded.Archive
    val Unarchive: ImageVector get() = Icons.Rounded.Unarchive
    val ChevronLeft: ImageVector get() = Icons.Rounded.ChevronLeft
    val ChevronRight: ImageVector get() = Icons.Rounded.ChevronRight
    val ExpandMore: ImageVector get() = Icons.Rounded.ExpandMore
    val ExpandLess: ImageVector get() = Icons.Rounded.ExpandLess
    val Palette: ImageVector get() = Icons.Rounded.Palette
    val ThemeMode: ImageVector get() = Icons.Rounded.Brightness4
    val Backup: ImageVector get() = Icons.Rounded.Backup
    val Restore: ImageVector get() = Icons.Rounded.Restore
    val Info: ImageVector get() = Icons.Rounded.Info
    val Entry: ImageVector get() = Icons.Rounded.ChatBubbleOutline
    val Note: ImageVector get() = Icons.Rounded.Description
    val Checklist: ImageVector get() = Icons.Rounded.PlaylistAddCheck

    fun forEntryType(type: JournalEntryType): ImageVector = when (type) {
        JournalEntryType.ENTRY -> Entry
        JournalEntryType.NOTE -> Note
        JournalEntryType.CHECKLIST -> Checklist
    }

    fun forGoal(icon: GoalIcon): ImageVector = when (icon) {
        GoalIcon.TARGET -> Icons.Rounded.TrackChanges
        GoalIcon.RUN -> Icons.Rounded.DirectionsRun
        GoalIcon.FITNESS -> Icons.Rounded.FitnessCenter
        GoalIcon.BIKE -> Icons.Rounded.DirectionsBike
        GoalIcon.SWIM -> Icons.Rounded.Pool
        GoalIcon.HIKE -> Icons.Rounded.Terrain
        GoalIcon.BOOK -> Icons.Rounded.Book
        GoalIcon.STUDY -> Icons.Rounded.School
        GoalIcon.LANGUAGE -> Icons.Rounded.Translate
        GoalIcon.WRITE -> Icons.Rounded.Create
        GoalIcon.CODE -> Icons.Rounded.Code
        GoalIcon.WORK -> Icons.Rounded.Work
        GoalIcon.MONEY -> Icons.Rounded.AttachMoney
        GoalIcon.SAVINGS -> Icons.Rounded.AccountBalanceWallet
        GoalIcon.MEDITATE -> Icons.Rounded.Spa
        GoalIcon.SLEEP -> Icons.Rounded.Hotel
        GoalIcon.WATER -> Icons.Rounded.LocalDrink
        GoalIcon.FOOD -> Icons.Rounded.Restaurant
        GoalIcon.NO_SMOKING -> Icons.Rounded.SmokeFree
        GoalIcon.MUSIC -> Icons.Rounded.MusicNote
        GoalIcon.ART -> Icons.Rounded.Palette
        GoalIcon.CAMERA -> Icons.Rounded.PhotoCamera
        GoalIcon.TRAVEL -> Icons.Rounded.Flight
        GoalIcon.HOME -> Icons.Rounded.Home
        GoalIcon.PLANT -> Icons.Rounded.LocalFlorist
        GoalIcon.FAMILY -> Icons.Rounded.People
        GoalIcon.HEART -> Icons.Rounded.Favorite
        GoalIcon.PETS -> Icons.Rounded.Pets
        GoalIcon.STAR -> Icons.Rounded.Star
    }
}
