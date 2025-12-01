package net.originmobi.pdv.sistema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;
import net.originmobi.pdv.selenium.pages.login.LoginPage;
import net.originmobi.pdv.selenium.pages.produto.ProdutoFormPage;
import net.originmobi.pdv.selenium.pages.produto.ProdutoListPage;
import org.junit.jupiter.api.Assertions;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FornecedorSistemaTest {


    private WebDriver driver;
    private WebDriverWait wait;
    private LoginPage loginPage;
    private ProdutoListPage produtoListPage;
    private ProdutoFormPage produtoFormPage;

    private static final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");

        driver = WebDriverManager.chromedriver()
                .capabilities(options)
                .create();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();

        loginPage = new LoginPage(driver, wait);
        produtoListPage = new ProdutoListPage(driver, wait);
        produtoFormPage = new ProdutoFormPage(driver, wait);

        driver.get(BASE_URL + "/login");

        WebElement userField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("user")));
        userField.sendKeys("gerente");
        driver.findElement(By.id("password")).sendKeys("123");
        driver.findElement(By.id("btn-login")).click();

        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
    }

    @AfterEach
    public void tearDown() {
    try {
        if (driver != null) {
            driver.quit();
        }
    } catch (Exception e) {
        System.out.println("Erro ao fechar o driver, ignorado para não quebrar a suíte: " + e.getMessage());
    }
}
    
    @Test
    @DisplayName("Fornecedor - Página de lista carrega corretamente")
    public void testListaFornecedorCarrega() {

        driver.get(BASE_URL + "/fornecedor");

        WebElement tabela = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("tabela-fornecedores")
                )
        );

        assertTrue(tabela.isDisplayed());
    }

   @Test
@DisplayName("Fornecedor - Cadastro completo de fornecedor")
public void testCadastroFornecedor() {

    driver.get(BASE_URL + "/fornecedor");

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("tabela-fornecedores")
    ));

    WebElement btnNovo = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("btn-novo-fornecedor"))
    );
    btnNovo.click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("form_fornecedor") 
    ));

    driver.findElement(By.id("nomefantasia")).sendKeys("Fornecedor Teste Selenium LTDA");
    driver.findElement(By.id("nome")).sendKeys("Fornecedor Teste");
    driver.findElement(By.id("cnpj")).sendKeys("76.256.662/0001-99");
    driver.findElement(By.id("escricao")).sendKeys("1234567");
    driver.findElement(By.id("situacao")).sendKeys("Ativo");

    driver.findElement(By.xpath("//a[@href='#menu1']")).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("rua")));
    driver.findElement(By.id("cidade")).sendKeys("Seringueiras");
    driver.findElement(By.id("rua")).sendKeys("Rua Teste");
    driver.findElement(By.id("bairro")).sendKeys("Centro");
    driver.findElement(By.id("numero")).sendKeys("123");
    driver.findElement(By.id("cep")).sendKeys("24000-000");

    driver.findElement(By.xpath("//a[@href='#menu2']")).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fone")));
    driver.findElement(By.id("fone")).sendKeys("21999990000");

    driver.findElement(By.id("btn-salvar-fornecedor")).click();


    WebElement msg = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'sucesso') or contains(text(),'Sucesso')]")
    ));
    assertTrue(msg.isDisplayed(), "Mensagem de sucesso não apareceu!");

    assertTrue(driver.findElement(By.id("nome")).getAttribute("value").isEmpty(),
            "Campo 'nome' deveria ter sido limpo após salvar");
}

@Test
@DisplayName("Fornecedor - Atualização de fornecedor existente")
public void testAtualizacaoFornecedor() {

    driver.get(BASE_URL + "/fornecedor");

    // Aguarda a tabela carregar
    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("tabela-fornecedores")
    ));

    // Clica no ícone de editar (lápis)
    WebElement btnEditar = wait.until(
            ExpectedConditions.elementToBeClickable(
                    By.xpath("//table[@id='tabela-fornecedores']//tr[2]//a")
            )
    );
    btnEditar.click();

    // Aguarda o formulário
    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("form_fornecedor")
    ));

    // ===== ALTERA NOME FANTASIA =====
    WebElement nomeFantasia = driver.findElement(By.id("nomefantasia"));
    nomeFantasia.clear();
    nomeFantasia.sendKeys("Alpha Teste Atualizado");

    // ===== ALTERA TELEFONE =====
    driver.findElement(By.xpath("//a[@href='#menu2']")).click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fone")));

    WebElement telefone = driver.findElement(By.id("fone"));
    telefone.clear();
    telefone.sendKeys("11999990000");

    // ===== SALVA =====
    driver.findElement(By.id("btn-salvar-fornecedor")).click();

    // ===== VALIDA MENSAGEM DE SUCESSO =====
    WebElement msg = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(text(),'sucesso') or contains(text(),'Sucesso')]")
    ));

    Assertions.assertTrue(msg.isDisplayed(), "Mensagem de sucesso não apareceu!");

    // ===== VOLTA PARA LISTA =====
    driver.get(BASE_URL + "/fornecedor");

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("tabela-fornecedores")
    ));

    // ===== VALIDA SE O NOME FOI ATUALIZADO NA LISTA =====
    WebElement nomeAtualizado = driver.findElement(
            By.xpath("//*[contains(text(),'Alpha Teste Atualizado')]")
    );

    Assertions.assertTrue(nomeAtualizado.isDisplayed(),
            "Fornecedor não foi atualizado corretamente!");
}

@Test
@DisplayName("Fornecedor - CNPJ duplicado gera erro e não salva")
public void testCadastroFornecedorCnpjDuplicado() {

    driver.get(BASE_URL + "/fornecedor");

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("tabela-fornecedores")
    ));

    WebElement btnNovo = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("btn-novo-fornecedor"))
    );
    btnNovo.click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("form_fornecedor")
    ));

    driver.findElement(By.id("nomefantasia")).sendKeys("FORNECEDOR DUPLICADO");
    driver.findElement(By.id("nome")).sendKeys("Fornecedor Duplicado");
    driver.findElement(By.id("cnpj")).sendKeys("76.256.662/0001-99");
    driver.findElement(By.id("escricao")).sendKeys("9999999");
    driver.findElement(By.id("situacao")).sendKeys("Ativo");

    driver.findElement(By.xpath("//a[@href='#menu1']")).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("rua")));
    driver.findElement(By.id("cidade")).sendKeys("Seringueiras");
    driver.findElement(By.id("rua")).sendKeys("Rua Duplicada");
    driver.findElement(By.id("bairro")).sendKeys("Centro");
    driver.findElement(By.id("numero")).sendKeys("999");
    driver.findElement(By.id("cep")).sendKeys("24000-000");

    driver.findElement(By.xpath("//a[@href='#menu2']")).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fone")));
    driver.findElement(By.id("fone")).sendKeys("21988887777");

    driver.findElement(By.id("btn-salvar-fornecedor")).click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.tagName("body")
    ));

    Assertions.assertTrue(
            driver.getCurrentUrl().contains("/fornecedor")
    );

    Assertions.assertTrue(
            driver.getPageSource().contains("There was an unexpected error")
                    || driver.getPageSource().contains("Whitelabel Error Page")
    );

    driver.get(BASE_URL + "/fornecedor");

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("tabela-fornecedores")
    ));

    Assertions.assertTrue(
            driver.findElements(By.xpath("//*[contains(text(),'FORNECEDOR DUPLICADO')]")).isEmpty()
    );
}

@Test
@DisplayName("Fornecedor - Consulta por filtro parcial (Alpha)")
public void testConsultaFornecedorPorFiltro() {

    driver.get(BASE_URL + "/fornecedor");

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("tabela-fornecedores")
    ));

    WebElement campoFiltro = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                    By.id("input-busca-fornecedor")
            )
    );

    campoFiltro.clear();
    campoFiltro.sendKeys("Alpha");

    driver.findElement(By.id("btn-buscar-fornecedor")).click();

    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.id("tabela-fornecedores")
    ));

    WebElement fornecedorFiltrado = driver.findElement(
            By.xpath("//*[contains(text(),'Alpha')]")
    );

    Assertions.assertTrue(fornecedorFiltrado.isDisplayed());
}







    
}
 