package com.pluto.core.database

import android.content.Context
import androidx.room.Room
import com.pluto.core.database.dao.FavoriteDao
import com.pluto.core.database.dao.FollowedSeriesDao
import com.pluto.core.database.dao.HistoryDao
import com.pluto.core.database.dao.NewEpisodeNotificationDao
import com.pluto.core.database.dao.PlaybackProgressDao
import com.pluto.core.database.dao.RecentSearchDao
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
    fun providePlutoDatabase(@ApplicationContext context: Context): PlutoDatabase {
        return Room.databaseBuilder(
            context,
            PlutoDatabase::class.java,
            PlutoDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideFavoriteDao(db: PlutoDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideHistoryDao(db: PlutoDatabase): HistoryDao = db.historyDao()
    @Provides fun provideProgressDao(db: PlutoDatabase): PlaybackProgressDao = db.progressDao()
    @Provides fun provideFollowedSeriesDao(db: PlutoDatabase): FollowedSeriesDao = db.followedSeriesDao()
    @Provides fun provideNewEpisodeDao(db: PlutoDatabase): NewEpisodeNotificationDao = db.newEpisodeDao()
    @Provides fun provideRecentSearchDao(db: PlutoDatabase): RecentSearchDao = db.recentSearchDao()
}
