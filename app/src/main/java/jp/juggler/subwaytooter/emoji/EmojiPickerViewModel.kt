package jp.juggler.subwaytooter.emoji

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.juggler.subwaytooter.App1
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.emoji.CustomEmoji
import jp.juggler.subwaytooter.emoji.UnicodeEmoji
import jp.juggler.subwaytooter.emoji.EmojiCategory
import jp.juggler.subwaytooter.emoji.EmojiMap
import jp.juggler.subwaytooter.pref.PrefS
import jp.juggler.subwaytooter.table.SavedAccount
import jp.juggler.util.data.JsonObject
import jp.juggler.util.data.JsonArray
import jp.juggler.util.data.decodeJsonArray
import jp.juggler.util.data.toJsonArray
import jp.juggler.util.log.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmojiPickerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val log = LogCategory("EmojiPickerViewModel")
        
        val skinTones = listOf(
            SkinTone(0x1f3fb), // light skin tone
            SkinTone(0x1f3fc), // medium-light skin tone
            SkinTone(0x1f3fd), // medium skin tone
            SkinTone(0x1f3fe), // medium-dark skin tone
            SkinTone(0x1f3ff), // dark skin tone
        )
    }

    private val _categories = MutableStateFlow<List<EmojiCategory>>(emptyList())
    val categories: StateFlow<List<EmojiCategory>> = _categories

    private val _selectedCategory = MutableStateFlow<EmojiCategory>(EmojiCategory.Recent)
    val selectedCategory: StateFlow<EmojiCategory> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedSkinTone = MutableStateFlow<SkinTone?>(null)
    val selectedSkinTone: StateFlow<SkinTone?> = _selectedSkinTone

    private val _items = MutableStateFlow<List<PickerItem>>(emptyList())
    val items: StateFlow<List<PickerItem>> = _items

    private var accessInfo: SavedAccount? = null
    private var customEmojiMap: Map<String, CustomEmoji>? = null
    
    // Cache for unfiltered lists
    private val categoryCache = mutableMapOf<EmojiCategory, List<PickerItem>>()

    fun initialize(accessInfo: SavedAccount?) {
        this.accessInfo = accessInfo
        viewModelScope.launch {
            loadCustomEmojis()
            updateCategories()
            loadCategory(EmojiCategory.Recent)
        }
    }

    private suspend fun loadCustomEmojis() {
        val ai = accessInfo ?: return
        customEmojiMap = withContext(Dispatchers.IO) {
             try {
                // Use getList instead of getMap
                val list = App1.custom_emoji_lister.getList(ai)
                list.associateBy { it.shortcode }
            } catch (ex: Throwable) {
                log.e(ex, "loadCustomEmojis failed")
                emptyMap()
            }
        }
    }

    private fun updateCategories() {
        val list = ArrayList<EmojiCategory>()
        list.add(EmojiCategory.Recent)
        if (accessInfo != null) {
            list.add(EmojiCategory.Custom)
        }
        list.addAll(EmojiCategory.values().filter { 
            it != EmojiCategory.Recent && it != EmojiCategory.Custom 
        })
        _categories.value = list
    }

    fun selectCategory(category: EmojiCategory) {
        _selectedCategory.value = category
        _searchQuery.value = "" // Clear search on category change
        loadCategory(category)
    }

    fun setQuery(query: String) {
        _searchQuery.value = query
        filterItems()
    }

    fun selectSkinTone(tone: SkinTone?) {
        if (_selectedSkinTone.value == tone) {
            _selectedSkinTone.value = null // Toggle off
        } else {
            _selectedSkinTone.value = tone
        }
        // Re-apply to current list if showing unicode category
        // Instead of reloading, we can just trigger a refresh or re-map
        val current = _selectedCategory.value
        if (current != EmojiCategory.Recent && current != EmojiCategory.Custom) {
            loadCategory(current)
        }
    }

    private fun loadCategory(category: EmojiCategory) {
        viewModelScope.launch(Dispatchers.Default) {
            val list = when (category) {
                EmojiCategory.Recent -> loadRecentItems()
                EmojiCategory.Custom -> loadCustomItems()
                else -> loadUnicodeItems(category)
            }
            categoryCache[category] = list
            filterItems()
        }
    }

    private fun loadRecentItems(): List<PickerItem> {
        val list = ArrayList<PickerItem>()
        try {
            val jsonArray = PrefS.spEmojiPickerRecent.value.decodeJsonArray()
            val jsonList = jsonArray.objectList()
            
            val currentHost = accessInfo?.apiHost?.ascii
            
            for (item in jsonList) {
                val name = item.optString("name")
                val instance = item.optString("instance")
                
                if (name.isEmpty()) continue
                
                if (instance.isEmpty() || instance == "null") {
                    // Unicode emoji
                    EmojiMap.shortNameMap[name]?.let {
                        list.add(PickerItemUnicode(it))
                    }
                } else if (instance == currentHost) {
                    // Custom emoji
                    customEmojiMap?.get(name)?.let {
                        list.add(PickerItemCustom(it))
                    }
                }
            }
        } catch (ex: Throwable) {
            log.e(ex, "loadRecentItems failed")
        }
        return list
    }

    private fun loadCustomItems(): List<PickerItem> {
        return customEmojiMap?.values?.map { PickerItemCustom(it) }?.sortedBy { it.customEmoji.shortcode } ?: emptyList()
    }
    
    // Extension to apply skin tone
    private fun UnicodeEmoji.applySkinTone(code: String): UnicodeEmoji? {
        return toneChildren.find { it.first == code }?.second
    }

    private fun loadUnicodeItems(category: EmojiCategory): List<PickerItem> {
        val tone = _selectedSkinTone.value
        return category.emojiList.map { base ->
            val modified = tone?.let { base.applySkinTone(it.code) }
            PickerItemUnicode(base, modified)
        }
    }

    private fun filterItems() {
        val query = _searchQuery.value.trim().lowercase()
        val category = _selectedCategory.value
        val source = categoryCache[category] ?: emptyList()
        
        if (query.isEmpty()) {
            _items.value = source
            return
        }

        _items.value = source.filter { item ->
            when (item) {
                is PickerItemCustom -> {
                    item.customEmoji.shortcode.contains(query) ||
                    item.customEmoji.aliases?.any { it.contains(query, ignoreCase = true) } == true
                }
                is PickerItemUnicode -> {
                    item.unicodeEmoji.namesLower.any { it.contains(query) }
                }
            }
        }
    }
    
    fun onPicked(item: PickerItem) {
        // Update Recent
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val name = when (item) {
                    is PickerItemCustom -> item.customEmoji.shortcode
                    is PickerItemUnicode -> item.unicodeEmoji.unifiedName
                }
                val instance = when (item) {
                    is PickerItemCustom -> accessInfo?.apiHost?.ascii
                    is PickerItemUnicode -> null
                }

                val list = try {
                    PrefS.spEmojiPickerRecent.value.decodeJsonArray().objectList().toMutableList()
                } catch (e: Throwable) {
                    ArrayList<JsonObject>()
                }

                // Remove existing entry for same item
                val it = list.iterator()
                var count = 0
                while (it.hasNext()) {
                    val entry = it.next()
                    val eName = entry.optString("name")
                    val eInstance = entry.optString("instance") // "null" or actual string
                    
                    val bSameInstance = if (instance == null) {
                         eInstance.isEmpty() || eInstance == "null"
                    } else {
                        eInstance == instance
                    }
                    
                    if (bSameInstance) {
                        if (eName == name) {
                            it.remove()
                        } else if (++count >= 256) {
                             it.remove()
                        }
                    }
                }

                // Add to top
                list.add(0, JsonObject().apply {
                    put("name", name)
                    if (instance != null) put("instance", instance)
                })
                
                // Save
                PrefS.spEmojiPickerRecent.value = list.toJsonArray().toString()
                
                // Reload recent if currently showing recent
                if (_selectedCategory.value == EmojiCategory.Recent) {
                    withContext(Dispatchers.Main) {
                        loadCategory(EmojiCategory.Recent)
                    }
                }

            } catch (ex: Throwable) {
                log.e(ex, "onPicked failed")
            }
        }
    }
}
