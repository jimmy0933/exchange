package com.fubon.fxmonitor.data.remote

import com.fubon.fxmonitor.domain.model.ExchangeRate

interface ExchangeRateProvider {
    suspend fun fetchRates(): Result<List<ExchangeRate>>
}
