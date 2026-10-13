package com.fubon.fxmonitor.di

import android.content.Context
import com.fubon.fxmonitor.data.local.AppDatabase
import com.fubon.fxmonitor.data.local.RateDao
import com.fubon.fxmonitor.data.local.AlertDao
import com.fubon.fxmonitor.data.local.ErrorLogDao
import com.fubon.fxmonitor.data.remote.ExchangeRateProvider
import com.fubon.fxmonitor.data.remote.FubonExchangeRateScraper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideExchangeRateProvider(
        scraper: FubonExchangeRateScraper
    ): ExchangeRateProvider = scraper

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    fun provideRateDao(db: AppDatabase): RateDao = db.rateDao()

    @Provides
    fun provideAlertDao(db: AppDatabase): AlertDao = db.alertDao()

    @Provides
    fun provideErrorLogDao(db: AppDatabase): ErrorLogDao = db.errorLogDao()
}
