import com.fasterxml.jackson.databind.ObjectMapper;
import cz.michalmusil.controllers.GoogleScraperController;
import cz.michalmusil.models.SearchResultModel;
import org.junit.jupiter.api.DisplayName; // For more descriptive test names
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import cz.michalmusil.services.GoogleScraperService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print; // Added for debugging output

/**
 * Unit tests for GoogleScraperController
 * These tests focus on verifying the controller's behavior,
 * including request mapping, parameter handling, and JSON response structure,
 * while mocking the underlying service logic.
 */
@WebMvcTest(GoogleScraperController.class)
class GoogleScraperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // The service layer is mocked to isolate controller testing
    @MockBean
    private GoogleScraperService scraperService;

    // ObjectMapper is useful for debugging but not strictly necessary for these tests
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test successful API call for scraping Google results.
     * Verifies correct status, content type, and JSON structure with mock data.
     */
    @Test
    @DisplayName("should return 200 OK with search results on successful scrape")
    void testScrapeGoogleResults_Success() throws Exception {
        // Given: Mock search results from the service
        List<SearchResultModel> mockResults = Arrays.asList(
                new SearchResultModel("Test Title 1", "https://example1.com", "Test Description 1"),
                new SearchResultModel("Test Title 2", "https://example2.com", "Test Description 2")
        );
        when(scraperService.scrapeGoogleResults("test keyword")).thenReturn(mockResults);

        // When & Then: Perform GET request and assert response
        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "test keyword")
                        .accept(MediaType.APPLICATION_JSON)) // Use accept for what we want to receive
                // .andDo(print()) // Uncomment for detailed debug output of the request/response
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
                .andExpect(jsonPath("$.timestamp").isNumber()); // More specific assertion for timestamp
    }

    /**
     * Test API call with an empty keyword parameter.
     * Expects a 400 Bad Request with a specific error message.
     */
    @Test
    @DisplayName("should return 400 Bad Request for empty keyword")
    void testScrapeGoogleResults_EmptyKeyword() throws Exception {
        // When & Then: Perform GET request with empty keyword and assert response
        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Klíčové slovo nesmí být prázdné"))
                .andExpect(jsonPath("$.results").doesNotExist());
    }

    /**
     * Test API call without the 'keyword' parameter.
     * Expects a 400 Bad Request due to missing required parameter.
     */
    @Test
    @DisplayName("should return 400 Bad Request for missing keyword parameter")
    void testScrapeGoogleResults_MissingKeyword() throws Exception {
        // When & Then: Perform GET request without keyword and assert response
        mockMvc.perform(get("/api/scrape")
                        .accept(MediaType.APPLICATION_JSON)) // Only accept header relevant here
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").doesNotExist()); // Or check for Spring's default error response if preferred
    }

    /**
     * Test API call when the underlying service throws a general RuntimeException.
     * Expects a 500 Internal Server Error with a detailed error message.
     */
    @Test
    @DisplayName("should return 500 Internal Server Error when service throws RuntimeException")
    void testScrapeGoogleResults_ServiceException() throws Exception {
        // Given: Service configured to throw a RuntimeException
        when(scraperService.scrapeGoogleResults(anyString()))
                .thenThrow(new RuntimeException("Chyba při scrapingu"));

        // When & Then: Perform GET request and assert response
        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "test keyword")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.keyword").value("test keyword"))
                .andExpect(jsonPath("$.message").value("Chyba při získávání výsledků: Chyba při scrapingu"))
                .andExpect(jsonPath("$.results").doesNotExist());
    }

    /**
     * Test API call when the underlying service throws an InterruptedException.
     * Expects a 500 Internal Server Error with a specific message for interruption.
     */
    @Test
    @DisplayName("should return 500 Internal Server Error when service throws InterruptedException")
    void testScrapeGoogleResults_InterruptedException() throws Exception {
        // Given: Service configured to throw an InterruptedException
        when(scraperService.scrapeGoogleResults(anyString()))
                .thenThrow(new InterruptedException("Přerušeno"));

        // When & Then: Perform GET request and assert response
        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "test keyword")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.keyword").value("test keyword"))
                .andExpect(jsonPath("$.message").value("Scraping byl přerušen"))
                .andExpect(jsonPath("$.results").doesNotExist());
    }

    /**
     * Test API call when the underlying service returns an empty list of results.
     * Expects a 200 OK with an empty results array.
     */
    @Test
    @DisplayName("should return 200 OK with empty results when no results found")
    void testScrapeGoogleResults_EmptyResults() throws Exception {
        // Given: Service returns an empty list
        when(scraperService.scrapeGoogleResults("test keyword"))
                .thenReturn(Collections.emptyList());

        // When & Then: Perform GET request and assert response
        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "test keyword")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.keyword").value("test keyword"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results.length()").value(0))
                .andExpect(jsonPath("$.message").value("Úspěšně nalezeno 0 výsledků"));
    }

    /**
     * Test the health check endpoint.
     * Verifies correct status and response body.
     */
    @Test
    @DisplayName("should return 200 OK for health endpoint")
    void testHealthEndpoint() throws Exception {
        // When & Then: Perform GET request and assert response
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.TEXT_PLAIN)) // Health endpoint returns text/plain
                .andExpect(status().isOk())
                .andExpect(content().string("Google Scraper API je funkční"));
    }

    /**
     * Test API call with a keyword containing leading/trailing spaces.
     * Verifies that the keyword is trimmed correctly by the controller.
     */
    @Test
    @DisplayName("should trim keyword and return results for keyword with spaces")
    void testScrapeGoogleResults_KeywordWithSpaces() throws Exception {
        // Given: Mock results for the trimmed keyword
        List<SearchResultModel> mockResults = Collections.singletonList(
                new SearchResultModel("Test Title", "https://example.com", "Test Description")
        );
        when(scraperService.scrapeGoogleResults("test keyword with spaces")) // Mock the service call with the *trimmed* keyword
                .thenReturn(mockResults);

        // When & Then: Perform GET request with spaced keyword and assert response
        mockMvc.perform(get("/api/scrape")
                        .param("keyword", "  test keyword with spaces  ") // Pass the keyword with spaces
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // Assert that the 'keyword' in the response is the trimmed version
                .andExpect(jsonPath("$.keyword").value("test keyword with spaces"))
                .andExpect(jsonPath("$.results.length()").value(1));
    }
}