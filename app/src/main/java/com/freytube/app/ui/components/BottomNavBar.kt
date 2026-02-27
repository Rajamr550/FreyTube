package com.freytube.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freytube.app.ui.navigation.Screen
import com.freytube.app.ui.theme.Primary

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            label = "Home",
            selectedIcon = { Icon(Icons.Filled.Home, contentDescription = "Home", modifier = Modifier.size(26.dp)) },
            unselectedIcon = { Icon(Icons.Outlined.Home, contentDescription = "Home", modifier = Modifier.size(26.dp)) }
        ),
        BottomNavItem(
            route = Screen.Search.route,
            label = "Search",
            selectedIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(26.dp)) },
            unselectedIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier.size(26.dp)) }
        ),
        BottomNavItem(
            route = Screen.Downloads.route,
            label = "Downloads",
            selectedIcon = { Icon(Icons.Filled.Download, contentDescription = "Downloads", modifier = Modifier.size(26.dp)) },
            unselectedIcon = { Icon(Icons.Outlined.Download, contentDescription = "Downloads", modifier = Modifier.size(26.dp)) }
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            label = "Settings",
            selectedIcon = { Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(26.dp)) },
            unselectedIcon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings", modifier = Modifier.size(26.dp)) }
        )
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (isSelected) item.selectedIcon() else item.unselectedIcon()
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    indicatorColor = Primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
