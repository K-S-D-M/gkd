package li.gkd.app.ui

import androidx.compose.runtime.Composable
import li.gkd.app.ui.navigation.SelectorPickerRoute
import li.gkd.app.ui.platform.UiHost

@Composable
expect fun SelectorPickerPage(
    route: SelectorPickerRoute,
    host: UiHost,
)
