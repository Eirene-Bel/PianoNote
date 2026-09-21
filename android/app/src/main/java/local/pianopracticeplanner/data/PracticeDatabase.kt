package local.pianopracticeplanner.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Entity(tableName="records",indices=[Index(value=["date"])])
data class PracticeRecord(
    @PrimaryKey val id: String,
    val date: String, val piece: String, val minutes: Int,
    @ColumnInfo(name="range") val practiceRange: String="",
    val memo: String="", val next: String="",val revision: Long=1,
    @ColumnInfo(name="created_at") val createdAt: String=Instant.now().toString(),
    @ColumnInfo(name="difficult_parts") val difficultParts: String="",
    @ColumnInfo(name="unpracticed_parts") val unpracticedParts: String="",
    @ColumnInfo(name="finishing_image") val finishingImage: String=""
)
@Entity(tableName="operations") data class SaveOperation(@PrimaryKey val id: String,val payload: String,val recordId: String)
@Entity(tableName="deleted_records") data class DeletedRecord(@PrimaryKey val id: String)
@Entity(tableName="drafts") data class DraftRow(@PrimaryKey val id: String="practice",val payload: String)
@Entity(tableName="note_attempts",indices=[Index(value=["date"])])
data class NoteAttempt(@PrimaryKey val id:String,val date:String,val answeredAt:Long,val mode:String,val clefOption:String,val countOption:Int,val range:String,val actualClef:String,val notes:String,val correct:Boolean,val assisted:Boolean)
@Entity(tableName="note_sessions",indices=[Index(value=["date"])])
data class NoteSession(@PrimaryKey val id:String,val date:String,val seconds:Int)
@Entity(tableName="progress_settings") data class ProgressSettingsRow(@PrimaryKey val id:String="progress",val payload:String)

@Dao interface PracticeDao {
    @Query("SELECT * FROM records ORDER BY date DESC, created_at DESC, id DESC") fun observe(): Flow<List<PracticeRecord>>
    @Query("SELECT * FROM records ORDER BY date DESC, created_at DESC, id DESC") suspend fun all(): List<PracticeRecord>
    @Query("SELECT * FROM records WHERE id=:id") suspend fun find(id: String): PracticeRecord?
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insert(record: PracticeRecord)
    @Update suspend fun update(record: PracticeRecord)
    @Query("DELETE FROM records WHERE id=:id") suspend fun delete(id: String)
    @Query("SELECT * FROM operations WHERE id=:id") suspend fun operation(id: String): SaveOperation?
    @Insert suspend fun addOperation(operation: SaveOperation)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun tombstone(record: DeletedRecord)
    @Query("SELECT COUNT(*) FROM deleted_records WHERE id=:id") suspend fun wasDeleted(id: String): Int
    @Query("SELECT id FROM deleted_records ORDER BY id") suspend fun deletedIds(): List<String>
    @Query("SELECT * FROM drafts WHERE id='practice'") suspend fun draft(): DraftRow?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putDraft(row: DraftRow)
    @Query("DELETE FROM drafts WHERE id='practice'") suspend fun clearDraft()
    @Query("SELECT * FROM note_attempts ORDER BY answeredAt DESC, id DESC") fun observeAttempts():Flow<List<NoteAttempt>>
    @Query("SELECT * FROM note_attempts ORDER BY answeredAt DESC, id DESC") suspend fun allAttempts():List<NoteAttempt>
    @Query("SELECT * FROM note_attempts WHERE id=:id") suspend fun attempt(id:String):NoteAttempt?
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun addAttempt(row:NoteAttempt):Long
    @Query("SELECT * FROM note_sessions ORDER BY date DESC, id DESC") fun observeSessions():Flow<List<NoteSession>>
    @Query("SELECT * FROM note_sessions ORDER BY date DESC, id DESC") suspend fun allSessions():List<NoteSession>
    @Query("SELECT * FROM note_sessions WHERE id=:id") suspend fun session(id:String):NoteSession?
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun addSession(row:NoteSession):Long
    @Query("SELECT * FROM progress_settings WHERE id='progress'") suspend fun progressSettings():ProgressSettingsRow?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putProgressSettings(row:ProgressSettingsRow)
}

@Database(entities=[PracticeRecord::class,SaveOperation::class,DeletedRecord::class,DraftRow::class,NoteAttempt::class,NoteSession::class,ProgressSettingsRow::class],version=2,exportSchema=true)
abstract class PracticeDatabase: RoomDatabase() {
    abstract fun practice(): PracticeDao
    companion object {
        val MIGRATION_1_2=object:Migration(1,2){
            override fun migrate(db:SupportSQLiteDatabase){
                db.execSQL("CREATE TABLE IF NOT EXISTS `note_attempts` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `answeredAt` INTEGER NOT NULL, `mode` TEXT NOT NULL, `clefOption` TEXT NOT NULL, `countOption` INTEGER NOT NULL, `range` TEXT NOT NULL, `actualClef` TEXT NOT NULL, `notes` TEXT NOT NULL, `correct` INTEGER NOT NULL, `assisted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_attempts_date` ON `note_attempts` (`date`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `note_sessions` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `seconds` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_sessions_date` ON `note_sessions` (`date`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `progress_settings` (`id` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}
