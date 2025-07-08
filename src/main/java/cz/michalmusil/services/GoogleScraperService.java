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
import java.util.concurrent.TimeUnit; // Stále potřebujeme pro Thread.sleep

// Selenium Imports
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration; // Důležité pro WebDriverWait v novějších verzích Selenium
import org.openqa.selenium.TimeoutException;

// WebDriverManager Import (pro automatické stažení ChromeDriveru)
import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class GoogleScraperService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleScraperService.class);

    // Rozšířený seznam USER_AGENTS pro případné použití s Jsoup nebo pro nastavení v Selenium
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:127.0) Gecko/20100101 Firefox/127.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Edge/126.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:127.0) Gecko/20100101 Firefox/127.0",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPad; CPU OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/126.0.0.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 12; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPad; CPU OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
    };

    private final Random random = new Random();

    /**
     * Původní metoda pro scraping Google výsledků pomocí Jsoup.
     * Tato metoda je zakomentovaná, aby sloužila pouze pro ukázku
     * a porovnání s robustnějším řešením se Selenium.
     *
     * @param keyword Klíčové slovo pro vyhledávání.
     * @return Seznam výsledků vyhledávání.
     * @throws IOException
     * @throws InterruptedException
     */
    /*
    public List<SearchResultModel> scrapeGoogleResults(String keyword) throws IOException, InterruptedException {
        List<SearchResultModel> results = new ArrayList<>();
        String searchUrl = "https://www.google.com/search?q=" + keyword;

        try {
            // Zpoždění 10-20 sekund pro simulaci lidského chování
            int delay = 10 + random.nextInt(11); // Nyní 10-20 sekund
            logger.info("Jsoup: Čekám {} sekund před požadavkem na Google.", delay);
            TimeUnit.SECONDS.sleep(delay);

            String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
            logger.info("Jsoup: Používám User-Agent: {}", userAgent);

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(userAgent)
                    .timeout(10000) // 10 sekund timeout
                    .get();

            logger.info("Jsoup: Stránka stažena pro klíčové slovo: {}", keyword);
            // logger.debug("Jsoup: Stažené HTML (prvních 500 znaků): {}", doc.html().substring(0, Math.min(doc.html().length(), 500)));


            Elements searchResults = doc.select("div.g"); // Selektor pro hlavní výsledky

            if (searchResults.isEmpty()) {
                logger.warn("Jsoup: Žádné výsledky nalezeny pro klíčové slovo '{}' pomocí selektoru 'div.g'. Možná blokace nebo změna struktury.", keyword);
            }

            for (Element result : searchResults) {
                // Přesné selektory je vždy dobré si ověřit v prohlížeči (F12) pro aktuální Google
                String title = result.select("h3").first() != null ? result.select("h3").first().text() : "N/A";
                // URL je často v a.href uvnitř div.g, nebo jako atribut data-ved v divu
                String url = result.select("a[href]").first() != null ? result.select("a[href]").first().attr("href") : "N/A";
                String snippet = result.select(".lBwE0B.NA7gNc.IUO0K").first() != null ? result.select(".lBwE0B.NA7gNc.IUO0K").first().text() : "N/A"; // Aktuální snippet selektor
                String description = result.select(".VwiC3b").first() != null ? result.select(".VwiC3b").first().text() : "N/A"; // Alternativní selektor pro popisek
                String domain = result.select(".yuRUbf .TbwUpd.NJjxre cite").first() != null ? result.select(".yuRUbf .TbwUpd.NJjxre cite").first().text() : "N/A";


                if (!isAdvertisement(result) && isValidResult(url)) {
                    results.add(extractSearchResult(title, url, description));
                }
            }

        } catch (IOException e) {
            logger.error("Jsoup: Chyba připojení nebo čtení dat pro klíčové slovo '{}': {}", keyword, e.getMessage());
            // Pokud je chyba typu "HTTP error fetching URL", Google nás zablokoval
            if (e.getMessage() != null && e.getMessage().contains("status=429")) { // Příklad pro Too Many Requests
                logger.error("Jsoup: Google pravděpodobně zablokoval požadavek (HTTP 429 - Too Many Requests).");
            }
            if (e.getMessage() != null && (e.getMessage().contains("HTTP error fetching URL") || e.getMessage().contains("SSLHandshakeException"))) {
                 logger.error("Jsoup: Google pravděpodobně detekoval bota a odmítl připojení.");
            }
        } catch (Exception e) {
            logger.error("Jsoup: Neočekávaná chyba při scrapingu pro klíčové slovo '{}': {}", keyword, e.getMessage(), e);
        }
        return results;
    }
    */

    /**
     * Nová, robustnější metoda pro scraping Google výsledků pomocí Selenium s Headless Chrome.
     * Tento přístup je výrazně efektivnější pro obejití protibotových opatření.
     *
     * @param keyword Klíčové slovo pro vyhledávání.
     * @return Seznam výsledků vyhledávání.
     */
    public List<SearchResultModel> scrapeGoogleResultsSelenium(String keyword) {
        List<SearchResultModel> results = new ArrayList<>();
        WebDriver driver = null; // Inicializujeme WebDriver na null

        try {
            // 1. Nastavení WebDriverManager (automatické stažení ChromeDriveru)
            WebDriverManager.chromedriver().setup();
            logger.info("Selenium: ChromeDriver nastaven pomocí WebDriverManager.");

            // 2. Konfigurace ChromeOptions pro headless mód a další nastavení
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Spustí Chrome v headless (bezhlavém) režimu
            options.addArguments("--disable-gpu"); // Doporučeno pro headless na některých systémech
            options.addArguments("--no-sandbox"); // Důležité pro Linux prostředí a izolaci
            options.addArguments("--window-size=1920,1080"); // Simulace standardního rozlišení obrazovky
            // --- Vylepšené maskování Selenium ---
            options.setExperimentalOption("excludeSwitches", List.of("enable-automation", "load-extension"));
            options.setExperimentalOption("useAutomationExtension", false);
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--incognito"); // Použít anonymní režim pro čistší session
            // Nastavení náhodného User-Agentu. Selenium by sice použilo svůj, ale takto je to explicitní a rozmanitější.
            options.addArguments("user-agent=" + USER_AGENTS[random.nextInt(USER_AGENTS.length)]);
            // Můžete přidat i další argumenty pro lepší maskování, např. odpojení od automatizace
            options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
            options.setExperimentalOption("useAutomationExtension", false);

            // 3. Inicializace ChromeDriveru
            driver = new ChromeDriver(options);
            logger.info("Selenium: WebDriver inicializován v headless režimu s nastaveným User-Agentem.");

            // 4. Sestavení URL pro Google vyhledávání
            String searchUrl = "https://www.google.com/search?q=" + keyword;
            logger.info("Selenium: Naviguji na URL: {}", searchUrl);
            driver.get(searchUrl);

            // 5. Počkání na načtení stránky a přítomnost výsledků (důležité pro dynamické stránky)
            // Použijeme WebDriverWait pro čekání na konkrétní element, což je robustnější než pevné sleep
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45)); // Max. 20 sekund čekání
            // Rozšířená detekce CAPTCHA / prázdných výsledků
            try {
                // Snažíme se počkat na hlavní element s výsledky vyhledávání
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));
                logger.info("Selenium: Stránka načtena, hlavní element výsledků ('search') je přítomen.");

                // Přidáme delší náhodnou pauzu pro simulaci uživatelského chování PŘED parsováním
                int delay = 10 + random.nextInt(11); // Náhodné zpoždění 10-20 sekund
                logger.info("Selenium: Čekám {} sekund pro simulaci uživatelského chování.", delay);
                TimeUnit.SECONDS.sleep(delay);

            } catch (TimeoutException e) {
                logger.warn("Selenium: Element 'search' nebyl nalezen v časovém limitu ({}s). Pravděpodobná blokace.", 45);
                String pageSource = driver.getPageSource(); // Získejte zdroj pro diagnostiku

                // Zkontrolujeme typické indikátory CAPTCHA nebo blokace
                if (pageSource.contains("Our systems have detected unusual traffic") ||
                        pageSource.contains("recaptcha") ||
                        pageSource.contains("reCAPTCHA_enterprise")) {
                    logger.error("Selenium: Google detekoval bota a zobrazil CAPTCHA stránku. Scraping nelze provést.");
                    return new ArrayList<>(); // Vracíme prázdný seznam, protože došlo k blokaci
                } else if (pageSource.contains("did not match any documents")) {
                    logger.warn("Selenium: Google vrátil stránku 'No results found'. Možná neplatné klíčové slovo nebo jiný problém.");
                    return new ArrayList<>();
                }
                else {
                    logger.error("Selenium: Neočekávaná struktura stránky nebo jiná neznámá blokace. Nelze nalézt výsledky.");
                    // Můžeme se rozhodnout buď vrátit prázdný list, nebo re-throw chybu pro kontroler
                    return new ArrayList<>();
                }
            }

            // 6. Získání obsahu stránky a předání Jsoup pro parsování
            // Selenium vrátí kompletní DOM po provedení JavaScriptu
            String pageSource = driver.getPageSource();
            logger.debug("Selenium: Stažený pageSource (prvních 500 znaků): {}", pageSource.substring(0, Math.min(pageSource.length(), 500)));

            Document doc = Jsoup.parse(pageSource); // Jsoup nyní dostane "čisté" HTML

            // 7. Parsování výsledků pomocí Jsoup (stejně jako dříve)
            Elements searchResults = doc.select("div.g"); // Selektor pro hlavní výsledky

            if (searchResults.isEmpty()) {
                logger.warn("Selenium: Žádné výsledky nalezeny pro klíčové slovo '{}' pomocí selektoru 'div.g'.", keyword);
                // Může se stát, že Google změnil selektory, nebo že je blokace tak silná, že i Selenium narazí na CAPTCHA
                if (pageSource.contains("Our systems have detected unusual traffic") || pageSource.contains("recaptcha")) {
                    logger.error("Selenium: Google detekoval bota a zobrazil CAPTCHA. Selenium bylo zablokováno.");
                } else {
                    logger.warn("Selenium: Pravděpodobná změna HTML struktury Google. Je potřeba aktualizovat Jsoup selektory.");
                }
            }

            for (Element result : searchResults) {
                // Přesné selektory zkontrolujte v prohlížeči pro aktuální Google
                String title = result.select("h3").first() != null ? result.select("h3").first().text() : "N/A";
                // URL je často v a.href uvnitř div.g, nebo jako atribut data-ved v divu
                String url = result.select("a[href]").first() != null ? result.select("a[href]").first().attr("href") : "N/A";
                String snippet = result.select(".lBwE0B.NA7gNc.IUO0K").first() != null ? result.select(".lBwE0B.NA7gNc.IUO0K").first().text() : "N/A"; // Aktuální snippet selektor
                String description = result.select(".VwiC3b").first() != null ? result.select(".VwiC3b").first().text() : "N/A"; // Alternativní selektor pro popisek
                String domain = result.select(".yuRUbf .TbwUpd.NJjxre cite").first() != null ? result.select(".yuRUbf .TbwUpd.NJjxre cite").first().text() : "N/A";

                if (!isAdvertisement(result) && isValidResult(url)) {
                    results.add(extractSearchResult(title, url, snippet, description, domain));
                }
            }
            logger.info("Selenium: Scraping pro '{}' dokončen. Nalezeno {} výsledků.", keyword, results.size());

        } catch (Exception e) {
            logger.error("Selenium: Chyba při Selenium scrapingu pro klíčové slovo '{}': {}", keyword, e.getMessage(), e);
            if (driver != null && driver.getPageSource().contains("Our systems have detected unusual traffic")) {
                logger.error("Selenium: Google detekoval neobvyklý provoz a zobrazil CAPTCHA stránku.");
            }
        } finally {
            // 8. Důležité: Ukončení WebDriveru pro uvolnění systémových zdrojů
            if (driver != null) {
                driver.quit(); // Ukončí proces Chrome a vyčistí zdroje
                logger.info("Selenium: WebDriver ukončen.");
            }
        }
        return results;
    }

    /**
     * Pomocná metoda pro kontrolu, zda je výsledek reklama.
     * Potřebuje aktualizovat selektory pro detekci reklam.
     */
    private boolean isAdvertisement(Element result) {
        // Příklad selektoru pro reklamy. Google často mění třídy.
        // Běžné třídy pro reklamy mohou být ".ads-ad", "div[data-text-ad]", nebo text "Reklama" uvnitř elementu
        // Zkontrolujte aktuální DOM v prohlížeči (F12) pro přesné selektory.
        boolean isAd = result.select(".ads-ad").first() != null ||
                result.select("span.cHIEz.WJg5P").first() != null; // Často používaná třída pro "Sponsored"
        if (isAd) {
            logger.debug("Identifikována reklama, ignoruji: {}", result.select("h3").first() != null ? result.select("h3").first().text() : "N/A");
        }
        return isAd;
    }

    /**
     * Pomocná metoda pro kontrolu platnosti URL.
     */
    private boolean isValidResult(String url) {
        return url != null && !url.isEmpty() && !url.startsWith("http://accounts.google.com") && !url.contains("google.com/search") && !url.contains("policies.google.com");
    }

    /**
     * Pomocná metoda pro extrakci dat a vytvoření objektu SearchResultModel.
     */
    private SearchResultModel extractSearchResult(String title, String url, String snippet, String description, String domain) {
        String cleanedUrl = cleanUrl(url); // Stále používáme cleanUrl pro odstranění Google přesměrování
        logger.debug("Extrahován výsledek: Titulek='{}', URL='{}', Popis='{}'", title, cleanedUrl, description);
        return new SearchResultModel(title, cleanedUrl, description);
    }

    /**
     * Pomocná metoda pro čištění URL z Google přesměrování.
     */
    private String cleanUrl(String url) {
        if (url != null && url.startsWith("/url?q=")) {
            int ampIndex = url.indexOf('&');
            if (ampIndex != -1) {
                return url.substring("/url?q=".length(), ampIndex);
            }
            return url.substring("/url?q=".length());
        }
        return url;
    }
}