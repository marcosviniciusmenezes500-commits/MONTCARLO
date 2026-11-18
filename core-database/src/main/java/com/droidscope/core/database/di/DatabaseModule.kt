package com.droidscope.core.database.di

import android.content.Context
import androidx.room.Room
import com.droidscope.core.database.DroidScopeDatabase
import com.droidscope.core.database.dao.TelemetryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DroidScopeDatabase {
        return Room.databaseBuilder(
            context,
            DroidScopeDatabase::class.java,
            "droidscope.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTelemetryDao(database: DroidScopeDatabase): TelemetryDao {
        return database.telemetryDao()
    }
}
