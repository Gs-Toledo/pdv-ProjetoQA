package net.originmobi.pdv.sistema;

import net.originmobi.pdv.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.By;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;

public class VendaSistemaTest {
    protected WebDriver driver;
    protected WebDriverWait wait;

    private static final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    public void setup() {
        driver = WebDriverManager.chromedriver().create();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();


        driver.get(BASE_URL + "/login");

        WebElement userField = driver.findElement(By.id("user"));
        WebElement passField = driver.findElement(By.id("password"));
        WebElement btnLogin = driver.findElement(By.id("btn-login"));

        userField.sendKeys("gerente");
        passField.sendKeys("123");
        btnLogin.click();
        wait.until(ExpectedConditions.urlContains("/"));

        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));

        criacaoTitulo();
        aberturaCaixa();

        driver.get(BASE_URL + "/venda/status/ABERTA");
        WebElement tituloPedidos = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1.titulo-h1")));
        assertTrue(tituloPedidos.isDisplayed(), "Página de venda aberta não foi exibida corretamente!");
    }

    @Test
    public void criaEFechaVenda() throws InterruptedException {
//      RF-V.01: Início de Venda
        WebElement botaoNovoPedido = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Novo Pedido")));
        botaoNovoPedido.click();
        Thread.sleep(2000);

        WebElement selectCliente = wait.until(ExpectedConditions.elementToBeClickable(By.id("cliente")));
        Select dropdown = new Select(selectCliente);
        dropdown.selectByIndex(1);
        Thread.sleep(2000);

        WebElement campoObservacao = wait.until(ExpectedConditions.elementToBeClickable(By.id("observacao")));
        campoObservacao.clear();
        campoObservacao.sendKeys("teste selenium");
        Thread.sleep(2000);

        WebElement botaoSalvar = wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-salva")));
        botaoSalvar.click();
        Thread.sleep(3000);

//      RF-V.02: Adição de Itens
        WebElement botaoDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-id='codigoProduto']")));
        botaoDropdown.click();
        Thread.sleep(2000);

        try {
            WebElement opcaoPicole = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@data-normalized-text, 'Picolé')]")));

            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", opcaoPicole);
        } catch (Exception e) {
            WebElement selectOriginal = driver.findElement(By.id("codigoProduto"));
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].selectedIndex = 1; arguments[0].dispatchEvent(new Event('change'));", selectOriginal);
        }
        Thread.sleep(2000);

        WebElement botaoInserir = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".js-addvenda-produto")));
        botaoInserir.click();
        Thread.sleep(3000);

        WebElement botaoGerarVenda = wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-venda")));
        botaoGerarVenda.click();
        Thread.sleep(2000);

        WebElement selectFormaPagamento = wait.until(ExpectedConditions.elementToBeClickable(By.id("pagamento")));
        Select dropdownPagamento = new Select(selectFormaPagamento);
        dropdownPagamento.selectByIndex(1);
        Thread.sleep(2000);

//      RF-V.03: Aplicação de Descontos
        WebElement campoDesconto = wait.until(ExpectedConditions.elementToBeClickable(By.id("desconto")));
        campoDesconto.clear();
        campoDesconto.sendKeys("1");
        Thread.sleep(2000);

        WebElement campoAcrescimo = wait.until(ExpectedConditions.elementToBeClickable(By.id("acrescimo")));
        campoAcrescimo.clear();
        campoAcrescimo.sendKeys("1");
        Thread.sleep(2000);

//      RF-V.04: Finalização e Pagamento
        WebElement botaoPagar = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-pagamento")));
        botaoPagar.click();

        try {
            wait.until(ExpectedConditions.alertIsPresent());

            org.openqa.selenium.Alert alert = driver.switchTo().alert();

            String textoAlerta = alert.getText();

            assertTrue(textoAlerta.contains("Venda finalizada com sucesso"),
                    "O texto do alerta está incorreto. Texto atual: " + textoAlerta);

            alert.accept();

        } catch (Exception e) {
            System.out.println("Erro ao manipular o alerta: " + e.getMessage());
            throw e;
        }
    }

    // RNF-01: Desempenho do Processamento de Venda
    @Test
    public void testeDesempenhoFechamentoVenda() {

        WebElement botaoNovoPedido = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Novo Pedido")));
        botaoNovoPedido.click();

        WebElement selectCliente = wait.until(ExpectedConditions.elementToBeClickable(By.id("cliente")));
        new Select(selectCliente).selectByIndex(1);

        WebElement botaoSalvar = wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-salva")));
        botaoSalvar.click();

        WebElement botaoDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-id='codigoProduto']")));
        botaoDropdown.click();

        WebElement selectOriginal = driver.findElement(By.id("codigoProduto"));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].selectedIndex = 1; arguments[0].dispatchEvent(new Event('change'));", selectOriginal);

        WebElement botaoInserir = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".js-addvenda-produto")));
        botaoInserir.click();

        WebElement botaoGerarVenda = wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-venda")));
        botaoGerarVenda.click();

        WebElement selectFormaPagamento = wait.until(ExpectedConditions.elementToBeClickable(By.id("pagamento")));
        new Select(selectFormaPagamento).selectByIndex(1);

        WebElement botaoPagar = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-pagamento")));


        long tempoInicio = System.currentTimeMillis();

        botaoPagar.click();

        wait.until(ExpectedConditions.alertIsPresent());

        long tempoFim = System.currentTimeMillis();

        long duracaoProcessamento = tempoFim - tempoInicio;

        System.out.println("Tempo de processamento da venda: " + duracaoProcessamento + "ms");

        driver.switchTo().alert().accept();

        long slaMaximo = 3000;

        assertTrue(duracaoProcessamento < slaMaximo,
                "Falha de Desempenho: O fechamento da venda demorou " + duracaoProcessamento +
                        "ms, excedendo o limite de " + slaMaximo + "ms.");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void criacaoTitulo() {
        try {
            driver.get(BASE_URL + "/titulos");
            WebElement botaoNovo = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Novo")));
            botaoNovo.click();
            WebElement campoDescricao = wait.until(ExpectedConditions.elementToBeClickable(By.id("descricao")));
            campoDescricao.clear();
            campoDescricao.sendKeys("Teste Selenium");
            WebElement botaoSalvar = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[type='submit'][value='Salvar']")));
            botaoSalvar.click();
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Erro ao criar título: " + e.getMessage());
        }
    }

    private void aberturaCaixa() {
        try {
            driver.get(BASE_URL + "/caixa");
            WebElement botaoAbrirNovo = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Abrir Novo")));
            botaoAbrirNovo.click();
            WebElement campoObservacao = wait.until(ExpectedConditions.elementToBeClickable(By.id("descricao")));
            campoObservacao.clear();
            campoObservacao.sendKeys("Teste Selenium");
            WebElement campoValorAbertura = wait.until(ExpectedConditions.elementToBeClickable(By.id("valorAbertura")));
            campoValorAbertura.clear();
            campoValorAbertura.sendKeys("100");
            WebElement botaoAbrir = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Abrir")));
            botaoAbrir.click();
            WebElement mensagemSucessoCaixa = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".alert-success span")));
            assertTrue(mensagemSucessoCaixa.isDisplayed(), "Caixa não foi aberto com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao abrir caixa: " + e.getMessage());
        }
    }

}