package com.nexora.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.nexora.core.designsystem.theme.NexoraTheme
import com.nexora.core.data.session.DocumentSessionRepository
import com.nexora.core.data.storage.FileOpenManager
import com.nexora.core.data.storage.RecentFilesRepository
import com.nexora.core.data.storage.StorageAccessRepository
import com.nexora.core.model.DocumentAccessMode
import com.nexora.core.model.WorkspaceFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var fileOpenManager: FileOpenManager
    @Inject lateinit var recentFilesRepository: RecentFilesRepository
    @Inject lateinit var documentSessionRepository: DocumentSessionRepository
    @Inject lateinit var storageAccessRepository: StorageAccessRepository

    private var externalOpenFile by mutableStateOf<WorkspaceFile?>(null)
    private var externalOpenError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleExternalIntent(intent)
        setContent {
            NexoraTheme {
                NexoraApp(
                    externalOpenFile = externalOpenFile,
                    externalOpenError = externalOpenError,
                    onExternalOpenConsumed = {
                        externalOpenFile = null
                        externalOpenError = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalIntent(intent)
    }

    private fun handleExternalIntent(intent: Intent?) {
        val incoming = intent ?: return
        val uris = incoming.extractDocumentUris()
        if (uris.isEmpty()) return

        lifecycleScope.launch {
            val openedFiles = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    runCatching {
                        persistIncomingGrantIfPossible(incoming, uri)
                        fileOpenManager.buildWorkspaceFile(uri)
                    }.onSuccess { file ->
                        recentFilesRepository.addRecent(file)
                        documentSessionRepository.openSession(file)
                    }.getOrNull()
                }
            }

            if (openedFiles.isNotEmpty()) {
                externalOpenFile = openedFiles.first()
                externalOpenError = null
            } else {
                externalOpenError = "Nexora Office could not open that file. Try saving it locally and opening it again."
            }
        }
    }

    private suspend fun persistIncomingGrantIfPossible(intent: Intent, uri: Uri) {
        if (uri.scheme != "content") return
        val hasPersistableGrant = intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
        if (!hasPersistableGrant) return
        storageAccessRepository.persistUriPermission(uri, DocumentAccessMode.READ)
    }

    @Suppress("DEPRECATION")
    private fun Intent.extractDocumentUris(): List<Uri> {
        val result = linkedSetOf<Uri>()
        data?.let(result::add)

        clipData?.let { clip ->
            repeat(clip.itemCount) { index ->
                clip.getItemAt(index).uri?.let(result::add)
            }
        }

        when (action) {
            Intent.ACTION_SEND -> {
                getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(result::add)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(result::addAll)
            }
        }

        return result.toList()
    }
}
