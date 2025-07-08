import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.GoogleScraperController;
import models.SearchResultModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import services.GoogleScraperService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit testy pro GoogleScraperController
 */
@WebMvcTest(GoogleScraperController.class)
class GoogleScraperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleScraperService scraperService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test úspěšného API volání
     */
    @Test
    void testScrapeGoogleResults_Success() throws Exception {
        // Příprava mock dat
        List<SearchResultModel> mockResults = Arrays.asList(
                new SearchResultModel("Test Title 1", "https://example1.com", "Test Description 1"),
                new SearchResultModel("Test Title 2", "https://example2.com", "Test Description 2")
        );

        when(scraperService.scrapeGoogleResults("test keyword")).thenReturn(mockResults);

        // Provedení GET požadavku
        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "test keyword")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.keyword").value("test keyword"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].title").value("Test Title 1"))
                .andExpect(jsonPath("$.results[0].url").value("https://example1.com"))
                .andExpect(jsonPath("$.results[0].description").value("Test Description 1"))
                .andExpect(jsonPath("$.results[1].title").value("Test Title 2"))
                .andExpect(jsonPath("$.results[1].url").value("https://example2.com"))
                .andExpect(jsonPath("$.results[1].description").value("Test Description 2"))
                .andExpect(jsonPath("$.message").value("Úspěšně nalezeno 2 výsledků"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Test API volání s prázdným klíčovým slovem
     */
    @Test
    void testScrapeGoogleResults_EmptyKeyword() throws Exception {
        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Klíčové slovo nesmí být prázdné"))
                .andExpect(jsonPath("$.results").doesNotExist());
    }

    /**
     * Test API volání bez parametru keyword
     */
    @Test
    void testScrapeGoogleResults_MissingKeyword() throws Exception {
        mockMvc.perform(get("/api/scrape")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test API volání s výjimkou ve službě
     */
    @Test
    void testScrapeGoogleResults_ServiceException() throws Exception {
        when(scraperService.scrapeGoogleResults(anyString()))
                .thenThrow(new RuntimeException("Chyba při scrapingu"));

        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "test keyword")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.keyword").value("test keyword"))
                .andExpect(jsonPath("$.message").value("Chyba při získávání výsledků: Chyba při scrapingu"))
                .andExpect(jsonPath("$.results").doesNotExist());
    }

    /**
     * Test API volání s InterruptedException
     */
    @Test
    void testScrapeGoogleResults_InterruptedException() throws Exception {
        when(scraperService.scrapeGoogleResults(anyString()))
                .thenThrow(new InterruptedException("Přerušeno"));

        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "test keyword")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.keyword").value("test keyword"))
                .andExpect(jsonPath("$.message").value("Scraping byl přerušen"))
                .andExpect(jsonPath("$.results").doesNotExist());
    }

    /**
     * Test API volání s prázdnými výsledky
     */
    @Test
    void testScrapeGoogleResults_EmptyResults() throws Exception {
        when(scraperService.scrapeGoogleResults("test keyword"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "test keyword")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.keyword").value("test keyword"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(0))
                .andExpect(jsonPath("$.message").value("Úspěšně nalezeno 0 výsledků"));
    }

    /**
     * Test health check endpointu
     */
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Google Scraper API je funkční"));
    }

    /**
     * Test API volání s mezerami v klíčovém slovu
     */
    @Test
    void testScrapeGoogleResults_KeywordWithSpaces() throws Exception {
        List<SearchResultModel> mockResults = Arrays.asList(
                new SearchResultModel("Test Title", "https://example.com", "Test Description")
        );

        when(scraperService.scrapeGoogleResults("test keyword with spaces"))
                .thenReturn(mockResults);

        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "  test keyword with spaces  ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.keyword").value("test keyword with spaces"))
                .andExpect(jsonPath("$.results.length()").value(1));
    }
}