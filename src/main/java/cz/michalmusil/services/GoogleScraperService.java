package cz.michalmusil.services;

import cz.michalmusil.models.SearchResultModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Služba pro scraping Google výsledků vyhledávání
 */
@Service
public class GoogleScraperService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleScraperService.class);

    // Rotace User-Agent stringů pro minimalizaci detekce
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    };

    private final Random random = new Random();

    /**
     * Provede scraping Google výsledků pro dané klíčové slovo
     * @param keyword klíčové slovní spojení
     * @return seznam nalezených výsledků
     */
    public List<SearchResultModel> scrapeGoogleResults(String keyword) throws IOException, InterruptedException {
        logger.info("Zahajuji scraping pro klíčové slovo: {}", keyword);

        // Náhodné zpoždění pro minimalizaci detekce (2-5 sekund)
        int delay = 2 + random.nextInt(4);
        logger.info("Čekám {} sekund před požadavkem", delay);
        TimeUnit.SECONDS.sleep(delay);

        // Sestavení URL
        String searchUrl = "https://www.google.com/search?q=" + keyword.replace(" ", "+") + "&num=10";
        logger.info("URL pro vyhledávání: {}", searchUrl);

        // Náhodný výběr User-Agent
        String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];

        // HTTP požadavek s realistickými hlavičkami
        Document doc = Jsoup.connect(searchUrl)
                .userAgent(userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "cs-CZ,cs;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("DNT", "1")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .timeout(10000)
                .get();

        return parseSearchResults(doc);
    }

    /**
     * Parsuje HTML dokument a extrahuje organické výsledky vyhledávání
     * @param doc HTML dokument
     * @return seznam výsledků
     */
    private List<SearchResultModel> parseSearchResults(Document doc) {
        List<SearchResultModel> results = new ArrayList<>();

        // Různé selektory pro organické výsledky (Google často mění strukturu)
        String[] selectors = {
                "div.g", // Hlavní kontejner pro organické výsledky
                "div[data-ved]", // Alternativní selektor
                ".tF2Cxc" // Další možný selektor
        };

        Elements searchResults = null;

        // Pokus o nalezení výsledků pomocí různých selektorů
        for (String selector : selectors) {
            searchResults = doc.select(selector);
            if (!searchResults.isEmpty()) {
                logger.info("Nalezeny výsledky pomocí selektoru: {}", selector);
                break;
            }
        }

        if (searchResults == null || searchResults.isEmpty()) {
            logger.warn("Nenalezeny žádné výsledky vyhledávání");
            return results;
        }

        int count = 0;
        for (Element result : searchResults) {
            if (count >= 10) break; // Omezit na prvních 10 výsledků

            try {
                // Ignorovat reklamy (obsahují určité třídy nebo atributy)
                if (isAdvertisement(result)) {
                    continue;
                }

                SearchResultModel searchResult = extractSearchResult(result);
                if (searchResult != null && isValidResult(searchResult)) {
                    results.add(searchResult);
                    count++;
                    logger.debug("Extrahován výsledek {}: {}", count, searchResult.getTitle());
                }
            } catch (Exception e) {
                logger.warn("Chyba při extrakci výsledku: {}", e.getMessage());
            }
        }

        logger.info("Celkem extrahováno {} výsledků", results.size());
        return results;
    }

    /**
     * Zjistí, zda je element reklama
     * @param element HTML element
     * @return true pokud je reklama
     */
    private boolean isAdvertisement(Element element) {
        // Kontrola různých indikátorů reklam
        String html = element.html().toLowerCase();
        String className = element.className().toLowerCase();

        return html.contains("ads-ad") ||
                html.contains("sponsored") ||
                className.contains("ads") ||
                element.select("span:contains(Ad)").size() > 0 ||
                element.select("span:contains(Reklama)").size() > 0;
    }

    /**
     * Extrahuje jednotlivý výsledek vyhledávání
     * @param element HTML element
     * @return SearchResult nebo null
     */
    private SearchResultModel extractSearchResult(Element element) {
        try {
            // Různé selektory pro titulek
            Element titleElement = element.selectFirst("h3");
            if (titleElement == null) {
                titleElement = element.selectFirst("a h3");
            }
            if (titleElement == null) {
                titleElement = element.selectFirst(".LC20lb");
            }

            // Různé selektory pro URL
            Element linkElement = element.selectFirst("a[href]");
            if (linkElement == null) {
                linkElement = element.selectFirst("h3 a[href]");
            }

            // Různé selektory pro popis
            Element descriptionElement = element.selectFirst(".VwiC3b");
            if (descriptionElement == null) {
                descriptionElement = element.selectFirst(".s");
            }
            if (descriptionElement == null) {
                descriptionElement = element.selectFirst("span:contains(...)");
            }

            String title = titleElement != null ? titleElement.text().trim() : "";
            String url = linkElement != null ? linkElement.attr("href") : "";
            String description = descriptionElement != null ? descriptionElement.text().trim() : "";

            // Čištění URL (odstranění Google redirect)
            url = cleanUrl(url);

            return new SearchResultModel(title, url, description);

        } catch (Exception e) {
            logger.warn("Chyba při extrakci výsledku: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Vyčistí URL od Google redirect parametrů
     * @param url původní URL
     * @return vyčištěné URL
     */
    private String cleanUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        // Odstranění Google redirect
        if (url.startsWith("/url?q=")) {
            int start = url.indexOf("q=") + 2;
            int end = url.indexOf("&", start);
            if (end == -1) {
                return url.substring(start);
            } else {
                return url.substring(start, end);
            }
        }

        return url;
    }

    /**
     * Validuje, zda je výsledek platný
     * @param result výsledek k validaci
     * @return true pokud je platný
     */
    private boolean isValidResult(SearchResultModel result) {
        return result != null &&
                result.getTitle() != null && !result.getTitle().isEmpty() &&
                result.getUrl() != null && !result.getUrl().isEmpty();
    }
}