package com.sumerge.currency;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "exchangeRateApi", url = "https://api.exchangerate-api.com/v4/latest")
public interface ExchangeRateApiClient {
    @GetMapping("/{base}")
    Map<String, Object> getRates(@PathVariable("base") String base);
}
