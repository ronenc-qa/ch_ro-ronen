package seleniumautomation;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class test1 {

    private static final By CHECK_AVAILABILITY_BTN = By.cssSelector(
            "#booking > div > div > div > form > div > div.col-8.mt-4 > button"
    );

    private static final By ROOMS_BOOK_ANY = By.xpath(
            "//*[@id='rooms']//*[self::a or self::button]" +
            "[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'book')]"
    );

    // Reserve Now ראשון (אחרי Book Now, לפני טופס פרטים)
    private static final By RESERVE_NOW_BY_ID = By.id("doReservation");
    private static final By RESERVE_NOW_BY_TEXT = By.xpath("//*[self::button or self::a][contains(normalize-space(.),'Reserve')]");

    // ✅ Reserve Now שני (כפתור של הטופס) - בדיוק מה שנתת
    private static final By RESERVE_NOW_FORM_BTN = By.cssSelector(
            "#root-container > div > div.container.my-5 > div > div.col-lg-4 > div > div > form > button.btn.btn-primary.w-100.mb-3"
    );

    private static final By BOOKING_ALL_INPUTS = By.cssSelector("#booking input");

    // Contact form
    private static final By CONTACT_NAME = By.id("name");
    private static final By CONTACT_EMAIL = By.id("email");
    private static final By CONTACT_PHONE = By.id("phone");
    private static final By CONTACT_SUBJECT = By.id("subject");
    private static final By CONTACT_DESC = By.id("description");
    private static final By CONTACT_SUBMIT = By.xpath("//button[normalize-space()='Submit']");

    public static void main(String[] args) {

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            driver.get("https://automationintesting.online/#/");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking")));

            boolean ok = makeBookingFull(driver, wait, js);
            System.out.println("BOOKING RESULT = " + ok);

            // ✅ הכי יציב: לחזור ל-home ואז לשלוח Contact
            driver.get("https://automationintesting.online/#/");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking")));

            sendContactForm(driver, wait, js);

            System.out.println("✅ סיום ריצה");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private static boolean makeBookingFull(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {

        js.executeScript("window.scrollTo(0, 450);");
        sleep(250);

        // 1) תאריכים
        List<WebElement> dateInputs = findBookingDateInputs(driver);
        if (dateInputs.size() < 2) {
            System.out.println("❌ לא מצאתי 2 שדות תאריך ב-#booking. found=" + dateInputs.size());
            return false;
        }

        WebElement checkIn = dateInputs.get(0);
        WebElement checkOut = dateInputs.get(1);

        LocalDate in = LocalDate.now().plusDays(3);
        LocalDate out = LocalDate.now().plusDays(5);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        setValueWithEvents(js, checkIn, in.format(fmt));
        setValueWithEvents(js, checkOut, out.format(fmt));
        System.out.println("✅ מילאתי תאריכים: " + in.format(fmt) + " -> " + out.format(fmt));

        // 2) Check Availability
        WebElement checkAvail = wait.until(ExpectedConditions.elementToBeClickable(CHECK_AVAILABILITY_BTN));
        safeClick(driver, wait, js, checkAvail);
        System.out.println("✅ לחצתי Check Availability");

        // 3) Rooms -> Book (עם טיפול ב-stale)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("rooms")));
        js.executeScript("document.querySelector('#rooms')?.scrollIntoView({block:'start'});");
        sleep(600);

        if (!clickFirstBookWithRetry(driver, wait, js)) {
            System.out.println("❌ לא הצלחתי ללחוץ Book");
            return false;
        }
        System.out.println("✅ לחצתי Book בהצלחה");

        // 4) Reserve Now ראשון
        WebElement reserve1 = waitForReserveNow(driver, wait);
        if (reserve1 == null) {
            System.out.println("❌ לא מצאתי Reserve Now אחרי Book");
            return false;
        }
        safeClick(driver, wait, js, reserve1);
        System.out.println("✅ לחצתי Reserve Now (ראשון)");

        // 5) מילוי טופס Book This Room
        WebElement firstName = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder='Firstname']")));
        WebElement lastName  = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder='Lastname']")));
        WebElement email     = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder='Email']")));
        WebElement phone     = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder='Phone']")));

        typeStrong(driver, wait, js, firstName, "Ronen");
        typeStrong(driver, wait, js, lastName, "Cohen"); // 3-30
        typeStrong(driver, wait, js, email, "ronen@example.com");
        typeStrong(driver, wait, js, phone, "07123456789");

        // 6) ✅ Reserve Now שני - הכפתור המדויק שנתת
        WebElement reserve2 = wait.until(ExpectedConditions.elementToBeClickable(RESERVE_NOW_FORM_BTN));
        safeClick(driver, wait, js, reserve2);
        System.out.println("✅ לחצתי Reserve Now (שני - בטופס)");

        // תן רגע שהמערכת תעבד
        sleep(800);

        return true;
    }

    private static WebElement waitForReserveNow(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(d -> !d.findElements(RESERVE_NOW_BY_ID).isEmpty() || !d.findElements(RESERVE_NOW_BY_TEXT).isEmpty());
            List<WebElement> byId = driver.findElements(RESERVE_NOW_BY_ID);
            if (!byId.isEmpty()) return byId.get(0);

            List<WebElement> byText = driver.findElements(RESERVE_NOW_BY_TEXT);
            if (!byText.isEmpty()) return byText.get(0);

        } catch (TimeoutException ignored) {}
        return null;
    }

    private static boolean clickFirstBookWithRetry(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                wait.until(d -> !d.findElements(ROOMS_BOOK_ANY).isEmpty());
                List<WebElement> books = driver.findElements(ROOMS_BOOK_ANY);

                WebElement target = null;
                for (WebElement b : books) {
                    if (b.isDisplayed() && b.isEnabled()) { target = b; break; }
                }
                if (target == null) return false;

                js.executeScript("arguments[0].scrollIntoView({block:'center'});", target);
                js.executeScript("window.scrollBy(0, -160);");
                safeClick(driver, wait, js, target);
                return true;

            } catch (StaleElementReferenceException sere) {
                System.out.println("⚠️ stale על Book attempt " + attempt + " — re-find");
                sleep(250);
            } catch (TimeoutException te) {
                System.out.println("⚠️ timeout על Book attempt " + attempt);
            }
        }
        return false;
    }

    private static List<WebElement> findBookingDateInputs(WebDriver driver) {
        List<WebElement> all = driver.findElements(BOOKING_ALL_INPUTS);
        all.removeIf(el -> {
            String id = safe(el.getAttribute("id")).toLowerCase();
            return id.equals("name") || id.equals("email") || id.equals("phone") || id.equals("subject") || id.equals("description");
        });
        return all;
    }

    private static void setValueWithEvents(JavascriptExecutor js, WebElement el, String value) {
        js.executeScript(
                "arguments[0].scrollIntoView({block:'center'});" +
                        "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                        "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                el, value
        );
    }

    // ===== CONTACT =====
    private static void sendContactForm(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {

        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        sleep(600);
        js.executeScript("window.scrollBy(0, -450);");
        sleep(250);

        WebElement name    = wait.until(ExpectedConditions.visibilityOfElementLocated(CONTACT_NAME));
        WebElement email   = wait.until(ExpectedConditions.visibilityOfElementLocated(CONTACT_EMAIL));
        WebElement phone   = wait.until(ExpectedConditions.visibilityOfElementLocated(CONTACT_PHONE));
        WebElement subject = wait.until(ExpectedConditions.visibilityOfElementLocated(CONTACT_SUBJECT));
        WebElement message = wait.until(ExpectedConditions.visibilityOfElementLocated(CONTACT_DESC));

        typeStrong(driver, wait, js, name, "Ronen QA");
        typeStrong(driver, wait, js, email, "ronen@example.com");
        typeStrong(driver, wait, js, phone, "0501234567");
        typeStrong(driver, wait, js, subject, "Selenium Test");
        typeStrong(driver, wait, js, message, "הודעת בדיקה אוטומטית.");

        WebElement submit = wait.until(ExpectedConditions.elementToBeClickable(CONTACT_SUBMIT));
        safeClick(driver, wait, js, submit);

        System.out.println("✅ הפנייה נשלחה");
    }

    // ===== Utils =====
    private static void safeClick(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, WebElement el) {
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            js.executeScript("window.scrollBy(0, -180);");
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", el);
        }
    }

    private static void typeStrong(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, WebElement el, String text) {
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            js.executeScript("window.scrollBy(0, -220);");
            js.executeScript("arguments[0].click();", el);
            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            el.sendKeys(text);
        } catch (Exception e) {
            js.executeScript("arguments[0].value = arguments[1];", el, text);
            js.executeScript("arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", el);
            js.executeScript("arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", el);
        }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
