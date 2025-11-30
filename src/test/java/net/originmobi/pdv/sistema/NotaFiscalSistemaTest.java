package net.originmobi.pdv.sistema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;
import net.originmobi.pdv.selenium.pages.login.LoginPage;
import net.originmobi.pdv.selenium.pages.notafiscal.NotaFiscalPage;

public class NotaFiscalSistemaTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static LoginPage loginPage;
    private static NotaFiscalPage notaPage;
    private static final String BASE_URL = "http://localhost:8080";

    @BeforeAll
    public static void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        loginPage = new LoginPage(driver, wait);
        notaPage = new NotaFiscalPage(driver, wait);

        // 1. Login
        loginPage.navigateTo(BASE_URL + "/login");
        loginPage.login("gerente", "123");
    }

    @Test
    public void testCriarNotaSimplificada() {
        notaPage.visitarListagem();
        notaPage.irParaNovaNota();

        notaPage.preencherNatureza("Venda de Mercadoria");

        notaPage.selecionarDestinatario("João Rafael Mendes Nogueira");

        assertDoesNotThrow(() -> notaPage.clicarCriarNota());

        boolean urlMudou = wait.until(d -> d.getCurrentUrl().contains("/notafiscal"));
        assertTrue(urlMudou, "Deveria ter redirecionado para a página da nota criada");
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}