import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        // Set the path to the ChromeDriver
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\Ahmed\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        // Create a new instance of the Chrome driver
        WebDriver driver = new ChromeDriver();

        // Path for the CSV file
        String csvFilePath = "C:\\Users\\Ahmed\\Downloads\\" + java.util.UUID.randomUUID().toString() + ".csv";


        try (FileWriter writer = new FileWriter(csvFilePath)) {
            WebDriverWait wait = new WebDriverWait(driver, 60); // Wait for up to 1 minute

            // Write CSV header
            writer.append("Menu Item,Type\n");

            // Launch the login page and perform login steps
            loginToSite(wait, driver);

            // Click all menu items and write data to CSV
            clickAllMenuItems(wait, driver, writer);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }

    private static void loginToSite(WebDriverWait wait, WebDriver driver) throws InterruptedException {
        driver.get("http://erp1.tsi.com.tn:5000/account/login");
        performLogin(wait, driver);
        driver.get("http://erp1.tsi.com.tn:5000/app/home");
    }

    private static void performLogin(WebDriverWait wait, WebDriver driver) throws InterruptedException {
        WebElement societeDropdownTrigger = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("p-dropdown-trigger")));
        societeDropdownTrigger.click();
        Thread.sleep(1000); // Allow dropdown to open

        WebElement societeOption = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//li[@role='option' and @aria-label='HALUNG']")));
        societeOption.click();
        Thread.sleep(1000);

        WebElement usernameField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        usernameField.sendKeys("Administrateur");
        Thread.sleep(1000);

        WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("p-password-input")));
        passwordField.clear(); // Ensure the password field is left empty
        Thread.sleep(1000);

        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@class='p-element p-ripple w-full p-3 text-xl btn p-button p-component']//label[contains(text(), 'Se connecter')]")));
        loginButton.click();
        Thread.sleep(1000);

        wait.until(ExpectedConditions.urlContains("/app/home"));
    }

    private static void clickAllMenuItems(WebDriverWait wait, WebDriver driver, FileWriter writer) throws InterruptedException, IOException {
        // Get the menu container element
        WebElement menuContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".layout-menu")));

        // Get all menu item links
        List<WebElement> menuItems = menuContainer.findElements(By.cssSelector("a.p-ripple.p-element"));

        Set<String> problematicMenuItems = new HashSet<>();
        String originalWindow = driver.getWindowHandle();

        for (int i = 0; i < menuItems.size(); i++) {
            try {
                // Find the menu item again to avoid stale element exception
                menuContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".layout-menu")));
                menuItems = menuContainer.findElements(By.cssSelector("a.p-ripple.p-element"));

                // Click the menu item
                WebElement menuItem = menuItems.get(i);
                String menuItemText = menuItem.getText();

                // Skip problematic menu items
                if (problematicMenuItems.contains(menuItemText)) {
                    continue;
                }

                // Check if the item has more than 2 child elements
                boolean hasChildren = menuItem.findElements(By.cssSelector("*")).size() > 2;

                if (!hasChildren) {
                    // Open the menu item in a new tab using JavaScript
                    ((JavascriptExecutor) driver).executeScript("window.open(arguments[0].href, '_blank');", menuItem);
                    Thread.sleep(500); // Allow time for the new tab to open

                    // Switch to the new tab
                    for (String windowHandle : driver.getWindowHandles()) {
                        if (!windowHandle.equals(originalWindow)) {
                            driver.switchTo().window(windowHandle);
                            break;
                        }
                    }

                    // Check for the specific element indicating "Page non trouvée"
                    boolean pageNotFound = false;

                    try {
                        WebDriverWait wait2 = new WebDriverWait(driver, 10); // 10 seconds timeout
                        WebElement element = wait2.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/app-root/app-invalid-route/div/div/h1")));
                        pageNotFound = element.getText().equals("Page non trouvée");
                    } catch (Exception e) {
                        // Do nothing, pageNotFound remains false
                    }
                    if (pageNotFound) {
                        writer.append(String.format("%s,no\n", menuItemText));
                    } else {
                        writer.append(String.format("%s,yes\n", menuItemText));
                    }

                    // Close the new tab and switch back to the original window
                    driver.close();
                    driver.switchTo().window(originalWindow);
                } else {
                    // Click the menu item directly
                    menuItem.click();
                    writer.append(String.format("%s,has_children\n", menuItemText));

                    // Get sub-menu items if available
                    List<WebElement> subMenuItems = menuItem.findElements(By.cssSelector("a.p-ripple.p-element"));

                    for (WebElement subMenuItem : subMenuItems) {
                        String subMenuItemText = subMenuItem.getText();
                        if (problematicMenuItems.contains(subMenuItemText)) {
                            continue;
                        }

                        // Open sub-menu item in a new tab using JavaScript
                        ((JavascriptExecutor) driver).executeScript("window.open(arguments[0].href, '_blank');", subMenuItem);
                        Thread.sleep(500); // Allow time for the new tab to open

                        // Switch to the new tab
                        for (String windowHandle : driver.getWindowHandles()) {
                            if (!windowHandle.equals(originalWindow)) {
                                driver.switchTo().window(windowHandle);
                                break;
                            }
                        }

                        // Check for the specific element indicating "Page non trouvée"
                        boolean subPageNotFound = false;
                        try {
                            WebDriverWait wait2 = new WebDriverWait(driver, 10); // 10 seconds timeout
                            WebElement element = wait2.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/app-root/app-invalid-route/div/div/h1")));
                            subPageNotFound = element.getText().equals("Page non trouvée");
                        } catch (Exception e) {
                            // Do nothing, subPageNotFound remains false
                        }

                        if (subPageNotFound) {
                            writer.append(String.format("%s,no\n", subMenuItemText));
                        } else {
                            writer.append(String.format("%s,yes\n", subMenuItemText));
                        }

                        // Close the new tab and switch back to the original window
                        driver.close();
                        driver.switchTo().window(originalWindow);
                    }
                }

                writer.flush(); // Ensure data is written to the file incrementally

                // Stop after the last element is processed
                if (menuItemText.equals("Gestion Des Alertes")) {

                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                // Continue to next item even if an error occurs
            }
        }
    }
}