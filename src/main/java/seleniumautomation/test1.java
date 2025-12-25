package seleniumautomation;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class test1 {

    public static void main(String[] args) {

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(18));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            driver.get("https://automationintesting.online/#/");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            boolean bookingDone = bookRoomFlow_CheckAvailability(driver, wait, js);
            System.out.println("BOOKING RESULT = " + bookingDone);

            sendContactForm(driver, wait, js);

            System.out.println("✅ סיום ריצה");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    // ===== BOOKING: Check Availability -> Calendar -> Reserve Now -> Form -> Reserve Now =====

    private static boolean bookRoomFlow_CheckAvailability(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {

        // 1) גלילה לאזור החדרים (רק כדי לוודא שהכפתור באזור נראה)
        js.executeScript("window.scrollTo(0, 500);");
        sleep(250);

        // 2) לחץ על הכפתור שמתחיל הזמנה: Check Availability (ובעתיד גם Book now אם יופיע)
        WebElement startBtn = findFirstPresent(driver, List.of(
                By.xpath("//button[normalize-space()='Check Availability']"),
                By.xpath("//button[normalize-space()='Book now']"),
                By.xpath("//button[normalize-space()='Book Now']")
        ));

        if (startBtn == null) {
            System.out.println("❌ לא מצאתי כפתור להתחלת הזמנה (Check Availability / Book now)");
            debugAllButtons(driver);
            return false;
        }

        safeClick(driver, wait, js, startBtn);
        System.out.println("✅ לחצתי על כפתור התחלת הזמנה: " + safe(startBtn.getText()));

        // 3) עכשיו אמור להופיע לוח שנה + כפתור Reserve Now
        WebElement reserveNow = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[normalize-space()='Reserve Now']")
        ));

        // 4) לבחור 2 ימים בלוח (קליקים על מספרי ימים שאינם disabled)
        if (!pickTwoDatesFromCalendar(driver, wait, js)) {
            System.out.println("❌ לא הצלחתי לבחור 2 תאריכים בלוח");
            return false;
        }

        // 5) Reserve Now (של שלב הלוח)
        reserveNow = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Reserve Now']")
        ));
        safeClick(driver, wait, js, reserveNow);
        System.out.println("✅ לחצתי Reserve Now אחרי בחירת תאריכים");

        // 6) טופס Book This Room (4 שדות)
        WebElement firstName = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='Firstname' or @aria-label='Firstname']")
        ));
        WebElement lastName = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='Lastname' or @aria-label='Lastname']")
        ));
        WebElement email = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='Email' or @type='email']")
        ));
        WebElement phone = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@placeholder='Phone' or @type='tel']")
        ));

        typeStrong(driver, wait, js, firstName, "Ronen");
        typeStrong(driver, wait, js, lastName, "QA");
        typeStrong(driver, wait, js, email, "ronen@example.com");
        typeStrong(driver, wait, js, phone, "07123456789"); // 11 ספרות

        // 7) Reserve Now (של הטופס)
        WebElement reserveNowForm = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Reserve Now']")
        ));
        safeClick(driver, wait, js, reserveNowForm);
        System.out.println("✅ לחצתי Reserve Now בטופס");

        sleep(800);
        return true;
    }

    /**
     * בוחר 2 ימים בלוח:
     * לוח כזה מציג את הימים בתאים ככפתורים עם טקסט מספרי (1..31).
     */
    private static boolean pickTwoDatesFromCalendar(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {

        // נחכה שיהיו כפתורי יום מספריים
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[not(@disabled)][string-length(normalize-space(.))<=2][translate(normalize-space(.), '0123456789', '')='']")
        ));

        List<WebElement> dayButtons = driver.findElements(By.xpath(
                "//button[not(@disabled)]" +
                        "[string-length(normalize-space(.))<=2]" +
                        "[translate(normalize-space(.), '0123456789', '')='']"
        ));

        if (dayButtons.size() < 2) {
            System.out.println("DEBUG: לא מצאתי מספיק ימים לבחור. found=" + dayButtons.size());
            return false;
        }

        // בוחרים שני ימים ראשונים זמינים
        WebElement day1 = dayButtons.get(0);
        WebElement day2 = dayButtons.get(1);

        safeClick(driver, wait, js, day1);
        sleep(200);
        safeClick(driver, wait, js, day2);
        sleep(200);

        System.out.println("✅ בחרתי ימים בלוח: " + safe(day1.getText()) + " ואז " + safe(day2.getText()));
        return true;
    }

    // ===== CONTACT =====

    private static void sendContactForm(WebDriver driver, WebDriverWait wait, JavascriptExecutor js) {

        js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
        sleep(300);

        // כדי שה-navbar לא יכסה את השדות
        js.executeScript("window.scrollBy(0, -350);");
        sleep(150);

        WebElement name    = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));
        WebElement email   = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        WebElement phone   = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("phone")));
        WebElement subject = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("subject")));
        WebElement message = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("description")));

        typeStrong(driver, wait, js, name, "Ronen QA");
        typeStrong(driver, wait, js, email, "ronen@example.com");
        typeStrong(driver, wait, js, phone, "07123456789"); // 11 ספרות
        typeStrong(driver, wait, js, subject, "Selenium Test");
        typeStrong(driver, wait, js, message, "הודעת בדיקה אוטומטית.");

        WebElement submitBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//button[normalize-space()='Submit']")
        ));
        safeClick(driver, wait, js, submitBtn);

        System.out.println("✅ הפנייה נשלחה");
    }

    // ===== HELPERS =====

    private static WebElement findFirstPresent(WebDriver driver, List<By> locators) {
        for (By by : locators) {
            List<WebElement> els = driver.findElements(by);
            if (!els.isEmpty()) return els.get(0);
        }
        return null;
    }

    private static void safeClick(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, WebElement el) {
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            js.executeScript("window.scrollBy(0, -160);");
            wait.until(ExpectedConditions.elementToBeClickable(el)).click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", el);
        }
    }

    private static void typeStrong(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, WebElement el, String text) {
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
            js.executeScript("window.scrollBy(0, -200);");
            js.executeScript("arguments[0].click();", el);

            el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            el.sendKeys(Keys.DELETE);
            el.sendKeys(text);
        } catch (Exception e) {
            // fallback קשוח
            js.executeScript("arguments[0].value = arguments[1];", el, text);
            js.executeScript("arguments[0].dispatchEvent(new Event('input', {bubbles:true}));", el);
            js.executeScript("arguments[0].dispatchEvent(new Event('change', {bubbles:true}));", el);
        }
    }

    private static void debugAllButtons(WebDriver driver) {
        List<WebElement> btns = driver.findElements(By.tagName("button"));
        System.out.println("==== DEBUG ALL BUTTONS (" + btns.size() + ") ====");
        for (WebElement b : btns) {
            String t = safe(b.getText());
            if (!t.isEmpty()) {
                System.out.println("BTN: '" + t + "' class='" + b.getAttribute("class") + "'");
            }
        }
        System.out.println("==== END DEBUG ====");
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
