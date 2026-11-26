/*

 This is the source code of RoqiGram, a fork of exteraGram for Android.

 Licensed under the GNU GPL v. 3 or later, with the additional terms
 in ADDITIONAL_TERMS.md at the root of the repository.

*/

package com.exteragram.messenger.preferences

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import com.exteragram.messenger.plugins.PluginsController
import com.exteragram.messenger.plugins.ui.PluginsActivity
import com.exteragram.messenger.preferences.appearance.AppearancePreferencesActivity
import com.exteragram.messenger.preferences.chats.ChatsPreferencesActivity
import com.exteragram.messenger.preferences.components.HeaderSettingsCell
import com.exteragram.messenger.useSystemIconShape
import com.exteragram.messenger.utils.addIf
import com.exteragram.messenger.utils.system.VibratorUtils
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter
import java.util.ArrayList

class MainPreferencesActivity : BasePreferencesActivity() {

    enum class PreferenceItem {
        HEADER_CELL,
        GENERAL_CATEGORY,
        APPEARANCE_CATEGORY,
        CHATS_CATEGORY,
        PLUGINS_CATEGORY,
        OTHER_CATEGORY;

        val id: Int = ordinal + 1
    }

    private lateinit var headerSettingsCell: HeaderSettingsCell

    override fun getTitle(): String = getString(R.string.Preferences)
    override fun needHideTitle(): Boolean = true

    override fun createView(context: Context): View {
        headerSettingsCell = HeaderSettingsCell(context)
        fragmentView = super.createView(context)
        if (parentLayout?.isRightLayout == true) actionBar.setBackButtonImage(R.drawable.ic_ab_close)
        return fragmentView
    }

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        items.add(UItem.asCustomShadow(headerSettingsCell, 198).setId(PreferenceItem.HEADER_CELL.id))

        items.add(UItem.asHeader(getString(R.string.Categories)))
        items.add(uButton(PreferenceItem.GENERAL_CATEGORY, getString(R.string.General), R.drawable.msg_media, linkAlias = "general"))
        items.add(uButton(PreferenceItem.APPEARANCE_CATEGORY, getString(R.string.Appearance), R.drawable.msg_theme, linkAlias = "appearance"))
        items.add(uButton(PreferenceItem.CHATS_CATEGORY, getString(R.string.SearchAllChatsShort), R.drawable.msg_discussion, linkAlias = "chats"))
        items.addIf(PluginsController.isPluginEngineSupported(), uButton(PreferenceItem.PLUGINS_CATEGORY, getString(R.string.Plugins), R.drawable.msg_plugins, linkAlias = "plugins"))
        items.add(uButton(PreferenceItem.OTHER_CATEGORY, getString(R.string.LocalOther), R.drawable.msg_fave, linkAlias = "other"))
        items.add(UItem.asShadow())
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        if (item.id <= 0 || item.id > PreferenceItem.entries.size) return
        when (PreferenceItem.entries[item.id - 1]) {
            PreferenceItem.HEADER_CELL -> Unit
            PreferenceItem.GENERAL_CATEGORY -> presentFragment(GeneralPreferencesActivity())
            PreferenceItem.APPEARANCE_CATEGORY -> presentFragment(AppearancePreferencesActivity())
            PreferenceItem.CHATS_CATEGORY -> presentFragment(ChatsPreferencesActivity())
            PreferenceItem.PLUGINS_CATEGORY -> presentFragment(PluginsActivity())
            PreferenceItem.OTHER_CATEGORY -> presentFragment(OtherPreferencesActivity())
        }
    }

    override fun onLongClick(item: UItem, view: View, position: Int, x: Float, y: Float): Boolean {
        if (item.id <= 0 || item.id > PreferenceItem.entries.size) return false
        if (PreferenceItem.entries[item.id - 1] == PreferenceItem.HEADER_CELL) {
            useSystemIconShape = !useSystemIconShape
            view.performHapticFeedback(
                VibratorUtils.getType(HapticFeedbackConstants.KEYBOARD_TAP),
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
            view.invalidate()
            return true
        }
        return super.onLongClick(item, view, position, x, y)
    }

    override fun getListTopPadding(topInset: Int): Int =
        topInset + ActionBar.getCurrentActionBarHeight() - AndroidUtilities.dp(20f)
}
