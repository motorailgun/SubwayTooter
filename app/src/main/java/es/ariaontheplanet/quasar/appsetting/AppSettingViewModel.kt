package es.ariaontheplanet.quasar.appsetting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Divider sentinel object used to mark dividers in the settings list.
 */
object Divider

class AppSettingViewModel(application: Application) : AndroidViewModel(application) {
    // State properties
    private val _currentSection = MutableStateFlow<AppSettingItem?>(null)
    val currentSection: StateFlow<AppSettingItem?> = _currentSection.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    private val _colorPickerItem = MutableStateFlow<AppSettingItem?>(null)
    val colorPickerItem: StateFlow<AppSettingItem?> = _colorPickerItem.asStateFlow()

    // Mutation methods
    fun setSection(item: AppSettingItem?) {
        _currentSection.value = item
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshUi() {
        _revision.value = _revision.value + 1
    }

    fun openColorPicker(item: AppSettingItem) {
        _colorPickerItem.value = item
    }

    fun closeColorPicker() {
        _colorPickerItem.value = null
    }

    /**
     * Build a filtered list of settings items based on the current section and search query.
     * 
     * @param section The current section, or null to show all root items
     * @param query Optional search query to filter items
     * @return A list containing AppSettingItem objects and Divider sentinels
     */
    fun buildItemsList(
        section: AppSettingItem?,
        query: String?,
    ): List<Any> = buildList {
        val context = getApplication<Application>()
        var lastPath: String? = null

        fun addParentPath(item: AppSettingItem) {
            add(Divider)
            val pathList = ArrayList<String>()
            var parent = item.parent
            while (parent != null) {
                if (parent.caption != 0) pathList.add(0, context.getString(parent.caption))
                parent = parent.parent
            }
            val path = pathList.joinToString("/")
            if (path != lastPath) {
                lastPath = path
                add(path)
                add(Divider)
            }
        }

        fun queryRecursive(item: AppSettingItem, q: String) {
            if (item.caption == 0) return
            when (item.type) {
                SettingType.Section ->
                    item.items.forEach { queryRecursive(it, q) }

                SettingType.Group -> {
                    if (item.match(context, q) ||
                        item.items.any { it.match(context, q) }
                    ) {
                        addParentPath(item)
                        add(item)
                        addAll(item.items)
                    }
                }

                else -> {
                    if (item.match(context, q)) {
                        addParentPath(item)
                        add(item)
                    }
                    item.items.forEach { queryRecursive(it, q) }
                }
            }
        }

        fun addSectionItems(sec: AppSettingItem?) {
            sec ?: return
            for (item in sec.items) {
                add(Divider)
                add(item)
                if (item.items.isNotEmpty()) {
                    when (item.type) {
                        SettingType.Group -> addAll(item.items)
                        else -> addSectionItems(item)
                    }
                }
            }
        }

        when {
            query?.isNotEmpty() == true -> queryRecursive(appSettingRoot, query)
            section != null -> addSectionItems(section)
            else -> {
                for (child in appSettingRoot.items) {
                    add(Divider)
                    add(child)
                }
            }
        }
        if (isNotEmpty()) add(Divider)
    }
}
