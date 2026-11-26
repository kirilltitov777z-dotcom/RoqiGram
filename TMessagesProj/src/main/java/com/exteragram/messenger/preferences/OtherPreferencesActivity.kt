/*

 This is the source code of RoqiGram, a fork of exteraGram for Android.

 Licensed under the GNU GPL v. 3 or later, with the additional terms
 in ADDITIONAL_TERMS.md at the root of the repository.

*/

package com.exteragram.messenger.preferences

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.os.CountDownTimer
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.view.View
import android.widget.TextView
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.Locale
import com.exteragram.messenger.useGoogleAnalytics
import com.exteragram.messenger.useGoogleCrashlytics
import com.exteragram.messenger.backup.PreferencesUtils
import com.exteragram.messenger.export.ui.ExportActivity
import com.exteragram.messenger.roqi.RoqiProfileDecor
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.R
import org.telegram.messenger.Utilities
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.UItem
import org.telegram.ui.Components.UniversalAdapter

class OtherPreferencesActivity : BasePreferencesActivity() {

    enum class OtherItem {
        BANNER_ON,
        BANNER_FULL,
        BANNER_BADGE,
        BANNER_NFT,
        BANNER_PICK,
        BANNER_RESET,
        CRASHLYTICS,
        ANALYTICS,
        EXPORT_DATA,
        EXPORT_SETTINGS,
        RESET_SETTINGS,
        DELETE_ACCOUNT;

        val id: Int = ordinal + 1
    }

    companion object {
        private const val REQ_BANNER = 4702
        private const val MOD_ID_BASE = 100
        private val MOD_NAMES = arrayOf(
            "Цветной ник", "Бейдж у ника", "Прозрачный профиль", "Скрыть истории",
            "Скрыть сторис", "Скрыть публикации", "Скрыть подписчиков", "Скрыть номер телефона",
            "Скрыть юзернейм", "Скрыть дату рождения", "Скрыть статус", "Компактный профиль",
            "Плоские карточки", "Без скруглений", "Яркие иконки", "Монохромные иконки",
            "Приглушённый фон", "Режим модера", "Показывать ID", "Скрыть кнопки профиля"
        )
        private val MOD_KEYS = arrayOf(
            "colored_name", "name_badge", "transparent_profile", "hide_history",
            "hide_stories", "hide_posts", "hide_subscribers", "hide_phone",
            "hide_username", "hide_birthday", "hide_status", "compact_profile",
            "flat_cards", "no_rounding", "bright_icons", "mono_icons",
            "dim_background", "moder_mode", "show_id", "hide_profile_buttons"
        )
    }

    private var modMode = false

    override fun createView(context: Context): View {
        val view = super.createView(context)
        val root = view as? FrameLayout ?: return view
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xEE101018)
        }
        fun render() {
            bar.removeAllViews()
            for ((text, active) in arrayOf("Настройки" to !modMode, "Мод" to modMode)) {
                val button = Button(context).apply {
                    this.text = text
                    isAllCaps = false
                    textSize = 14f
                    setTextColor(if (active) 0xFF1A1206 else Color.WHITE)
                    setBackgroundColor(if (active) 0xFFC9A227 else Color.TRANSPARENT)
                    setPadding(8, 12, 8, 12)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        modMode = text == "Мод"
                        render()
                        listView.adapter?.update(true)
                    }
                }
                bar.addView(button)
            }
        }
        render()
        root.addView(bar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        listView.setPadding(listView.paddingLeft, listView.paddingTop,
            listView.paddingRight, AndroidUtilities.dp(64f))
        return view
    }

    override fun getTitle(): String = getString(R.string.LocalOther)

    override fun fillItems(items: ArrayList<UItem>, adapter: UniversalAdapter) {
        val ctx = context ?: return

        if (modMode) {
            items.add(UItem.asHeader("Мод — 20 функций"))
            MOD_NAMES.forEachIndexed { i, name ->
                items.add(uCheck(MOD_ID_BASE + i, name,
                    com.exteragram.messenger.roqi.RoqiMods.get(ctx, MOD_KEYS[i], true),
                    subtext = "Локально на устройстве"))
            }
            items.add(UItem.asShadow())
            return
        }

        items.add(UItem.asHeader("RoqiGram"))
        items.add(uCheck(OtherItem.BANNER_ON, "Баннер профиля", RoqiProfileDecor.bannerEnabled(ctx), subtext = "Фон в шапке и за списком", icon = R.drawable.msg_theme))
        items.add(uCheck(OtherItem.BANNER_FULL, "Баннер на весь профиль", RoqiProfileDecor.fullProfile(ctx), subtext = "Фон за всеми записями", icon = R.drawable.msg_media))
        items.add(uCheck(OtherItem.BANNER_BADGE, "Бейдж Roqi", RoqiProfileDecor.badgeEnabled(ctx), subtext = "Бесплатно", icon = R.drawable.msg_sticker))
        items.add(uCheck(OtherItem.BANNER_NFT, "NFT-карточка", RoqiProfileDecor.nftEnabled(ctx), subtext = "Локальный декор, не настоящий NFT", icon = R.drawable.msg_fave))
        items.add(uButton(OtherItem.BANNER_PICK, "Выбрать фото баннера", R.drawable.menu_edit_appearance, subtext = "Своя фотография из галереи"))
        items.add(uButton(OtherItem.BANNER_RESET, "Сбросить баннер", R.drawable.msg_reset))
        items.add(UItem.asShadow())

        items.add(UItem.asHeader("Google"))
        items.add(uCheck(OtherItem.CRASHLYTICS, "Crashlytics", useGoogleCrashlytics, icon = R.drawable.msg_report, linkAlias = "crashlytics"))
        items.add(uCheck(OtherItem.ANALYTICS, "Analytics", useGoogleAnalytics, icon = R.drawable.msg_data, linkAlias = "analytics"))
        items.add(UItem.asShadow(getString(R.string.AnalyticsInfo)))

        items.add(uButton(OtherItem.EXPORT_DATA, getString(R.string.ExportData), R.drawable.msg_archive, linkAlias = "exportData"))
        items.add(uButton(OtherItem.EXPORT_SETTINGS, getString(R.string.ExportSettings), R.drawable.msg_settings, linkAlias = "exportSettings"))
        items.add(uButton(OtherItem.RESET_SETTINGS, getString(R.string.ResetSettings), R.drawable.msg_reset, linkAlias = "resetSettings"))
        items.add(uButton(OtherItem.DELETE_ACCOUNT, getString(R.string.DeleteAccount), R.drawable.msg_clearcache, red = true, linkAlias = "deleteAccount"))
        items.add(UItem.asShadow())
    }

    private fun rebuild() {
        parentLayout.rebuildFragments(0)
        listView.adapter?.update(true)
    }

    override fun onClick(item: UItem, view: View, position: Int, x: Float, y: Float) {
        val ctx = context ?: return
        if (item.id in MOD_ID_BASE until MOD_ID_BASE + MOD_NAMES.size) {
            val i = item.id - MOD_ID_BASE
            toggleBooleanSettingAndRefresh(item) {
                com.exteragram.messenger.roqi.RoqiMods.set(ctx, MOD_KEYS[i], it)
            }
            rebuild()
            return
        }
        if (item.id <= 0 || item.id > OtherItem.entries.size) return
        when (OtherItem.entries[item.id - 1]) {
            OtherItem.BANNER_ON -> {
                toggleBooleanSettingAndRefresh(item) { RoqiProfileDecor.setBannerEnabled(ctx, it) }
                rebuild()
            }
            OtherItem.BANNER_FULL -> {
                toggleBooleanSettingAndRefresh(item) { RoqiProfileDecor.setFullProfile(ctx, it) }
                rebuild()
            }
            OtherItem.BANNER_BADGE -> {
                toggleBooleanSettingAndRefresh(item) { RoqiProfileDecor.setBadgeEnabled(ctx, it) }
                rebuild()
            }
            OtherItem.BANNER_NFT -> {
                toggleBooleanSettingAndRefresh(item) { RoqiProfileDecor.setNftEnabled(ctx, it) }
                rebuild()
            }
            OtherItem.BANNER_PICK -> pickBanner()
            OtherItem.BANNER_RESET -> {
                RoqiProfileDecor.clearBanner(ctx)
                android.widget.Toast.makeText(ctx, "Баннер сброшен", android.widget.Toast.LENGTH_SHORT).show()
                rebuild()
            }
            OtherItem.CRASHLYTICS -> {
                toggleBooleanSettingAndRefresh(item) { useGoogleCrashlytics = it }
                ApplicationLoader.getFirebaseCrashlytics()?.isCrashlyticsCollectionEnabled = useGoogleCrashlytics
            }
            OtherItem.ANALYTICS -> {
                toggleBooleanSettingAndRefresh(item) { useGoogleAnalytics = it }
                ApplicationLoader.getFirebaseAnalytics()?.let {
                    it.setAnalyticsCollectionEnabled(useGoogleAnalytics)
                    if (!useGoogleAnalytics) it.resetAnalyticsData()
                }
            }
            OtherItem.RESET_SETTINGS -> handleResetSettingsClick()
            OtherItem.DELETE_ACCOUNT -> handleDeleteAccountClick()
            OtherItem.EXPORT_SETTINGS -> PreferencesUtils.getInstance().exportSettings(this)
            OtherItem.EXPORT_DATA -> ExportActivity.open(this)
        }
    }

    private fun pickBanner() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQ_BANNER)
    }

    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResultFragment(requestCode, resultCode, data)
        if (requestCode != REQ_BANNER || resultCode != Activity.RESULT_OK) return
        val ctx = context ?: return
        val uri: Uri = data?.data ?: return
        try {
            val target = File(ctx.filesDir, "roqi_banner.jpg")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            RoqiProfileDecor.setBannerPath(ctx, target.absolutePath)
            RoqiProfileDecor.setBannerEnabled(ctx, true)
            android.widget.Toast.makeText(ctx, "Баннер сохранён", android.widget.Toast.LENGTH_SHORT).show()
            rebuild()
        } catch (t: Throwable) {
            android.widget.Toast.makeText(ctx, "Не удалось открыть изображение", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleResetSettingsClick() {
        val dialog = AlertDialog.Builder(parentActivity).apply {
            setMessage(AndroidUtilities.replaceTags(getString(R.string.ResetPreferencesInfo)))
            setTitle(getString(R.string.ResetSettings))
            setNegativeButton(getString(R.string.Cancel), null)
            setPositiveButton(getString(R.string.Reset)) { _, _ ->
                PreferencesUtils.clearPreferences()
                parentLayout.rebuildFragments(0)
                notificationCenter.postNotificationName(NotificationCenter.mainUserInfoChanged)
                notificationCenter.postNotificationName(NotificationCenter.dialogFiltersUpdated)
                LocaleController.getInstance().recreateFormatters()
                Theme.reloadAllResources(parentActivity)
                BulletinFactory.of(this@OtherPreferencesActivity)
                    .createErrorBulletin(getString(R.string.ResetPreferences), resourceProvider).show()
            }
        }.create()
        showDialog(dialog)
        (dialog.getButton(DialogInterface.BUTTON_POSITIVE) as? TextView)
            ?.setTextColor(Theme.getColor(Theme.key_text_RedBold))
    }

    private fun handleDeleteAccountClick() {
        val builder = AlertDialog.Builder(parentActivity).apply {
            setMessage(getString(R.string.TosDeclineDeleteAccount))
            setTitle(getString(R.string.DeleteAccount))
            setPositiveButton(getString(R.string.Deactivate)) { _, _ ->
                val progressDialog = AlertDialog(parentActivity, AlertDialog.ALERT_TYPE_SPINNER).apply {
                    setCanCancel(false)
                    show()
                }
                Utilities.globalQueue.postRunnable({
                    connectionsManager.sendRequest(TL_account.deleteAccount().apply {
                        reason = "RoqiGram"
                    }) { response, error -> AndroidUtilities.runOnUIThread {
                        try {
                            progressDialog.dismiss()
                        } catch (e: Exception) {
                            FileLog.e(e)
                        }
                        if (response is TLRPC.TL_boolTrue) {
                            messagesController.performLogout(0)
                            return@runOnUIThread
                        }
                        if (error == null || error.code != -1000) {
                            val errorText = getString(R.string.ErrorOccurred) + error?.text?.let { "\n$it" }.orEmpty()
                            AlertDialog.Builder(parentActivity).apply {
                                setTitle(getString(R.string.AppName))
                                setMessage(errorText)
                                setPositiveButton(getString(R.string.OK), null)
                                show()
                            }
                        }
                    }}
                }, 500)
            }
            setNegativeButton(getString(R.string.Cancel), null)
        }
        builder.create().apply {
            setOnShowListener {
                val button = this.getButton(AlertDialog.BUTTON_POSITIVE) as? TextView ?: return@setOnShowListener
                button.setTextColor(Theme.getColor(Theme.key_text_RedBold))
                button.isEnabled = false
                val buttonText = button.text
                object : CountDownTimer(30000, 100) {
                    override fun onTick(millisUntilFinished: Long) {
                        button.text = String.format(Locale.getDefault(), "%s • %d", buttonText, millisUntilFinished / 1000 + 1)
                    }
                    override fun onFinish() {
                        button.text = buttonText
                        button.isEnabled = true
                    }
                }.start()
            }
        }.also { showDialog(it) }
    }
}
