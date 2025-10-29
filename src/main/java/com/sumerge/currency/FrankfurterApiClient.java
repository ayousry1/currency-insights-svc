package com.sumerge.currency;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;

@FeignClient(name = "frankfurterApi", url = "https://api.frankfurter.app/latest")
public interface FrankfurterApiClient {
    @GetMapping
    Map<String, Object> getRates(@RequestParam("from") String base);
}

