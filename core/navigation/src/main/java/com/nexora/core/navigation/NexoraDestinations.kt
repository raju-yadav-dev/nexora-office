package com.nexora.core.navigation

import android.net.Uri
import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile

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
        const val baseRoute = "editor"
        const val fileIdArg = "fileId"
        const val titleArg = "title"
        const val typeArg = "type"
        const val pathArg = "path"

        override val route: String =
            "$baseRoute?$fileIdArg={$fileIdArg}&$titleArg={$titleArg}&$typeArg={$typeArg}&$pathArg={$pathArg}"

        fun createRoute(file: WorkspaceFile): String = buildString {
            append(baseRoute)
            append("?")
            append(fileIdArg)
            append("=")
            append(Uri.encode(file.id))
            append("&")
            append(titleArg)
            append("=")
            append(Uri.encode(file.name))
            append("&")
            append(typeArg)
            append("=")
            append(file.type.name)
            append("&")
            append(pathArg)
            append("=")
            append(Uri.encode(file.path))
        }

        fun createRoute(
            title: String,
            type: DocumentType = DocumentType.DOC,
            path: String = "nexora://workspace/new"
        ): String = createRoute(
            WorkspaceFile(
                name = title,
                path = path,
                type = type,
                sizeLabel = "New Pro file"
            )
        )
    }
}
