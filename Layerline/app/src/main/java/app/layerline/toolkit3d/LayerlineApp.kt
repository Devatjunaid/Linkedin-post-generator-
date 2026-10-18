package app.layerline.toolkit3d

import android.app.Application
import app.layerline.toolkit3d.ads.AdsManager
import app.layerline.toolkit3d.data.AppDatabase
import app.layerline.toolkit3d.data.Repository
import app.layerline.toolkit3d.data.SettingsStore

class LayerlineApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: Repository by lazy { Repository(database, SettingsStore(this)) }
    val ads: AdsManager by lazy { AdsManager(this) }
}
