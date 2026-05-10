package com.nexora.core.navigation

sealed interface NexoraDestination {
    val route: String

    data object Dashboard : NexoraDestination {
        override val route: String = "dashboard"
    }

    data object FileManager : NexoraDestination {
        override val route: String = "files"
    }

    data object Tools : NexoraDestination {
        override val route: String = "tools"
    }

    data object Templates : NexoraDestination {
        override val route: String = "templates"
    }

    data object Profile : NexoraDestination {
        override val route: String = "profile"
    }

    data object Editor : NexoraDestination {
        override val route: String = "editor"
    }
}
