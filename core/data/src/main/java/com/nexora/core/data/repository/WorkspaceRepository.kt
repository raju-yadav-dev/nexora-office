package com.nexora.core.data.repository

import com.nexora.core.model.DocumentType
import com.nexora.core.model.WorkspaceFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface WorkspaceRepository {
    /** Emits workspace files from local cache and future cloud merges. */
    fun observeFiles(): Flow<List<WorkspaceFile>>
    /** Inserts or updates a file entry in local storage. */
    suspend fun upsertFile(file: WorkspaceFile)
}

class InMemoryWorkspaceRepository : WorkspaceRepository {
    private val files = MutableStateFlow(
        listOf(
            WorkspaceFile(name = "Welcome.docx", path = "/local/Welcome.docx", type = DocumentType.DOC),
            WorkspaceFile(name = "Sprint Board.xlsx", path = "/local/Sprint Board.xlsx", type = DocumentType.SHEET)
        )
    )

    override fun observeFiles(): Flow<List<WorkspaceFile>> = files.asStateFlow()

    override suspend fun upsertFile(file: WorkspaceFile) {
        files.update { existing ->
            val withoutCurrent = existing.filterNot { it.id == file.id }
            withoutCurrent + file
        }
    }
}
