package seleniumautomation;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.JavascriptExecutor;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;

public class test1 {

    public static void main(String[] args) {

        // WebDriverManager דואג לדרייבר, בלי System.setProperty
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.manage().window().maximize();
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            // 1. פתיחת האתר
            driver.get("https://automationintesting.online/#/");

            // 2. גלילה לתחתית הדף (איפה שהטופס)
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

            // 3. איתור שדות הטופס
            // *** אם יש קו אדום / NoSuchElement – תעשה Inspect ותעדכן id / name לפי מה שאתה רואה ***
            WebElement name    = driver.findElement(By.id("name"));      // או By.name("name")
            WebElement email   = driver.findElement(By.id("email"));     // או By.name("email")
            WebElement phone   = driver.findElement(By.id("phone"));     // או By.name("phone")
            WebElement subject = driver.findElement(By.id("subject"));   // או By.name("subject")
            WebElement message = driver.findElement(By.id("description"));// לפעמים נקרא description

            // 4. מילוי השדות
            name.sendKeys("Ronen QA");
            email.sendKeys("ronen@example.com");
            phone.sendKeys("0501234567");
            subject.sendKeys("Selenium Test");
            message.sendKeys("הודעת בדיקה אוטומטית.");

            // 5. שליחה
            WebElement submitBtn = driver.findElement(By.cssSelector("button[type='submit']"));
            submitBtn.click();

            // 6. הדפסה למסך – אם לא נזרקה שגיאה עד פה, הטופס נשלח
            System.out.println("נראה שהטופס נשלח (אם יש הודעת הצלחה – זה בונוס 😊)");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
