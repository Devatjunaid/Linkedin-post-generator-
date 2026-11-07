package com.superdevlabs.autosubtitles

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.superdevlabs.autosubtitles.data.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Users upgrading from v1.0 must keep every saved project. Builds a database exactly as
 * v1.0 created it, then opens it with the current Room setup (which runs MIGRATION_1_2 and
 * validates the schema — a mismatch would throw here instead of crashing on users' phones).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DatabaseMigrationTest {

    @Test
    fun v1_projects_survive_the_upgrade() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.getDatabasePath("subtitles.db").apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `projects` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `sourceUri` TEXT NOT NULL, " +
                    "`videoPath` TEXT, `thumbnailPath` TEXT, `durationMs` INTEGER NOT NULL, `width` INTEGER NOT NULL, " +
                    "`height` INTEGER NOT NULL, `languageId` TEXT NOT NULL, `status` TEXT NOT NULL, `captionsJson` TEXT NOT NULL, " +
                    "`styleJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'v1')")
            db.execSQL(
                "INSERT INTO projects VALUES('p1','Old clip','content://x',NULL,NULL,61000,1080,1920,'en-us','READY'," +
                    "'[{\"id\":1,\"startMs\":0,\"endMs\":900,\"words\":[{\"text\":\"hello\",\"startMs\":0,\"endMs\":400}]}]'," +
                    "'{\"presetId\":\"pop\"}',1,2)",
            )
            db.version = 1
        }

        val database = AppDatabase.create(context)
        try {
            val entity = runBlocking { database.projects().get("p1") }
            assertNotNull(entity)
            assertEquals("Old clip", entity!!.name)
            assertEquals("vosk", entity.engine)
            assertEquals("", entity.sourceLanguage)
            assertEquals("en-us", entity.languageId)
        } finally {
            database.close()
        }
    }
}
