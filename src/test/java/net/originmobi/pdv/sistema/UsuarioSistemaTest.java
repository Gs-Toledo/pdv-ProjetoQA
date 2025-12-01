package net.originmobi.pdv.sistema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UsuarioSistemaTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private final String BASE_URL = "http://localhost:8080";
    private int clickCount = 0;

    private void clicar(By by) {
        driver.findElement(by).click();
        clickCount++;
    }

    private void clicar(WebElement element) {
        element.click();
        clickCount++;
    }

    private void avaliarUsabilidade() {
        System.out.println("Total de cliques realizados no fluxo: " + clickCount);

        if (clickCount <= 5) {
            System.out.println("Usabilidade: OK (≤5 cliques)");
        } else if (clickCount <= 8) {
            System.out.println("Usabilidade: MÉDIA (6–8 cliques)");
        } else {
            System.out.println("Usabilidade: RUIM (>8 cliques)");
        }
    }

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        realizarLogin("gerente", "123");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void realizarLogin(String user, String pass) {
        driver.get(BASE_URL + "/login");
        if (!driver.getCurrentUrl().contains("login")) {
            driver.get(BASE_URL + "/logout");
            driver.get(BASE_URL + "/login");
        }

        driver.findElement(By.id("user")).sendKeys(user);
        driver.findElement(By.id("password")).sendKeys(pass);
        clicar(By.id("btn-login"));
    }

    @Test
    @DisplayName("RF-S.01 - Deve criar um usuário e atribuir um grupo de permissão")
    public void testCriarUsuarioEAtribuirGrupo() throws InterruptedException {
        driver.get(BASE_URL + "/usuario/form");

        try {
            Select selectPessoa = new Select(driver.findElement(By.id("pessoa")));
            selectPessoa.selectByIndex(1);

            driver.findElement(By.id("userName")).sendKeys("vendedor02");
            driver.findElement(By.id("password")).sendKeys("123456");

            clicar(By.cssSelector("button[type='submit']"));

            wait.until(ExpectedConditions.urlContains("/usuario"));

            driver.get(BASE_URL + "/usuario");

            WebElement botaoEditar = driver.findElement(
                    By.xpath("//td[contains(text(),'vendedor02')]/..//a[contains(@href, 'editar')]")
            );
            clicar(botaoEditar);

            Select selectGrupo = new Select(driver.findElement(By.id("grupo")));
            selectGrupo.selectByVisibleText("Vendedor");

            clicar(By.id("btn-add-grupo"));

            Thread.sleep(1000);

            WebElement tabelaGrupos = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("tabela-grupos-usuario"))
            );
            assertTrue(tabelaGrupos.getText().contains("Vendedor"));
            avaliarUsabilidade();

        } catch (Exception e) {
            System.out.println("Erro ao testar grupo: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("RF-S.02 - Usuário sem permissão não deve acessar área restrita")
    public void testAcessoRestritoPorGrupo() throws InterruptedException {
        driver.get(BASE_URL + "/logout");
        Thread.sleep(1000);

        realizarLogin("vendedor02", "123456");

        driver.get(BASE_URL + "/relatorios/financeiro");

        String pageTitle = driver.getTitle();
        boolean acessoNegado =
                pageTitle.contains("403")
                        || driver.getCurrentUrl().contains("login")
                        || driver.getPageSource().contains("Acesso Negado");

        assertTrue(acessoNegado);
        avaliarUsabilidade();
    }
}
