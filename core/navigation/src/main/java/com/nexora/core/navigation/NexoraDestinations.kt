package com.nexora.core.navigation

sealed interface NexoraDestination {
    val route: String

    data object Dashboard : NexoraDestination {
        override val route: String = "dashboard"
    }

    data object FileManager : NexoraDestination {
        override val route: String = "file_manager"
    }

    data object Editor : NexoraDestination {
        override val route: String = "editor"
    }
}
