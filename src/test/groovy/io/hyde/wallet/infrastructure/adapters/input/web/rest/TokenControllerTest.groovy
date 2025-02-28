package io.hyde.wallet.infrastructure.adapters.input.web.rest

import io.hyde.wallet.BaseIntegrationTest
import io.hyde.wallet.domain.model.Token
import io.hyde.wallet.infrastructure.adapters.input.web.rest.request.CreateTokenRequest
import io.hyde.wallet.infrastructure.adapters.input.web.rest.response.TokenDto
import org.hamcrest.Matchers
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import reactor.test.StepVerifier

class TokenControllerTest extends BaseIntegrationTest {

    def "should return all tokens"() {
        given:
        Token token1 = testHelper.initToken("token1")
        Token token2 = testHelper.initToken("token2")
        Token token3 = testHelper.initToken("token3")
        expect:
        webTestClient.get()
                .uri(TokenController.BASE_URL)
                .exchange()
                .expectStatus().isOk()
                .returnResult(TokenDto.class)
                .getResponseBody()
                .as(tokensFlux -> StepVerifier.create(tokensFlux))
                .expectNext(new TokenDto(token1.getName()))
                .expectNext(new TokenDto(token2.getName()))
                .expectNext(new TokenDto(token3.getName()))
                .expectComplete()
                .verify()
    }

    def "should create token when request is valid"() {
        given:
        CreateTokenRequest request = new CreateTokenRequest("BTC")

        expect:
        performCreateToken(request)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath('$.name').isEqualTo("BTC")

        and:
        List<Token> tokens = testHelper.getAllTokens()
        tokens.size() == 1

        and:
        Token createdToken = tokens[0]
        createdToken.getId() != null
        createdToken.getName() == "BTC"
        createdToken.getCreatedDate() != null
        createdToken.getLastModifiedDate() != null
    }

    def "should not create token when already exists for name"() {
        given:
        testHelper.initToken("BTC")

        and:
        CreateTokenRequest request = new CreateTokenRequest("BTC")

        expect:
        performCreateToken(request)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath('$.errors').value(Matchers.hasSize(1))
                .jsonPath('$.errors[0]').isEqualTo("Token with name %s already exists".formatted(request.name()))

        and:
        testHelper.getAllTokensCount() == 1
    }

    private ResponseSpec performCreateToken(CreateTokenRequest request) {
        webTestClient.post()
                .uri(TokenController.BASE_URL)
                .bodyValue(request)
                .exchange()
    }
}
