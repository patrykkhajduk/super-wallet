package io.hyde.wallet.infrastructure.adapters.input.web.rest

import io.hyde.wallet.BaseIntegrationTest
import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.command.BlockFundsCommand
import io.hyde.wallet.domain.model.command.DepositFundsCommand
import io.hyde.wallet.infrastructure.adapters.input.web.rest.request.CreateWalletRequest
import io.hyde.wallet.infrastructure.adapters.input.web.rest.response.WalletDto
import io.hyde.wallet.infrastructure.adapters.input.web.rest.response.WalletDto.FundDto
import org.hamcrest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec

class WalletControllerTest extends BaseIntegrationTest {

    private static final String BTC = "BTC"

    @Autowired
    @Value('${wallets.limit-per-owner}')
    private Integer walletsLimitPerOwner

    def "should return wallets page"() {
        given:
        Wallet wallet1 = testHelper.initWallet()
        testHelper.executeAndStoreSendCommands(wallet1,
                new DepositFundsCommand(UUID.randomUUID().toString(), wallet1.getId(), BTC, 33.33),
                new BlockFundsCommand(UUID.randomUUID().toString(), wallet1.getId(), BTC, 22.22))

        Wallet wallet2 = testHelper.initWallet()
        testHelper.executeAndStoreSendCommands(wallet2,
                new DepositFundsCommand(UUID.randomUUID().toString(), wallet2.getId(), BTC, 55.55),
                new BlockFundsCommand(UUID.randomUUID().toString(), wallet1.getId(), BTC, 33.33))

        and:
        WalletDto expectedWallet1Dto = new WalletDto(
                wallet1.getId(),
                wallet1.getOwnerId(),
                [new FundDto(BTC, 11.11, 22.22)])
        WalletDto expectedWallet2Dto = new WalletDto(
                wallet2.getId(),
                wallet2.getOwnerId(),
                [new FundDto(BTC, 22.22, 33.33)])

        expect:
        performGetWallets(0, 2)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content').value(Matchers.hasSize(2))
                .jsonPath('$.content[0]').isEqualTo(expectedWallet1Dto)
                .jsonPath('$.content[1]').isEqualTo(expectedWallet2Dto)
                .jsonPath('$.totalElements').isEqualTo(2)

        and:
        performGetWallets(1, 2)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath('$.content').value(Matchers.hasSize(0))
                .jsonPath('$.totalElements').isEqualTo(2)
    }

    def "should return wallet by id when found"() {
        given:
        Wallet wallet = testHelper.initWallet()
        wallet = testHelper.executeAndStoreSendCommands(wallet,
                new DepositFundsCommand(UUID.randomUUID().toString(), wallet.getId(), BTC, 33.33),
                new BlockFundsCommand(UUID.randomUUID().toString(), wallet.getId(), BTC, 11.11))

        and:
        WalletDto expectedWalletDto = new WalletDto(
                wallet.getId(),
                wallet.getOwnerId(),
                [new FundDto(BTC, 22.22, 11.11)])

        expect:
        performGetWalletById(wallet.getId())
                .expectStatus().isOk()
                .expectBody(WalletDto)
                .isEqualTo(expectedWalletDto)
    }

    def "should return 404 when wallet not found by id"() {
        expect:
        performGetWalletById("unknownWalletId")
                .expectStatus().isNotFound()
    }

    def "should create wallet when limit is not exceeded"() {
        given:
        String ownerId = UUID.randomUUID().toString()
        CreateWalletRequest request = new CreateWalletRequest(ownerId)

        expect:
        performCreateWallet(request)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.id').isNotEmpty()
                .jsonPath('$.ownerId').isEqualTo(ownerId)
                .jsonPath('$.balance').value(Matchers.hasSize(0))

        and:
        List<Wallet> wallets = testHelper.getAllWallets()
        wallets.size() == 1

        and:
        Wallet createdWallet = wallets[0]
        createdWallet.getId() != null
        createdWallet.getVersion() != null
        createdWallet.getOwnerId() == ownerId
        createdWallet.getFunds().isEmpty()
        createdWallet.getLastExecutedCommandResult().isEmpty()
        createdWallet.getCreatedDate() != null
        createdWallet.getLastModifiedDate() != null
    }

    def "should not create wallet when limit is exceeded"() {
        given:
        String ownerId = UUID.randomUUID().toString()

        and:
        (1..walletsLimitPerOwner).collect({ testHelper.initWallet(ownerId) })

        and:
        CreateWalletRequest request = new CreateWalletRequest(ownerId)

        expect:
        performCreateWallet(request)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.errors').value(Matchers.hasSize(1))
                .jsonPath('$.errors[0]').isEqualTo("Wallets limit reached for owner: " + ownerId)

        and:
        testHelper.getAllWalletsCount() == walletsLimitPerOwner
    }

    private ResponseSpec performGetWallets(int pageNumber, int pageSize) {
        webTestClient.get()
                .uri("${WalletController.BASE_URL}?pageNumber={pageNumber}&pageSize={pageSize}", pageNumber, pageSize)
                .exchange()
    }

    private ResponseSpec performGetWalletById(String walletId) {
        webTestClient.get()
                .uri("${WalletController.BASE_URL}/{walletId}", walletId)
                .exchange()
    }

    private ResponseSpec performCreateWallet(CreateWalletRequest request) {
        webTestClient.post()
                .uri(WalletController.BASE_URL)
                .bodyValue(request)
                .exchange()
    }
}
