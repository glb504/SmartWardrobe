package com.smartwardrobe.data.repository

import com.smartwardrobe.data.local.preferences.tryOnDataStore
import androidx.datastore.preferences.core.edit
import com.smartwardrobe.domain.model.LayerTransform
import com.smartwardrobe.domain.model.TryOnLayer
import com.smartwardrobe.domain.model.TryOnSession
import com.smartwardrobe.domain.model.TryOnSlots
import com.smartwardrobe.domain.repository.TryOnRepository
import com.smartwardrobe.domain.repository.WardrobeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 试衣间实现。
 * 会话状态用 DataStore（以 JSON 字符串持久化），保证杀进程后仍可恢复人像与搭配。
 */
@Singleton
class TryOnRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wardrobeRepository: WardrobeRepository,
    private val json: Json
) : TryOnRepository {

    private val dataStore = context.tryOnDataStore
    private var cachedSession: TryOnSession? = null

    override suspend fun savePortrait(portraitPath: String): String {
        val current = getSession()
        val session = current?.copy(portraitPath = portraitPath) ?: TryOnSession(portraitPath = portraitPath)
        persist(session)
        return portraitPath
    }

    override suspend fun getSession(): TryOnSession? {
        cachedSession?.let { return it }
        val raw = dataStore.data.first()[KEY_SESSION]
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<TryOnSessionDto>(raw).toDomain() }.getOrNull()
            .also { cachedSession = it }
    }

    override suspend fun buildSessionFromItems(itemIds: List<String>, title: String): TryOnSession {
        val portrait = getSession()?.portraitPath.orEmpty()
        val items = wardrobeRepository.getClothesByIds(itemIds)
        val layers = items.map { item ->
            val slot = TryOnSlots.fromCategory(item.category)
            TryOnLayer(
                slot = slot,
                clothingItemId = item.id,
                imagePath = item.imagePath,
                name = item.name,
                transform = TryOnSlots.defaultTransform(slot)
            )
        }
        val session = TryOnSession(portraitPath = portrait, layers = layers, outfitTitle = title)
        persist(session)
        return session
    }

    override suspend fun replaceSlot(slot: String, itemId: String?): TryOnSession {
        val current = getSession() ?: TryOnSession(portraitPath = "")
        val layers = current.layers.filterNot { it.slot == slot }.toMutableList()
        if (itemId != null) {
            val item = wardrobeRepository.getClothing(itemId)
            if (item != null) {
                layers.add(
                    TryOnLayer(
                        slot = slot,
                        clothingItemId = item.id,
                        imagePath = item.imagePath,
                        name = item.name,
                        transform = TryOnSlots.defaultTransform(slot)
                    )
                )
            }
        }
        val session = current.copy(layers = layers, outfitTitle = "自由搭配")
        persist(session)
        return session
    }

    override suspend fun updateTransform(slot: String, transform: LayerTransform): TryOnSession {
        val current = getSession() ?: TryOnSession(portraitPath = "")
        val session = current.copy(
            layers = current.layers.map { if (it.slot == slot) it.copy(transform = transform) else it }
        )
        persist(session)
        return session
    }

    override suspend fun clearSession() {
        dataStore.edit { it.remove(KEY_SESSION) }
        cachedSession = null
    }

    private suspend fun persist(session: TryOnSession) {
        cachedSession = session
        val dto = TryOnSessionDto.fromDomain(session)
        dataStore.edit { it[KEY_SESSION] = json.encodeToString(dto) }
    }

    // ---- 持久化 DTO ----
    @Serializable
    data class TryOnSessionDto(
        val portraitPath: String,
        val outfitTitle: String,
        val layers: List<TryOnLayerDto>
    ) {
        companion object {
            fun fromDomain(s: TryOnSession) = TryOnSessionDto(
                portraitPath = s.portraitPath,
                outfitTitle = s.outfitTitle,
                layers = s.layers.map { TryOnLayerDto.fromDomain(it) }
            )
        }
        fun toDomain() = TryOnSession(
            portraitPath = portraitPath,
            outfitTitle = outfitTitle,
            layers = layers.map { it.toDomain() }
        )
    }

    @Serializable
    data class TryOnLayerDto(
        val slot: String,
        val clothingItemId: String,
        val imagePath: String,
        val name: String,
        val transform: TransformDto
    ) {
        companion object {
            fun fromDomain(l: TryOnLayer) = TryOnLayerDto(
                slot = l.slot,
                clothingItemId = l.clothingItemId,
                imagePath = l.imagePath,
                name = l.name,
                transform = TransformDto.fromDomain(l.transform)
            )
        }
        fun toDomain() = TryOnLayer(
            slot = slot,
            clothingItemId = clothingItemId,
            imagePath = imagePath,
            name = name,
            transform = transform.toDomain()
        )
    }

    @Serializable
    data class TransformDto(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float,
        val rotation: Float,
        val alpha: Float
    ) {
        companion object {
            fun fromDomain(t: LayerTransform) = TransformDto(t.scale, t.offsetX, t.offsetY, t.rotation, t.alpha)
        }
        fun toDomain() = LayerTransform(scale, offsetX, offsetY, rotation, alpha)
    }

    companion object {
        private val KEY_SESSION = androidx.datastore.preferences.core.stringPreferencesKey("tryon_session")
    }
}
