package net.originmobi.pdv.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;
import net.originmobi.pdv.selenium.pages.login.LoginPage;
import net.originmobi.pdv.selenium.pages.notafiscal.NotaFiscalPage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NotaFiscalPerformanceTest {

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
        options.addArguments("--headless");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        loginPage = new LoginPage(driver, wait);
        notaPage = new NotaFiscalPage(driver, wait);

        loginPage.navigateTo(BASE_URL + "/login");
        loginPage.login("gerente", "123");
    }

    // O sistema deve processar a criação de uma nota em menos de 2 segundos

    @Test
    @Order(1)
    public void testDesempenhoCriacaoNotaUnica() {
        notaPage.visitarListagem();
        notaPage.irParaNovaNota();
        notaPage.preencherNatureza("Venda Teste Desempenho");
        notaPage.selecionarDestinatario("João Rafael Mendes Nogueira");

        long inicio = System.currentTimeMillis();

        notaPage.clicarCriarNota();

        wait.until(d -> d.getCurrentUrl().contains("/notafiscal/"));

        long fim = System.currentTimeMillis();
        long duracao = fim - inicio;

        System.out.println("Tempo de resposta (Criação Nota): " + duracao + "ms");

        long slaMaximo = 2000;
        assertTrue(duracao < slaMaximo,
                "Performance ruim! A criação demorou " + duracao + "ms, o limite era " + slaMaximo + "ms.");
    }

    // Simula um usuário criando várias notas em sequência para verificar se o
    // sistema engasga.
    @Test
    @Order(2)
    public void testEstabilidadeCriacaoEmLote() {
        int quantidadeNotas = 5;
        long somaTempos = 0;

        for (int i = 1; i <= quantidadeNotas; i++) {
            notaPage.visitarListagem();
            notaPage.irParaNovaNota();
            notaPage.preencherNatureza("Venda Lote " + i);
            notaPage.selecionarDestinatario("João Rafael Mendes Nogueira");

            long inicio = System.currentTimeMillis();

            notaPage.clicarCriarNota();
            wait.until(d -> d.getCurrentUrl().contains("/notafiscal/"));

            long fim = System.currentTimeMillis();
            long duracao = fim - inicio;

            System.out.println("Nota " + i + " criada em: " + duracao + "ms");
            somaTempos += duracao;
        }

        long media = somaTempos / quantidadeNotas;
        System.out.println("Tempo médio de criação em lote: " + media + "ms");

        assertTrue(media < 2500, "A média de tempo de criação em lote está muito alta.");
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}