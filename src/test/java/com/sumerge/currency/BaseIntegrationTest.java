package com.sumerge.currency;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class BaseIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @BeforeAll
    void setUp() {
        // Setup logic before all tests
    }

    @AfterAll
    void tearDown() {
        // Cleanup logic after all tests
    }

    void givenUserWhenExceedingRateLimitThenReturn429() {
    }

    void givenUnderlyingApiTimeoutThenReturnFromCache() {
    }

    void givenUnderlyingApiTimeoutAndNoCachePresentThenReturnError() {
    }

    void givenUnderlyingApiReturnsMalformedJsonThenReturnError() {
    }

    void givenValidBaseWhenExportingRatesThenReturnTextFile() {
    }

    void givenValidBaseWhenCalculatingInsightsThenReturnCorrectCalculation() {
    }

}
