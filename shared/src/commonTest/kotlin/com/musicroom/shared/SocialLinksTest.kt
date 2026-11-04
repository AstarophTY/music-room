package com.musicroom.shared

import com.musicroom.shared.dto.SocialNetwork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SocialLinksTest {
    @Test
    fun acceptsHandlesAtSignsAndProfileLinks() {
        assertEquals("swann.music", SocialLinks.normalize(SocialNetwork.INSTAGRAM, "  @swann.music "))
        assertEquals("swann.music", SocialLinks.normalize(SocialNetwork.INSTAGRAM, "https://www.instagram.com/swann.music/?igsh=abc"))
        assertEquals("swann", SocialLinks.normalize(SocialNetwork.TIKTOK, "https://www.tiktok.com/@swann"))
        assertEquals("swann_42", SocialLinks.normalize(SocialNetwork.DISCORD, "Swann_42"))
    }

    @Test
    fun checksEachNetworkFormat() {
        assertNull(SocialLinks.handleError(SocialNetwork.X, "swann_42"))
        assertNotNull(SocialLinks.handleError(SocialNetwork.X, "way_too_long_for_x_handles"))
        assertNotNull(SocialLinks.handleError(SocialNetwork.INSTAGRAM, "has space"))
        assertNotNull(SocialLinks.handleError(SocialNetwork.TWITCH, "abc"))
    }

    @Test
    fun buildsProfileUrls() {
        assertEquals("https://www.twitch.tv/swann", SocialNetwork.TWITCH.urlFor("swann"))
        assertNull(SocialNetwork.DISCORD.urlFor("swann"))
    }
}
