package com.smartwardrobe.data.remote

import com.smartwardrobe.data.remote.dto.ChatRequest
import com.smartwardrobe.data.remote.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * DeepSeek OpenAI-兼容 Chat Completions 接口。
 * 打标使用 deepseek-v4-flash-vision-exp（支持图片），穿搭生成用 deepseek-v4-flash。
 * baseUrl 在 NetworkModule 中配置为 https://api.deepseek.com/
 */
interface AiService {

    @POST("chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
