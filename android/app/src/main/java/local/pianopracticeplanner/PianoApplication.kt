package local.pianopracticeplanner

import android.app.Application
import androidx.room.Room
import local.pianopracticeplanner.data.*

class PianoApplication: Application() {
    val database by lazy { Room.databaseBuilder(this,PracticeDatabase::class.java,"practice.db").addMigrations(PracticeDatabase.MIGRATION_1_2).build() }
    val repository by lazy { PracticeRepository(database) }
}
