package com.maumela.magnummanagement.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/** A lightning bolt for Energy (the core icon set has none). */
private val BoltIcon: ImageVector = ImageVector.Builder(
    name = "Bolt",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(fill = SolidColor(Color.Black)) {
    moveTo(13f, 2f)
    lineTo(5f, 13f)
    lineTo(11f, 13f)
    lineTo(10f, 22f)
    lineTo(19f, 10f)
    lineTo(13f, 10f)
    close()
}.build()

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomItem(Routes.MARKETPLACE_BASE, "Market", Icons.Filled.Search),
    BottomItem(Routes.ENERGY, "Energy", BoltIcon),
    BottomItem(Routes.ORDERS, "Orders", Icons.Filled.ShoppingCart),
    BottomItem(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

/** The five top-level destinations. The Orders item shows how many items are in the draft order. */
@Composable
fun MmmBottomBar(currentRoute: String?, draftCount: Int, onNavigate: (String) -> Unit) {
    val current = Routes.baseRoute(currentRoute)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomItems.forEach { item ->
            NavigationBarItem(
                selected = current == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.route == Routes.ORDERS && draftCount > 0) Badge { Text("$draftCount") }
                        },
                    ) {
                        Icon(item.icon, contentDescription = null)
                    }
                },
                label = { Text(item.label) },
            )
        }
    }
}

/** Switches between top-level tabs, keeping each tab's state and not piling up the back stack. */
fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
