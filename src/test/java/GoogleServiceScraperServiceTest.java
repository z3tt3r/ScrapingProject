import cz.michalmusil.models.SearchResultModel;
import cz.michalmusil.services.GoogleScraperService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName; // For more descriptive test names
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GoogleScraperService.
 * These tests focus on verifying the core business logic of the service,
 * especially the HTML parsing and data extraction.
 */
@ExtendWith(MockitoExtension.class) // Good for general mocking, though not strictly needed if no @Mock is used directly
class GoogleScraperServiceTest {

    private GoogleScraperService scraperService;

    @BeforeEach
    void setUp() {
        scraperService = new GoogleScraperService();
        // If you had dependencies in GoogleScraperService, you'd mock them here
        // e.g., if it depended on a HttpClient, you'd mock that.
    }

    /**
     * Test parsing HTML with organic search results.
     * Verifies that valid results are extracted correctly.
     */
    @Test
    @DisplayName("parseSearchResults should extract valid organic results from HTML")
    void testParseSearchResults_ValidHTML_ReturnsResults() throws Exception {
        // Given: Mock HTML with organic results
        String mockHtml = """
                <html>
                    <body>
                        <div class="g">
                            <h3>První výsledek</h3>
                            <a href="https://example1.com">Link 1</a>
                            <div class="VwiC3b">Popis prvního výsledku</div>
                        </div>
                        <div class="g">
                            <h3>Druhý výsledek</h3>
                            <a href="https://example2.com">Link 2</a>
                            <div class="VwiC3b">Popis druhého výsledku</div>
                        </div>
                        <div class="g ads-ad">
                            <h3>Reklama</h3>
                            <a href="https://ads.example.com">Ad Link</a>
                            <div class="VwiC3b">Popis reklamy</div>
                        </div>
                        <div class="g other-content">
                            <h3>Some other content</h3>
                            <a></a>
                            <div></div>
                        </div>
                    </body>
                </html>
                """;

        Document mockDoc = Jsoup.parse(mockHtml);

        // When: Call the private parseSearchResults method using reflection
        // NOTE: Testing private methods directly via reflection is generally discouraged
        // as it breaks encapsulation. A better approach would be to make this method
        // package-private or extract it to a separate utility class if its complexity
        // warrants direct testing. For interview purposes, it demonstrates knowledge
        // but be prepared to discuss alternatives.
        Method parseMethod = GoogleScraperService.class.getDeclaredMethod("parseSearchResults", Document.class);
        parseMethod.setAccessible(true); // Allow access to private method

        @SuppressWarnings("unchecked") // Suppress warning for unchecked cast
        List<SearchResultModel> results = (List<SearchResultModel>) parseMethod.invoke(scraperService, mockDoc);

        // Then: Assert the extracted results
        assertNotNull(results, "Results list should not be null");
        assertEquals(2, results.size(), "Should extract exactly 2 organic results (ads ignored)");

        SearchResultModel firstResult = results.get(0);
        assertEquals("První výsledek", firstResult.getTitle(), "First result title mismatch");
        assertEquals("https://example1.com", firstResult.getUrl(), "First result URL mismatch");
        assertEquals("Popis prvního výsledku", firstResult.getDescription(), "First result description mismatch");

        SearchResultModel secondResult = results.get(1);
        assertEquals("Druhý výsledek", secondResult.getTitle(), "Second result title mismatch");
        assertEquals("https://example2.com", secondResult.getUrl(), "Second result URL mismatch");
        assertEquals("Popis druhého výsledku", secondResult.getDescription(), "Second result description mismatch");
    }

    /**
     * Test parsing HTML where ads are present and should be ignored.
     * Ensures only organic results are returned.
     */
    @Test
    @DisplayName("parseSearchResults should ignore ad elements based on various indicators")
    void testParseSearchResults_WithAds_IgnoresAds() throws Exception {
        String mockHtml = """
                <html>
                    <body>
                        <div class="g ads-ad">
                            <h3>Reklama 1</h3>
                            <a href="https://ads1.example.com">Ad Link 1</a>
                            <div class="VwiC3b">Popis reklamy 1</div>
                        </div>
                        <div class="g">
                            <h3>Organický výsledek</h3>
                            <a href="https://organic.example.com">Organic Link</a>
                            <div class="VwiC3b">Popis organického výsledku</div>
                        </div>
                        <div class="g sponsored-content">
                            <h3>Sponsored výsledek</h3>
                            <a href="https://sponsored.example.com">Sponsored Link</a>
                            <span class="VwiC3b">sponsored content</span>
                            <span class="Ad"></span> </div>
                        <div class="tF2Cxc"> <h3>Another Organic</h3>
                            <a href="https://another-organic.com">Link</a>
                            <div class="VwiC3b">Description</div>
                        </div>
                    </body>
                </html>
                """;

        Document mockDoc = Jsoup.parse(mockHtml);

        Method parseMethod = GoogleScraperService.class.getDeclaredMethod("parseSearchResults", Document.class);
        parseMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SearchResultModel> results = (List<SearchResultModel>) parseMethod.invoke(scraperService, mockDoc);

        assertNotNull(results, "Results list should not be null");
        assertEquals(2, results.size(), "Should extract exactly 2 organic results (ads and sponsored ignored)");

        assertEquals("Organický výsledek", results.get(0).getTitle());
        assertEquals("https://organic.example.com", results.get(0).getUrl());

        assertEquals("Another Organic", results.get(1).getTitle());
        assertEquals("https://another-organic.com", results.get(1).getUrl());
    }

    /**
     * Test the cleanUrl method to ensure it correctly removes Google redirect parameters.
     */
    @Test
    @DisplayName("cleanUrl should remove Google redirect parameters")
    void testCleanUrl_GoogleRedirect_ReturnsCleanUrl() throws Exception {
        Method cleanUrlMethod = GoogleScraperService.class.getDeclaredMethod("cleanUrl", String.class);
        cleanUrlMethod.setAccessible(true);

        String dirtyUrl = "/url?q=https://example.com/some/path?param=value&another_param=abc&sa=U&ved=123";
        String cleanUrl = (String) cleanUrlMethod.invoke(scraperService, dirtyUrl);
        assertEquals("https://example.com/some/path?param=value&another_param=abc", cleanUrl, "URL should be cleaned before &sa=");

        // Test with no & after q=
        dirtyUrl = "/url?q=https://simple.com";
        cleanUrl = (String) cleanUrlMethod.invoke(scraperService, dirtyUrl);
        assertEquals("https://simple.com", cleanUrl, "URL should be cleaned when no further params exist");

        // Test with non-google URL
        dirtyUrl = "https://www.regularsite.com/page";
        cleanUrl = (String) cleanUrlMethod.invoke(scraperService, dirtyUrl);
        assertEquals("https://www.regularsite.com/page", cleanUrl, "Regular URL should remain unchanged");

        // Test with empty URL
        dirtyUrl = "";
        cleanUrl = (String) cleanUrlMethod.invoke(scraperService, dirtyUrl);
        assertEquals("", cleanUrl, "Empty URL should return empty string");

        // Test with null URL
        cleanUrl = (String) cleanUrlMethod.invoke(scraperService, null);
        assertNull(cleanUrl, "Null URL should return null"); // Or empty string, depends on cleanUrl implementation, currently returns ""
    }

    /**
     * Test cleanUrl when a simple URL is passed (not a Google redirect).
     */
    @Test
    @DisplayName("cleanUrl should return original URL if not a Google redirect")
    void testCleanUrl_NonGoogleUrl_ReturnsOriginalUrl() throws Exception {
        Method cleanUrlMethod = GoogleScraperService.class.getDeclaredMethod("cleanUrl", String.class);
        cleanUrlMethod.setAccessible(true);

        String regularUrl = "https://www.regularsite.com/page";
        String cleanedUrl = (String) cleanUrlMethod.invoke(scraperService, regularUrl);
        assertEquals(regularUrl, cleanedUrl);
    }

    /**
     * Test the isValidResult method for a valid SearchResultModel.
     */
    @Test
    @DisplayName("isValidResult should return true for a valid SearchResultModel")
    void testIsValidResult_ValidResult_ReturnsTrue() throws Exception {
        Method isValidMethod = GoogleScraperService.class.getDeclaredMethod("isValidResult", SearchResultModel.class);
        isValidMethod.setAccessible(true);

        SearchResultModel validResult = new SearchResultModel("Test Title", "https://example.com", "Test Description");
        boolean isValid = (boolean) isValidMethod.invoke(scraperService, validResult);
        assertTrue(isValid, "A valid result should return true");
    }

    /**
     * Test the isValidResult method for various invalid SearchResultModels.
     */
    @Test
    @DisplayName("isValidResult should return false for invalid SearchResultModels")
    void testIsValidResult_InvalidResult_ReturnsFalse() throws Exception {
        Method isValidMethod = GoogleScraperService.class.getDeclaredMethod("isValidResult", SearchResultModel.class);
        isValidMethod.setAccessible(true);

        // Test with empty title
        SearchResultModel invalidResult1 = new SearchResultModel("", "https://example.com", "Test Description");
        assertFalse((boolean) isValidMethod.invoke(scraperService, invalidResult1), "Should be false for empty title");

        // Test with null title
        SearchResultModel invalidResult2 = new SearchResultModel(null, "https://example.com", "Test Description");
        assertFalse((boolean) isValidMethod.invoke(scraperService, invalidResult2), "Should be false for null title");

        // Test with empty URL
        SearchResultModel invalidResult3 = new SearchResultModel("Test Title", "", "Test Description");
        assertFalse((boolean) isValidMethod.invoke(scraperService, invalidResult3), "Should be false for empty URL");

        // Test with null URL
        SearchResultModel invalidResult4 = new SearchResultModel("Test Title", null, "Test Description");
        assertFalse((boolean) isValidMethod.invoke(scraperService, invalidResult4), "Should be false for null URL");

        // Test with null result model
        assertFalse((boolean) isValidMethod.invoke(scraperService, (SearchResultModel) null), "Should be false for null SearchResultModel");
    }


    /**
     * Test the isAdvertisement method for an element identified as an ad.
     */
    @Test
    @DisplayName("isAdvertisement should return true for elements identified as ads")
    void testIsAdvertisement_AdElement_ReturnsTrue() throws Exception {
        Method isAdMethod = GoogleScraperService.class.getDeclaredMethod("isAdvertisement", org.jsoup.nodes.Element.class);
        isAdMethod.setAccessible(true);

        // Test cases for various ad indicators
        assertTrue((boolean) isAdMethod.invoke(scraperService, Jsoup.parse("<div class=\"ads-ad\"><h3>Reklama</h3></div>").selectFirst("div")), "Should detect 'ads-ad' class");
        assertTrue((boolean) isAdMethod.invoke(scraperService, Jsoup.parse("<div class=\"g sponsored\"><h3>Test</h3></div>").selectFirst("div")), "Should detect 'sponsored' in content");
        assertTrue((boolean) isAdMethod.invoke(scraperService, Jsoup.parse("<div class=\"g\"><span class=\"some-class\">Ad</span></div>").selectFirst("div")), "Should detect 'Ad' span");
        assertTrue((boolean) isAdMethod.invoke(scraperService, Jsoup.parse("<div class=\"g\"><span class=\"some-class\">Reklama</span></div>").selectFirst("div")), "Should detect 'Reklama' span");
        assertTrue((boolean) isAdMethod.invoke(scraperService, Jsoup.parse("<div class=\"g\"><a href=\"#\">Sponsored Content</a></div>").selectFirst("div")), "Should detect 'sponsored' in link text"); // Adjusted for your `html.contains("sponsored")` check
    }

    /**
     * Test the isAdvertisement method for an element that is not an ad.
     */
    @Test
    @DisplayName("isAdvertisement should return false for elements not identified as ads")
    void testIsAdvertisement_NonAdElement_ReturnsFalse() throws Exception {
        Method isAdMethod = GoogleScraperService.class.getDeclaredMethod("isAdvertisement", org.jsoup.nodes.Element.class);
        isAdMethod.setAccessible(true);

        String nonAdHtml = "<div class=\"g\"><h3>Regular Result</h3><a href=\"https://example.com\">Link</a></div>";
        Document doc = Jsoup.parse(nonAdHtml);
        org.jsoup.nodes.Element nonAdElement = doc.select("div").first();

        assertFalse((boolean) isAdMethod.invoke(scraperService, nonAdElement), "Should return false for a non-ad element");
    }

    /**
     * Test parsing an empty HTML document.
     * Expects an empty list of results.
     */
    @Test
    @DisplayName("parseSearchResults should return an empty list for empty HTML body")
    void testParseSearchResults_EmptyHTML_ReturnsEmptyList() throws Exception {
        String emptyHtml = "<html><body></body></html>";
        Document mockDoc = Jsoup.parse(emptyHtml);

        Method parseMethod = GoogleScraperService.class.getDeclaredMethod("parseSearchResults", Document.class);
        parseMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SearchResultModel> results = (List<SearchResultModel>) parseMethod.invoke(scraperService, mockDoc);

        assertNotNull(results, "Results list should not be null");
        assertTrue(results.isEmpty(), "Results list should be empty for empty HTML");
    }

    /**
     * Test parseSearchResults when no valid result containers are found.
     */
    @Test
    @DisplayName("parseSearchResults should return empty list if no valid result containers found")
    void testParseSearchResults_NoResultContainers_ReturnsEmptyList() throws Exception {
        String htmlWithNoResults = "<html><body><div class=\"some-other-div\">Content</div></body></html>";
        Document mockDoc = Jsoup.parse(htmlWithNoResults);

        Method parseMethod = GoogleScraperService.class.getDeclaredMethod("parseSearchResults", Document.class);
        parseMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SearchResultModel> results = (List<SearchResultModel>) parseMethod.invoke(scraperService, mockDoc);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Test the scenario where an extracted result has missing or null components (e.g., no title or URL).
     * Ensures such invalid results are not added to the final list.
     */
    @Test
    @DisplayName("parseSearchResults should skip invalid results with missing title or URL")
    void testParseSearchResults_InvalidResultSkipped() throws Exception {
        String mockHtml = """
                <html>
                    <body>
                        <div class="g">
                            <h3>Valid Title</h3>
                            <a href="https://valid.com">Valid Link</a>
                            <div class="VwiC3b">Valid Description</div>
                        </div>
                        <div class="g">
                            <a href="https://no-title.com">No Title Link</a>
                            <div class="VwiC3b">Description</div>
                        </div>
                        <div class="g">
                            <h3>No URL</h3>
                            <div class="VwiC3b">Description</div>
                        </div>
                    </body>
                </html>
                """;

        Document mockDoc = Jsoup.parse(mockHtml);

        Method parseMethod = GoogleScraperService.class.getDeclaredMethod("parseSearchResults", Document.class);
        parseMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SearchResultModel> results = (List<SearchResultModel>) parseMethod.invoke(scraperService, mockDoc);

        assertNotNull(results);
        assertEquals(1, results.size(), "Only one valid result should be extracted");
        assertEquals("Valid Title", results.get(0).getTitle());
    }
}