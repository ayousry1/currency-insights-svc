package com.sumerge.currency;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//todo: add export example
//todo: remove resttemplate and add feign client
//todo: hardcode the export format

@RestController
public class CurrencyController {
    private static final List<String> SUPPORTED = Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD");
    private static final String EXCHANGE_API = "https://api.exchangerate-api.com/v4/latest/";
    private static final String FRANKFURTER_API = "https://api.frankfurter.app/latest?from=";
    private RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/api/rates/supported")
    public ResponseEntity<?> supported() {
        return ResponseEntity.ok(SUPPORTED);
    }

    @GetMapping("/api/rates/latest/{base}")
    public ResponseEntity<?> latest(@PathVariable String base) {
        Map<String, Double> rates = fetchRates(base);
        return ResponseEntity.ok(rates);
    }

    private Map<String, Double> fetchRates(String base) {
        try {
            String url = EXCHANGE_API + base;
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            Map<String, Double> rates = (Map<String, Double>) resp.get("rates");
            return rates;
        } catch (Exception e) {
            try {
                String url = FRANKFURTER_API + base;
                Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
                Map<String, Double> rates = (Map<String, Double>) resp.get("rates");
                return rates;
            } catch (Exception ex) {
                return new HashMap<>();
            }
        }
    }
}
