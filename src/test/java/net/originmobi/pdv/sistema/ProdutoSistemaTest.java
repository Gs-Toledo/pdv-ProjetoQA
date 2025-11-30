package net.originmobi.pdv.sistema;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.chrome.ChromeOptions;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProdutoSistemaTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl = "http://localhost:8080";

    @BeforeEach
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        realizarLogin();
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void realizarLogin() {
        driver.get(baseUrl + "/login");

        WebElement userField = driver.findElement(By.id("user"));
        WebElement passField = driver.findElement(By.id("password"));
        WebElement btnLogin = driver.findElement(By.id("btn-login"));

        userField.sendKeys("gerente");
        passField.sendKeys("123");
        btnLogin.click();

        wait.until(ExpectedConditions.urlContains("/"));
    }

    private void navegarParaCadastroProduto() {
        driver.get(baseUrl + "/produto/form");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("form_produto")));
    }

    @Test
    @Order(1)
    public void testF_PROD_001_CadastroProdutoSucesso() {
        navegarParaCadastroProduto();

        driver.findElement(By.id("descricao")).sendKeys("REFRIGERANTE COLA 2L");

        new Select(driver.findElement(By.id("fornecedor"))).selectByIndex(1);
        new Select(driver.findElement(By.id("categoria"))).selectByIndex(1);
        new Select(driver.findElement(By.id("grupo"))).selectByIndex(1);

        driver.findElement(By.id("valorCusto")).sendKeys("5,00");
        driver.findElement(By.id("valorVenda")).sendKeys("8,00");
        driver.findElement(By.id("unidade")).sendKeys("UN");

        new Select(driver.findElement(By.id("balanca"))).selectByVisibleText("NAO");
        new Select(driver.findElement(By.id("ativo"))).selectByVisibleText("ATIVO");
        new Select(driver.findElement(By.name("controla_estoque"))).selectByVisibleText("SIM");
        new Select(driver.findElement(By.id("vendavel"))).selectByVisibleText("SIM");
        new Select(driver.findElement(By.id("st"))).selectByIndex(1);

        driver.findElement(By.id("ncm")).sendKeys("22021000");
        driver.findElement(By.id("cest")).sendKeys("0300100");

        driver.findElement(By.name("enviar")).click();

        WebElement mensagemSucesso = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("alert-success")));
        assertTrue(mensagemSucesso.getText().contains("sucesso"), "Deveria exibir mensagem de sucesso");

        driver.get(baseUrl + "/produto");
        WebElement campoBusca = driver.findElement(By.name("descricao"));
        campoBusca.sendKeys("REFRIGERANTE COLA 2L");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        WebElement tabela = driver.findElement(By.tagName("table"));
        assertTrue(tabela.getText().contains("REFRIGERANTE COLA 2L"));
        assertTrue(tabela.getText().contains("R$ 8,00"));
    }

    @Test
    @Order(2)
    public void testF_PROD_ERR_002_PrecoVendaMenorQueCusto() {
        navegarParaCadastroProduto();

        driver.findElement(By.id("descricao")).sendKeys("PRODUTO PREJUÍZO");
        new Select(driver.findElement(By.id("fornecedor"))).selectByIndex(1);
        new Select(driver.findElement(By.id("categoria"))).selectByIndex(1);
        new Select(driver.findElement(By.id("grupo"))).selectByIndex(1);

        driver.findElement(By.id("valorCusto")).sendKeys("10,00");
        driver.findElement(By.id("valorVenda")).sendKeys("9,00");
        driver.findElement(By.id("unidade")).sendKeys("UN");

        driver.findElement(By.name("enviar")).click();

        try {
            WebElement erro = driver.findElement(By.className("alert-danger"));
            assertTrue(erro.isDisplayed());
        } catch (Exception e) {
            String currentUrl = driver.getCurrentUrl();
            assertTrue(currentUrl.contains("/form"), "Não deveria ter saído do formulário de cadastro");
        }
    }

    @Test
    @Order(3)
    public void testF_PROD_ERR_003_CamposObrigatorios() {
        navegarParaCadastroProduto();

        driver.findElement(By.id("valorCusto")).sendKeys("10,00");
        driver.findElement(By.id("valorVenda")).sendKeys("20,00");

        driver.findElement(By.name("enviar")).click();

        assertTrue(driver.getCurrentUrl().contains("/produto"), "Deveria permanecer na página de produto");

        driver.findElement(By.id("descricao")).sendKeys("AA");
        driver.findElement(By.name("enviar")).click();

    }

    @Test
    @Order(4)
    public void testF_PROD_004_ConsultaProduto() {
        driver.get(baseUrl + "/produto");

        WebElement campoBusca = driver.findElement(By.id("descricao"));
        if(campoBusca == null) campoBusca = driver.findElement(By.name("descricao"));

        campoBusca.clear();
        campoBusca.sendKeys("COLA");

        driver.findElement(By.xpath("//button[@type='submit']")).click();

        WebElement corpoTabela = driver.findElement(By.tagName("tbody"));
        assertTrue(corpoTabela.getText().contains("REFRIGERANTE COLA 2L"), "Deveria encontrar o produto cadastrado");
    }

    @Test
    @Order(5)
    public void testF_PROD_005_AtualizacaoProduto() {
        testF_PROD_004_ConsultaProduto();

        WebElement btnEditar = driver.findElement(By.className("glyphicon-pencil"));
        btnEditar.click();

        wait.until(ExpectedConditions.attributeToBeNotEmpty(driver.findElement(By.id("codigo")), "value"));

        WebElement campoVenda = driver.findElement(By.id("valorVenda"));
        campoVenda.clear();
        campoVenda.sendKeys("8,50");

        driver.findElement(By.name("enviar")).click();

        WebElement mensagemSucesso = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("alert-success")));
        assertTrue(mensagemSucesso.getText().contains("sucesso"));

        testF_PROD_004_ConsultaProduto();
        WebElement corpoTabela = driver.findElement(By.tagName("tbody"));
        assertTrue(corpoTabela.getText().contains("8,50"), "Valor de venda não foi atualizado na listagem");
    }

    @Test
    @Order(6)
    public void testNF_SEG_001_AcessoNegadoSemLogin() {
        driver.manage().deleteAllCookies();

        driver.get(baseUrl + "/produto/form");

        String urlAtual = driver.getCurrentUrl();

        assertTrue(urlAtual.contains("/login"), "Falha de Segurança: Usuário não autenticado acessou página protegida!");

        WebElement painelLogin = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("painelLoginPrimaio")));
        assertTrue(painelLogin.isDisplayed());
    }

    @Test
    @Order(7)
    public void testNF_PERF_001_TempoRespostaBusca() {
        driver.get(baseUrl + "/produto");

        WebElement campoBusca = driver.findElement(By.name("descricao"));
        campoBusca.sendKeys("A");

        WebElement btnBuscar = driver.findElement(By.xpath("//button[@type='submit']"));

        long inicio = System.currentTimeMillis();

        btnBuscar.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("tbody")));

        long fim = System.currentTimeMillis();

        long tempoTotal = fim - inicio;
        System.out.println("Tempo de resposta da busca: " + tempoTotal + "ms");

        assertTrue(tempoTotal < 2000, "Falha de Desempenho: Busca demorou mais de 2 segundos (" + tempoTotal + "ms)");
    }
}