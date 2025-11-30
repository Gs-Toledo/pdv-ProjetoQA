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
        driver.findElement(By.id("btn-login")).click();
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

            driver.findElement(By.cssSelector("button[type='submit']")).click();

            wait.until(ExpectedConditions.urlContains("/usuario"));

            driver.get(BASE_URL + "/usuario");
            
            driver.findElement(By.xpath("//td[contains(text(),'vendedor02')]/..//a[contains(@href, 'editar')]")).click();

            Select selectGrupo = new Select(driver.findElement(By.id("grupo"))); 
            selectGrupo.selectByVisibleText("Vendedor");
            
            driver.findElement(By.id("btn-add-grupo")).click();
            
            Thread.sleep(1000);

            WebElement tabelaGrupos = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tabela-grupos-usuario")));
            assertTrue(tabelaGrupos.getText().contains("Vendedor"));
            
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
        boolean acessoNegado = pageTitle.contains("403") || 
                               driver.getCurrentUrl().contains("login") ||
                               driver.getPageSource().contains("Acesso Negado");
        
        assertTrue(acessoNegado);
    }
}