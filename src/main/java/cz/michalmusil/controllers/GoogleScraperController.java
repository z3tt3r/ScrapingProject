package cz.michalmusil.controllers;


import cz.michalmusil.models.SearchResponseModel;
import cz.michalmusil.models.SearchResultModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import cz.michalmusil.services.GoogleScraperService;

import java.util.List;

/**
 * REST Controller pro Google scraping API
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Pro vývoj - v produkci specifikovat konkrétní domény
public class GoogleScraperController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleScraperController.class);

    @Autowired
    private GoogleScraperService scraperService;

    /**
     * Endpoint pro scraping Google výsledků
     * @param keyword klíčové slovní spojení
     * @return JSON response s výsledky
     */
    @GetMapping("/scrape")
    public ResponseEntity<SearchResponseModel> scrapeGoogleResults(@RequestParam String keyword) {
        logger.info("Přijat požadavek na scraping pro klíčové slovo: {}", keyword);

        // Validace vstupu
        if (keyword == null || keyword.trim().isEmpty()) {
            SearchResponseModel errorResponse = new SearchResponseModel(
                    keyword,
                    null,
                    false,
                    "Klíčové slovo nesmí být prázdné"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // Provedení scrapingu
            List<SearchResultModel> results = scraperService.scrapeGoogleResults(keyword.trim());

            SearchResponseModel response = new SearchResponseModel(
                    keyword.trim(),
                    results,
                    true,
                    String.format("Úspěšně nalezeno %d výsledků", results.size())
            );

            logger.info("Scraping dokončen pro klíčové slovo: {}, nalezeno {} výsledků",
                    keyword, results.size());

            return ResponseEntity.ok(response);

        } catch (InterruptedException e) {
            logger.error("Scraping byl přerušen: {}", e.getMessage());
            Thread.currentThread().interrupt();

            SearchResponseModel errorResponse = new SearchResponseModel(
                    keyword,
                    null,
                    false,
                    "Scraping byl přerušen"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } catch (Exception e) {
            logger.error("Chyba při scrapingu pro klíčové slovo {}: {}", keyword, e.getMessage(), e);

            SearchResponseModel errorResponse = new SearchResponseModel(
                    keyword,
                    null,
                    false,
                    "Chyba při získávání výsledků: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     * @return status aplikace
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Google Scraper API je funkční");
    }
}