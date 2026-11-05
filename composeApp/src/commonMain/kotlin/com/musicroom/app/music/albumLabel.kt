package com.musicroom.app.music

import androidx.compose.runtime.Composable
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.album_type_album
import com.musicroom.app.generated.resources.album_type_compilation
import com.musicroom.app.generated.resources.album_type_single
import com.musicroom.shared.dto.AlbumResponse
import com.musicroom.shared.dto.AlbumType
import org.jetbrains.compose.resources.stringResource

/** "Album · 2001", "Single · 2013": the kind of album and its year when known. */
@Composable
fun albumLabel(album: AlbumResponse): String {
    val kind = stringResource(
        when (album.type) {
            AlbumType.ALBUM -> Res.string.album_type_album
            AlbumType.SINGLE -> Res.string.album_type_single
            AlbumType.COMPILATION -> Res.string.album_type_compilation
        },
    )
    return listOfNotNull(kind, album.releaseYear).joinToString(" · ")
}
