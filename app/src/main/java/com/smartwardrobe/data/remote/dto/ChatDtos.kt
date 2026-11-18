package com.smartwardrobe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** OpenAI 兼容的 chat completion 请求（DeepSeek v4 系列）。 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("response_format")
    val responseFormat: ResponseFormat? = null,
    val stream: Boolean = false,
    @SerialName("max_tokens")
    val maxTokens: Int? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    /** 可以是纯字符串，也可以是内容块数组（视觉输入）。 */
    val content: JsonElement
)

@Serializable
data class ResponseFormat(val type: String = "json_object")

@Serializable
data class ChatResponse(
    val choices: List<Choice>? = null,
    val error: ApiError? = null,
    val usage: Usage? = null
) {
    @Serializable
    data class Choice(val message: AssistantMessage)

    @Serializable
    data class AssistantMessage(val content: String? = null)

    @Serializable
    data class ApiError(val message: String? = null, val type: String? = null)

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int = 0,
        @SerialName("completion_tokens") val completionTokens: Int = 0
    )
}

/** 内容块：文本。 */
@Serializable
data class TextPart(val type: String = "text", val text: String)

/** 内容块：图片。 */
@Serializable
data class ImagePart(
    val type: String = "image_url",
    @SerialName("image_url") val imageUrl: ImageUrl
)

@Serializable
data class ImageUrl(
    val url: String,
    val detail: String = "low" // low=512×512，打标足够且省 token
)
