package com.musicroom.app.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SendHorizontal
import com.musicroom.app.common.PersonAvatar
import com.musicroom.app.common.UiText
import com.musicroom.app.generated.resources.Res
import com.musicroom.app.generated.resources.chat_say_hi
import com.musicroom.app.generated.resources.chat_seen
import com.musicroom.app.generated.resources.chat_typing
import com.musicroom.app.generated.resources.common_back
import com.musicroom.app.generated.resources.common_loading
import com.musicroom.app.generated.resources.message_placeholder
import com.musicroom.app.generated.resources.message_send
import com.musicroom.app.theme.components.pressable
import com.musicroom.app.theme.tokens.AppShapes
import com.musicroom.app.theme.tokens.AppTheme
import com.musicroom.shared.MusicRoomApi
import com.musicroom.shared.dto.MessageResponse
import com.musicroom.shared.dto.PublicUserResponse
import com.musicroom.shared.dto.SocialEvent
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Chat with the friend [userId]: their name at the top (opens their profile), the messages with the newest at the
 * bottom, and a field to write. Messages arrive live; scrolling up loads older ones. The friend sees when the
 * user types, and "Seen" shows under the user's last message once they read it.
 */
@Composable
fun ChatScreen(
    api: MusicRoomApi,
    social: SocialController,
    meId: String,
    userId: String,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var friend by remember(userId) { mutableStateOf<PublicUserResponse?>(social.person(userId)) }
    // Oldest first.
    var messages by remember(userId) { mutableStateOf<List<MessageResponse>?>(null) }
    var hasMore by remember(userId) { mutableStateOf(false) }
    var loadingOlder by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<UiText?>(null) }
    var draft by rememberSaveable(userId) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var lastTypingSent by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()
    // The friend's app shows "typing…" for a few seconds: the user's app says it again while they type.
    val typingRepeatMs = 3_000L
    // Messages of the same sender closer than this form one group (one time label).
    val groupGapMs = 5 * 60_000L
    // Messages are listed from the bottom: index 0 is the newest.
    val newestFirst by remember { derivedStateOf { messages.orEmpty().asReversed() } }

    suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = UiText.of(e)
        }
    }

    fun add(message: MessageResponse) {
        val current = messages ?: return
        if (current.none { it.id == message.id }) messages = current + message
    }

    LaunchedEffect(userId) {
        if (friend == null) guarded { friend = api.userProfile(userId).user }
    }
    LaunchedEffect(userId) {
        guarded {
            val page = api.messages(userId)
            messages = page.items
            hasMore = page.hasMore
        }
    }
    // While the chat shows, its messages are read as they arrive.
    DisposableEffect(userId) {
        social.openChat = userId
        social.markChatRead(userId)
        onDispose {
            if (social.openChat == userId) social.openChat = null
            social.sendTyping(userId, false)
        }
    }
    LaunchedEffect(userId) {
        social.events.collect { event ->
            when (event) {
                is SocialEvent.Message -> {
                    val message = event.message
                    if (message.senderId == userId || message.recipientId == userId) {
                        add(message)
                        listState.animateScrollToItem(0)
                    }
                }
                is SocialEvent.Read -> if (event.userId == userId) {
                    messages = messages?.map { if (it.senderId == meId) it.copy(read = true) else it }
                }
                else -> Unit
            }
        }
    }
    // Older messages when the top of the list comes into view.
    val nearTop by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearTop, hasMore) {
        val current = messages
        if (!nearTop || !hasMore || loadingOlder || current.isNullOrEmpty()) return@LaunchedEffect
        loadingOlder = true
        guarded {
            val page = api.messages(userId, before = current.first().sentAt)
            messages = page.items + (messages ?: emptyList())
            hasMore = page.hasMore
        }
        loadingOlder = false
    }

    fun onDraftChange(text: String) {
        draft = text
        val now = Clock.System.now().toEpochMilliseconds()
        if (text.isBlank()) {
            social.sendTyping(userId, false)
            lastTypingSent = 0
        } else if (now - lastTypingSent > typingRepeatMs) {
            social.sendTyping(userId, true)
            lastTypingSent = now
        }
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        error = null
        scope.launch {
            guarded {
                add(api.sendMessage(userId, text))
                draft = ""
                lastTypingSent = 0
                listState.animateScrollToItem(0)
            }
            sending = false
        }
    }

    Box(
        Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = 760.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(AppTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Lucide.ArrowLeft, contentDescription = stringResource(Res.string.common_back), tint = colors.textPrimary)
                }
                val shown = friend
                if (shown != null) {
                    Row(
                        Modifier.weight(1f).pressable(onClick = { onOpenUser(userId) }),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PersonAvatar(api, shown.id, shown.displayName, shown.avatarColor, shown.avatarVersion, size = 36.dp)
                        Column {
                            Text(shown.displayName, style = AppTheme.typography.heading, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            AnimatedVisibility(social.typing[userId] == true, enter = fadeIn(), exit = fadeOut()) {
                                Text(stringResource(Res.string.chat_typing), style = AppTheme.typography.bodySmall, color = colors.accentText)
                            }
                        }
                    }
                }
            }

            val list = messages
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    list == null -> if (error == null) {
                        Text(
                            stringResource(Res.string.common_loading),
                            style = AppTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    list.isEmpty() -> Text(
                        stringResource(Res.string.chat_say_hi, friend?.displayName.orEmpty()),
                        style = AppTheme.typography.body,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(AppTheme.spacing.lg),
                    )
                    else -> {
                        val lastMineId = list.lastOrNull { it.senderId == meId }?.id
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            state = listState,
                            reverseLayout = true,
                            contentPadding = PaddingValues(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                        ) {
                            itemsIndexed(newestFirst, key = { _, it -> it.id }) { index, message ->
                                val mine = message.senderId == meId
                                // The last bubble of a group (same sender, a few minutes apart) shows the time.
                                val next = newestFirst.getOrNull(index - 1)
                                val endsGroup = next == null || next.senderId != message.senderId ||
                                    next.sentAt - message.sentAt > groupGapMs
                                val footer = when {
                                    mine && message.id == lastMineId && message.read -> stringResource(Res.string.chat_seen)
                                    endsGroup -> timeAgo(message.sentAt)
                                    else -> null
                                }
                                MessageBubble(message.body, mine, footer, Modifier.animateItem())
                            }
                        }
                    }
                }
            }

            error?.let {
                Text(
                    it.resolve(),
                    style = AppTheme.typography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.md),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(AppTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    draft,
                    ::onDraftChange,
                    Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.message_placeholder), style = AppTheme.typography.body) },
                    textStyle = AppTheme.typography.body,
                    shape = AppShapes.large,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accentBorder,
                        unfocusedBorderColor = colors.border,
                        cursorColor = colors.accentText,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedPlaceholderColor = colors.textSecondary,
                        unfocusedPlaceholderColor = colors.textSecondary,
                    ),
                )
                val canSend = draft.isNotBlank() && !sending && messages != null
                IconButton(
                    onClick = ::send,
                    enabled = canSend,
                    modifier = Modifier.size(48.dp).background(if (canSend) colors.accent else colors.surfaceSubtle, CircleShape),
                ) {
                    Icon(
                        Lucide.SendHorizontal,
                        contentDescription = stringResource(Res.string.message_send),
                        tint = if (canSend) colors.onAccent else colors.textDisabled,
                    )
                }
            }
        }
    }
}

