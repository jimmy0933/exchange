package com.fubon.fxmonitor

import com.fubon.fxmonitor.domain.model.BankSide
import com.fubon.fxmonitor.domain.model.CurrencyCode
import com.fubon.fxmonitor.domain.model.ExchangeRate
import com.fubon.fxmonitor.domain.model.RateType
import com.fubon.fxmonitor.domain.usecase.ConversionDirection
import com.fubon.fxmonitor.domain.usecase.ExchangeCalculatorUseCase
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FubonFXMonitorUnitTest {

    @Test
    fun testExchangeCalculatorBigDecimalPrecision() {
        val calculator = ExchangeCalculatorUseCase()

        val rate = ExchangeRate(
            currency = CurrencyCode.JPY,
            spotBuy = BigDecimal("0.2002"),
            spotSell = BigDecimal("0.2038"),
            cashBuy = BigDecimal("0.1975"),
            cashSell = BigDecimal("0.2065"),
            officialUpdateTime = "2026/10/13 09:23:31"
        )

        val result = calculator.convert(
            amount = BigDecimal("10000"),
            rate = rate,
            direction = ConversionDirection.TWD_TO_FOREIGN,
            rateType = RateType.CASH
        )

        assertNotNull(result)
        assertEquals(CurrencyCode.JPY.name, result!!.currencyCode)
        assertEquals(RateType.CASH, result.rateType)
        assertEquals(BankSide.SELL, result.bankSide)
        assertEquals(BigDecimal("0.2065"), result.rateUsed)
        assertEquals(BigDecimal("48426.1501"), result.outputAmount)
    }

    @Test
    fun testCurrencyCodeEnumMapping() {
        assertEquals(CurrencyCode.USD, CurrencyCode.fromString("USD"))
        assertEquals(CurrencyCode.JPY, CurrencyCode.fromString("jpy"))
        assertEquals(CurrencyCode.EUR, CurrencyCode.fromString("歐元"))
        assertNull(CurrencyCode.fromString("UNKNOWN"))
    }
}
