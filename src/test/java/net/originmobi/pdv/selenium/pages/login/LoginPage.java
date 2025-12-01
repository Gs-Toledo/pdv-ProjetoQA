package net.originmobi.pdv.selenium.pages.login;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {

    private WebDriver driver;
    private WebDriverWait wait;

    private By userField = By.id("user");
    private By passwordField = By.id("password");
    private By btnLogin = By.id("btn-login");
    private By painelLogin = By.id("painelLoginPrimaio");

    public LoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void navigateTo(String url) {
        driver.get(url);
    }

    public boolean isLoginPageDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(painelLogin)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void login(String username, String password) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(userField)).sendKeys(username);
        driver.findElement(passwordField).sendKeys(password);
        driver.findElement(btnLogin).click();
        wait.until(ExpectedConditions.urlContains("/"));
    }
}