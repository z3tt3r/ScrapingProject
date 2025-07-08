
import models.SearchResultModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import services.GoogleScraperService;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit testy pro GoogleScraperService
 */
@ExtendWith(MockitoExtension.class)
class GoogleScraperServiceTest {

    private GoogleScraperService scraperService;

    @BeforeEach
    void setUp() {
        scraperService = new GoogleScraperService();
    }

    /**
     * Test parsování HTML s organickými výsledky
     */
    @Test
    void testParseSearchResults_ValidHTML_ReturnsResults() throws Exception {
        // Příprava mock HTML dat s organickými výsledky
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
                        <!-- Reklama - měla by být ignorována -->
                        <div class="g ads-ad">
                            <h3>Reklama</h3>
                            <a href="https://ads.example.com">Ad Link</a>
                            <div class="VwiC3b">Popis reklamy</div>
                        </div>
                    </body>
                </html>
                """;

        Document mockDoc = Jsoup.parse(mockHtml);

        // Použití reflexe pro volání private metody
        Method parseMethod = GoogleScraperService.class.getDeclaredMethod("parseSearchResults", Document.class);
        parseMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SearchResultModel> results = (List<SearchResultModel>) parseMethod.invoke(scraperService, mockDoc);

        // Ověření výsledků
        assertNotNull(results);
        assertEquals(2, results.size()); // Pouze 2 organické výsledky, reklama ignorována

        SearchResultModel firstResult = results.get(0);
        assertEquals("První výsledek", firstResult.getTitle());
        assertEquals("https://example1.com", firstResult.getUrl());
        assertEquals("Popis prvního výsledku", firstResult.getDescription());

        SearchResultModel secondResult = results.get(1);
        assertEquals("Druhý výsledek", secondResult.getTitle());
        assertEquals("https://example2.com", secondResult.getUrl());
        assertEquals("Popis druhého výsledku", secondResult.getDescription());
    }

    /**
     * Test parsování HTML s reklamami
     */
    @Test
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
                        <div class="g">
                            <h3>Sponsored výsledek</h3>
                            <a href="https://sponsored.example.com">Sponsored Link</a>
                            <div class="VwiC3b">sponsored content</div>
                        </div>
                    </body>
                </html>
                """;

        Document mockDoc = Jsoup.parse(mockHtml);

        Method parseMethod = GoogleScraperService.class.getDeclaredMethod("parseSearchResults", Document.class);
        parseMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SearchResultModel> results = (List<SearchResultModel>) parseMethod.invoke(scraperService, mockDoc);

        // Ověření, že reklamy jsou ignorovány
        assertNotNull(results);
        assertEquals(1, results.size()); // Pouze 1 organický výsledek

        SearchResultModel result = results.get(0);
        assertEquals("Organický výsledek", result.getTitle());
        assertEquals("https://organic.example.com", result.getUrl());
    }

    /**
     * Test čištění URL
     */
    @Test
    void testCleanUrl_GoogleRedirect_ReturnsCleanUrl() throws Exception {
        Method cleanUrlMethod = GoogleScraperService.class.getDeclaredMethod("cleanUrl", String.class);
        cleanUrlMethod.setAccessible(true);

        String dirtyUrl = "/url?q=https://example.com&sa=U&ved=123";
        String cleanUrl = (String) cleanUrlMethod.invoke(scraperService, dirtyUrl);

        assertEquals("https://example.com", cleanUrl);
    }

    /**
     * Test validace výsledků
     */
    @Test
    void testIsValidResult_ValidResult_ReturnsTrue() throws Exception {
        Method isValidMethod = GoogleScraperService.class.getDeclaredMethod("isValidResult", SearchResultModel.class);
        isValidMethod.setAccessible(true);

        SearchResultModel validResult = new SearchResultModel("Test Title", "https://example.com", "Test Description");
        boolean isValid = (boolean) isValidMethod.invoke(scraperService, validResult);

        assertTrue(isValid);
    }

    /**
     * Test validace výsledků - neplatný výsledek
     */
    @Test
    void testIsValidResult_InvalidResult_ReturnsFalse() throws Exception {
        Method isValidMethod = GoogleScraperService.class.getDeclaredMethod("isValidResult", SearchResultModel.class);
        isValidMethod.setAccessible(true);

        // Test s prázdným titulem
        SearchResultModel invalidResult1 = new SearchResultModel("", "https://example.com", "Test Description");
        boolean isValid1 = (boolean) isValidMethod.invoke(scraperService, invalidResult1);
        assertFalse(isValid1);

        // Test s prázdným URL
        SearchResultModel invalidResult2 = new SearchResultModel("Test Title", "", "Test Description");
        boolean isValid2 = (boolean) isValidMethod.invoke(scraperService, invalidResult2);
        assertFalse(isValid2);

        // Test s null hodnoty
        SearchResultModel invalidResult3 = new SearchResultModel(null, "https://example.com", "Test Description");
        boolean isValid3 = (boolean) isValidMethod.invoke(scraperService, invalidResult3);
        assertFalse(isValid3);
    }

    /**
     * Test detekce reklam
     */
    @Test
    void testIsAdvertisement_AdElement_ReturnsTrue() throws Exception {
        Method isAdMethod = GoogleScraperService.class.getDeclaredMethod("isAdvertisement", org.jsoup.nodes.Element.class);
        isAdMethod.setAccessible(true);

        String adHtml = "<div class=\"ads-ad\"><h3>Reklama</h3></div>";
        Document doc = Jsoup.parse(adHtml);
        org.jsoup.nodes.Element adElement = doc.select("div").first();

        boolean isAd = (boolean) isAdMethod.invoke(scraperService, adElement);
        assertTrue(isAd);
    }

    /**
     * Test parsování prázdného HTML
     */
    @Test
    void testParseSearchResults_EmptyHTML_ReturnsEmptyList() throws Exception {
        String emptyHtml = "<html><body></body></html>";
        Document mockDoc = Jsoup.parse(emptyHtml);

        Method parseMethod = GoogleScraperService.class.getDeclaredMethod("parseSearchResults", Document.class);
        parseMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<SearchResultModel> results = (List<SearchResultModel>) parseMethod.invoke(scraperService, mockDoc);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}