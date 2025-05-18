package com.aks_labs.tulsi.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val Settings.Debugging: SettingsDebuggingImpl
    get() = SettingsDebuggingImpl(context, viewModelScope)

val Settings.Permissions: SettingsPermissionsImpl
    get() = SettingsPermissionsImpl(context, viewModelScope)

val Settings.TrashBin: SettingsTrashBinImpl
    get() = SettingsTrashBinImpl(context, viewModelScope)

val Settings.AlbumsList: SettingsAlbumsListImpl
    get() = SettingsAlbumsListImpl(context, viewModelScope)

val Settings.Versions: SettingsVersionImpl
    get() = SettingsVersionImpl(context, viewModelScope)

val Settings.User: SettingsUserImpl
    get() = SettingsUserImpl(context, viewModelScope)

val Settings.Storage: SettingsStorageImpl
    get() = SettingsStorageImpl(context, viewModelScope)

val Settings.Video: SettingsVideoImpl
    get() = SettingsVideoImpl(context, viewModelScope)

val Settings.LookAndFeel: SettingsLookAndFeelImpl
    get() = SettingsLookAndFeelImpl(context, viewModelScope)

val Settings.Editing: SettingsEditingImpl
    get() = SettingsEditingImpl(context, viewModelScope)

val Settings.MainGalleryView: SettingMainGalleryViewImpl
	get() = SettingMainGalleryViewImpl(context, viewModelScope)

val Settings.DefaultTabs: SettingsDefaultTabsImpl
    get() = SettingsDefaultTabsImpl(context, viewModelScope)

val Settings.PhotoGrid: SettingsPhotoGridImpl
    get() = SettingsPhotoGridImpl(context, viewModelScope)

