package com.drdisagree.conversa

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.drdisagree.conversa.data.ChatState
import com.drdisagree.conversa.event.ChatUiEvent
import com.drdisagree.conversa.ui.theme.ConversaTheme
import com.drdisagree.conversa.viewmodel.ChatViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {

    private val uriState = MutableStateFlow("")
    private val imagePicker = registerForActivityResult<PickVisualMediaRequest, Uri>(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            uriState.update { uri.toString() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = bottom)
            insets
        }

        setContent {
            ChatScreen()
        }
    }

    @Composable
    fun ChatScreen() {
        val chatViewModel = viewModel<ChatViewModel>()
        val chatState = chatViewModel.chatState.collectAsState().value

        ConversaTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(
                    topBar = {
                        Header(chatState)
                    }
                ) {
                    InnerContent(
                        paddingValues = it,
                        chatState = chatState,
                        chatViewModel = chatViewModel
                    )
                }
            }
        }
    }

    @Composable
    private fun Header(chatState: ChatState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                modifier = Modifier
                    .padding(top = 26.dp),
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF05619B), Color(0xFF00E496))
                    ),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp
                )
            )

            if (chatState.isLoading) {
                AnimateDottedText(
                    text = "Typing",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    cycleDuration = 1500
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            )
        }
    }

    @Composable
    fun InnerContent(
        paddingValues: PaddingValues = PaddingValues(0.dp),
        chatState: ChatState,
        chatViewModel: ChatViewModel
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            verticalArrangement = Arrangement.Bottom
        ) {
            EmptyChatView(chatState, chatViewModel)

            val lazyListState = rememberLazyListState()
            val screenWidthDp =
                LocalConfiguration.current.screenWidthDp - LocalDensity.current.run { 24.dp.toPx() }
            val maxWidth: Dp = (0.9f * screenWidthDp).dp

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 16.dp,
                    bottom = 8.dp
                ),
                reverseLayout = true
            ) {
                itemsIndexed(chatState.chatList) { _, chat ->
                    ChatItem(
                        text = chat.prompt,
                        bitmap = chat.bitmap,
                        isFromUser = chat.isFromUser,
                        maxWidth = maxWidth
                    )
                }
            }

            BottomChatBar(
                chatState = chatState,
                chatViewModel = chatViewModel
            )
        }
    }

    @Composable
    fun EmptyChatView(
        chatState: ChatState,
        chatViewModel: ChatViewModel
    ) {
        if (chatState.chatList.isEmpty()) {
            val rememberedItems = remember { mutableListOf<String>() }
            if (rememberedItems.isEmpty()) {
                rememberedItems.addAll(preChatStringList.shuffled().take(4))
            }

            val scrollState = rememberLazyListState()

            LazyRow(
                state = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(rememberedItems) { _, text ->
                    PreChatItem(
                        text = text,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                chatViewModel.onEvent(ChatUiEvent.UpdatePrompt(text))
                            }
                    )
                }
            }
        }
    }

    @Composable
    fun BottomChatBar(
        chatState: ChatState,
        chatViewModel: ChatViewModel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    bottom = 16.dp,
                    start = 24.dp,
                    end = 24.dp
                )
                .clip(RoundedCornerShape(34.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val bitmap = getSelectedBitmap()

            if (bitmap != null) {
                Image(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .align(Alignment.Bottom)
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest
                                    .Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    .build()
                            )
                        },
                    contentScale = ContentScale.Crop,
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Picked image"
                )
            } else {
                Icon(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest
                                    .Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    .build()
                            )
                        }
                        .padding(13.dp)
                        .align(Alignment.Bottom),
                    imageVector = Icons.Outlined.AddAPhoto,
                    contentDescription = "Pick an image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(48.dp)
                    .padding(vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    .align(Alignment.Bottom)
            )

            val focusRequester = remember {
                FocusRequester()
            }

            BasicTextField(modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 6.dp,
                    vertical = 8.dp
                )
                .focusRequester(focusRequester),
                value = chatState.prompt,
                onValueChange = {
                    chatViewModel.onEvent(ChatUiEvent.UpdatePrompt(it))
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            innerTextField()
                            if (chatState.prompt.isEmpty()) Text(
                                "Type something...",
                                style = LocalTextStyle.current.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Icon(
                modifier = Modifier
                    .size(48.dp)
                    .rotate(45f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        chatViewModel.onEvent(
                            ChatUiEvent.SendPrompt(
                                prompt = chatState.prompt,
                                bitmap = bitmap
                            )
                        )
                        uriState.update { "" }
                    }
                    .padding(
                        start = 11.dp,
                        end = 13.dp,
                        top = 13.dp,
                        bottom = 11.dp
                    )
                    .align(Alignment.Bottom),
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = "Send prompt",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }

    @Composable
    fun ChatItem(
        modifier: Modifier = Modifier,
        text: String,
        bitmap: Bitmap? = null,
        isFromUser: Boolean,
        maxWidth: Dp
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(),
            horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
        ) {
            bitmap?.let {
                Image(
                    modifier = Modifier
                        .width(maxWidth)
                        .padding(top = 8.dp)
                        .aspectRatio(1f)
                        .clip(
                            RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp
                            )
                        ),
                    contentScale = ContentScale.Crop,
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Image"
                )
            }

            MarkdownText(
                modifier = Modifier
                    .widthIn(max = maxWidth, min = if (bitmap == null) 0.dp else maxWidth)
                    .padding(
                        top = if (bitmap == null) 8.dp else 0.dp,
                        bottom = 8.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = if (bitmap == null) 12.dp else 0.dp,
                            topEnd = if (bitmap == null) 12.dp else 0.dp,
                            bottomStart = if (isFromUser) 12.dp else 2.dp,
                            bottomEnd = if (isFromUser) 2.dp else 12.dp
                        )
                    )
                    .background(if (isFromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
                markdown = text,
                isTextSelectable = true,
                style = TextStyle(
                    color = if (isFromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 10.sp,
                    textAlign = TextAlign.Start,
                ),
                linkColor = if (isFromUser) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary
            )
        }
    }

    @Composable
    fun AnimateDottedText(
        modifier: Modifier = Modifier,
        text: String = "Loading...",
        style: TextStyle = LocalTextStyle.current,
        cycleDuration: Int = 1000
    ) {
        val transition = rememberInfiniteTransition(label = text)

        val visibleDotsCount = transition.animateValue(
            initialValue = 0,
            targetValue = 4,
            typeConverter = Int.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = cycleDuration,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = text
        )

        Text(
            text = text + ".".repeat(visibleDotsCount.value),
            modifier = modifier,
            style = style
        )
    }

    @Composable
    fun PreChatItem(
        modifier: Modifier = Modifier,
        text: String
    ) {
        Box(
            modifier = modifier
                .width(160.dp)
                .heightIn(min = 160.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp)
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
            )
        }
    }

    @Composable
    private fun getSelectedBitmap(): Bitmap? {
        val uri = uriState.collectAsState().value

        val imageState: AsyncImagePainter.State = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .size(Size.ORIGINAL)
                .build()
        ).state

        return if (imageState is AsyncImagePainter.State.Success) {
            imageState.result.drawable.toBitmap()
        } else {
            null
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun Preview() {
        ConversaTheme {
            ChatScreen()
        }
    }

    private val preChatStringList = listOf(
        "I'm feeling curious! Share an interesting fact with me.",
        "In the mood for a chuckle! How about sharing a joke?",
        "Looking to learn something new! Teach me a fascinating tidbit.",
        "Feeling adventurous! Let's play a quick game or solve a puzzle.",
        "Seeking inspiration! Recommend a captivating book or movie.",
        "Navigating a decision! Could you offer me some advice or insight?",
        "Up for a mental challenge! Pose me a thought-provoking question.",
        "Ready for an escape! Transport me with an engaging story.",
        "Craving a mental workout! Hit me with a brain teaser or riddle.",
        "Eager to explore! Let's dive into a discussion about a fascinating topic.",
        "Feeling philosophical! Share a meaningful quote or proverb.",
        "In the mood for a debate! Let's spar over a controversial issue.",
        "Curious about the future! Let's talk about emerging technology or trends.",
        "In the mood for trivia! Challenge me with a mind-bending question.",
        "Hungry for inspiration! Share a mouthwatering recipe or cooking tip.",
        "Intrigued by the subconscious! Tell me about a vivid dream you've had.",
        "Seeking enlightenment! Let's dissect a recent news headline or event.",
        "Feeling poetic! Share a beautiful piece of literature or poetry.",
        "Ready for a laugh! Share a funny anecdote or humorous story.",
        "In the mood for some culture! Let's discuss art, music, or cultural phenomena."
    )
}