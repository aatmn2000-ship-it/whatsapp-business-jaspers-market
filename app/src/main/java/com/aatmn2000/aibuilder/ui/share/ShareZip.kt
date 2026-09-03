package com.aatmn2000.aibuilder.ui.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aatmn2000.aibuilder.data.ProjectRepository

/**
 * Shares a generated ZIP through the Android share sheet. The file is
 * served via FileProvider (cache path declared in res/xml/file_paths.xml)
 * — no storage permissions involved.
 */
object ShareZip {

    fun share(
        context: Context,
        repository: ProjectRepository,
        projectName: String,
        zipBytes: ByteArray
    ) {
        val file = repository.writeZipFile("$projectName.zip", zipBytes)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$projectName.zip")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Share $projectName.zip")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
