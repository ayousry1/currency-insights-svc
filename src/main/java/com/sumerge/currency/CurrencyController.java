package com.sumerge.currency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CurrencyController {
    private static final List<String> SUPPORTED = Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD");

    @Autowired
    private ExchangeRateApiClient exchangeRateApiClient;
    @Autowired
    private FrankfurterApiClient frankfurterApiClient;

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
            Map<String, Object> resp = exchangeRateApiClient.getRates(base);
            Map<String, Double> rates = (Map<String, Double>) resp.get("rates");
            return rates;
        } catch (Exception e) {
            try {
                Map<String, Object> resp = frankfurterApiClient.getRates(base);
                Map<String, Double> rates = (Map<String, Double>) resp.get("rates");
                return rates;
            } catch (Exception ex) {
                return new HashMap<>();
            }
        }
    }

    @GetMapping("/api/rates/export/{base}")
    public ResponseEntity<byte[]> exportRates(@PathVariable String base) {
        Map<String, Double> rates = fetchRates(base);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : rates.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        byte[] fileContent = sb.toString().getBytes();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + base + "_rates.txt")
                .header("Content-Type", "text/plain")
                .body(fileContent);
    }
}
