package local.pianopracticeplanner

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import local.pianopracticeplanner.data.PracticeDatabase
import local.pianopracticeplanner.data.PracticeRepository
import local.pianopracticeplanner.data.PracticeRecord
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProgressMigrationTest {
    @Test fun versionOneRecordsDraftAndTombstonesSurviveMigration()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="migration-${UUID.randomUUID()}.db"
        val schema=InstrumentationRegistry.getInstrumentation().context.assets.open("local.pianopracticeplanner.data.PracticeDatabase/1.json")
            .bufferedReader().use{JSONObject(it.readText()).getJSONObject("database")}
        val id=UUID.randomUUID().toString()
        val deleted=UUID.randomUUID().toString()
        val legacy=SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name),null)
        try {
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val entity=entities.getJSONObject(i)
                legacy.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}",entity.getString("tableName")))
                val indices=entity.optJSONArray("indices")
                if(indices!=null)for(j in 0 until indices.length())legacy.execSQL(indices.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}",entity.getString("tableName")))
            }
            val setup=schema.getJSONArray("setupQueries")
            for(i in 0 until setup.length())legacy.execSQL(setup.getString(i))
            legacy.execSQL("INSERT INTO records VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",arrayOf<Any>(id,"2026-09-21","移行前",30,"8小節","メモ","次",2,"2026-09-21T00:00:00Z","難所","未練習","完成像"))
            legacy.execSQL("INSERT INTO operations VALUES(?,?,?)",arrayOf<Any>("legacy-operation","legacy-payload",id))
            legacy.execSQL("INSERT INTO drafts VALUES(?,?)",arrayOf<Any>("practice","途中の下書き"))
            legacy.execSQL("INSERT INTO deleted_records VALUES(?)",arrayOf<Any>(deleted))
            legacy.version=1
        }finally{legacy.close()}
        val db=Room.databaseBuilder(context,PracticeDatabase::class.java,name).addMigrations(PracticeDatabase.MIGRATION_1_2).build()
        try {
            val repo=PracticeRepository(db)
            assertEquals(PracticeRecord(id,"2026-09-21","移行前",30,"8小節","メモ","次",2,"2026-09-21T00:00:00Z","難所","未練習","完成像"),repo.records().single())
            assertEquals("legacy-payload",db.practice().operation("legacy-operation")?.payload)
            assertEquals("途中の下書き",repo.readDraft())
            assertEquals(1,db.practice().wasDeleted(deleted))
            assertTrue(db.practice().allAttempts().isEmpty())
        }finally{db.close();context.deleteDatabase(name)}
    }
}
