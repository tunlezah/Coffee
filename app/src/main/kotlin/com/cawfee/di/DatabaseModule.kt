package com.cawfee.di

import android.content.Context
import androidx.room.Room
import com.cawfee.data.local.CawfeeDatabase
import com.cawfee.data.local.ShotDao
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
    fun provideDatabase(@ApplicationContext context: Context): CawfeeDatabase =
        Room.databaseBuilder(context, CawfeeDatabase::class.java, "cawfee.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideShotDao(db: CawfeeDatabase): ShotDao = db.shotDao()
}
