package io.hyde.wallet.utils

import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.Wallet.Fund

class TestUtils {

    static void verifyFund(Wallet wallet,
                           String token,
                           BigDecimal expectedAmount,
                           Map<String, BigDecimal> expectedBlocked = [:]) {
        Fund fund = wallet.getFunds()[token]
        assert fund.getAvailable() == expectedAmount
        assert fund.getBlocked().size() == expectedBlocked.size()
        expectedBlocked.entrySet().each {
            assert fund.getBlocked().get(it.key) == it.value
        }
    }

}
