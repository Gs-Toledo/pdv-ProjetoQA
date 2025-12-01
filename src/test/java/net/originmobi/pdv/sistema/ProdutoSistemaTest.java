package net.originmobi.pdv.sistema;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;
import net.originmobi.pdv.selenium.pages.login.LoginPage;
import net.originmobi.pdv.selenium.pages.produto.ProdutoFormPage;
import net.originmobi.pdv.selenium.pages.produto.ProdutoListPage;

import java.time.Duration;

public class ProdutoSistemaTest {

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

        driver = WebDriverManager.chromedriver().create();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.manage().window().maximize();

        loginPage = new LoginPage(driver, wait);
        produtoListPage = new ProdutoListPage(driver, wait);
        produtoFormPage = new ProdutoFormPage(driver, wait);

        driver.get(BASE_URL + "/login");
        WebElement userField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("user")));
        WebElement passField = driver.findElement(By.id("password"));
        WebElement btnLogin = driver.findElement(By.id("btn-login"));

        userField.sendKeys("gerente");
        passField.sendKeys("123");
        btnLogin.click();

        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/login")));
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testF_PROD_001_CadastroProdutoSucesso() {
        driver.get(BASE_URL + "/produto/form");
        assertTrue(produtoFormPage.isPageLoaded(), "Formulário de produto não foi carregado.");

        produtoFormPage.cadastrarProdutoCompleto("REFRIGERANTE COLA 2L", "5,00", "8,00");

        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("/produto"),
                ExpectedConditions.presenceOfElementLocated(By.className("alert-success"))
        ));

        driver.get(BASE_URL + "/produto");
        assertTrue(produtoListPage.isPageLoaded(), "Página de listagem não foi carregada.");

        produtoListPage.buscarProduto("REFRIGERANTE COLA 2L");

        assertTrue(produtoListPage.produtoEncontrado("REFRIGERANTE COLA 2L"),
                "Produto cadastrado não foi encontrado na busca.");
        assertTrue(produtoListPage.valorEncontrado("R$ 8,00"),
                "Valor de venda não foi encontrado na listagem.");
    }

    @Test
    public void testF_PROD_ERR_002_PrecoVendaMenorQueCusto() {
        driver.get(BASE_URL + "/produto/form");
        assertTrue(produtoFormPage.isPageLoaded(), "Formulário de produto não foi carregado.");

        produtoFormPage.preencherDescricao("PRODUTO PREJUÍZO");
        produtoFormPage.selecionarFornecedor(1);
        produtoFormPage.selecionarCategoria(1);
        produtoFormPage.selecionarGrupo(1);
        produtoFormPage.preencherValorCusto("10,00");
        produtoFormPage.preencherValorVenda("9,00");
        produtoFormPage.preencherUnidade("UN");
        produtoFormPage.submeterFormulario();

        boolean erroExibido = produtoFormPage.mensagemErroExibida();
        boolean permaneceuNoForm = driver.getCurrentUrl().contains("/form");

        assertTrue(erroExibido || permaneceuNoForm,
                "Deveria exibir erro ou permanecer no formulário.");
    }

    @Test
    public void testF_PROD_ERR_003_CamposObrigatorios() {
        driver.get(BASE_URL + "/produto/form");
        assertTrue(produtoFormPage.isPageLoaded(), "Formulário de produto não foi carregado.");

        produtoFormPage.preencherValorCusto("10,00");
        produtoFormPage.preencherValorVenda("20,00");
        produtoFormPage.submeterFormulario();

        assertTrue(driver.getCurrentUrl().contains("/produto"),
                "Deveria permanecer na URL /produto ao submeter formulário incompleto.");
    }

    @Test
    public void testF_PROD_004_ConsultaProduto() {
        driver.get(BASE_URL + "/produto/form");
        produtoFormPage.cadastrarProdutoCompleto("REFRIGERANTE COLA 2L", "5,00", "8,00");
        wait.until(ExpectedConditions.urlContains("/produto"));

        driver.get(BASE_URL + "/produto");
        assertTrue(produtoListPage.isPageLoaded(), "Página de listagem não foi carregada.");

        produtoListPage.buscarProduto("COLA");
        assertTrue(produtoListPage.produtoEncontrado("REFRIGERANTE COLA 2L"),
                "Deveria encontrar o produto cadastrado.");
    }

    @Test
    public void testF_PROD_005_AtualizacaoProduto() {
        driver.get(BASE_URL + "/produto/form");
        produtoFormPage.cadastrarProdutoCompleto("REFRIGERANTE COLA 2L", "5,00", "8,00");
        wait.until(ExpectedConditions.urlContains("/produto"));

        driver.get(BASE_URL + "/produto");
        assertTrue(produtoListPage.isPageLoaded(), "Página de listagem não foi carregada.");

        produtoListPage.buscarProduto("COLA");
        assertTrue(produtoListPage.produtoEncontrado("REFRIGERANTE COLA 2L"),
                "Produto deve existir antes da edição.");

        produtoListPage.clicarEditar();
        assertTrue(produtoFormPage.codigoPreenchido(),
                "Formulário de edição não foi carregado.");

        produtoFormPage.preencherValorVenda("8,50");
        produtoFormPage.submeterFormulario();

        assertTrue(produtoFormPage.mensagemSucessoExibida(),
                "Mensagem de sucesso não foi exibida.");

        driver.get(BASE_URL + "/produto");
        produtoListPage.buscarProduto("COLA");
        assertTrue(produtoListPage.valorEncontrado("8,50"),
                "Valor não foi atualizado.");
    }

    @Test
    public void testNF_SEG_001_AcessoNegadoSemLogin() {
        driver.manage().deleteAllCookies();
        driver.get(BASE_URL + "/produto/form");

        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Falha de Segurança: Usuário não autenticado acessou página protegida!");
        assertTrue(loginPage.isLoginPageDisplayed(),
                "Página de login não foi exibida.");

        loginPage.login("gerente", "123");
    }

    @Test
    public void testNF_PERF_001_TempoRespostaBusca() {
        driver.get(BASE_URL + "/produto/form");
        produtoFormPage.cadastrarProdutoCompleto("REFRIGERANTE COLA 2L", "5,00", "8,00");
        wait.until(ExpectedConditions.urlContains("/produto"));

        driver.get(BASE_URL + "/produto");
        assertTrue(produtoListPage.isPageLoaded(), "Página de listagem não foi carregada.");

        long inicio = System.currentTimeMillis();
        produtoListPage.buscarProduto("COLA");
        long fim = System.currentTimeMillis();
        long tempoTotal = fim - inicio;

        System.out.println("Tempo de resposta da busca: " + tempoTotal + "ms");

        assertTrue(tempoTotal < 2000,
                "Falha de Desempenho: Busca demorou " + tempoTotal + "ms");
        assertTrue(produtoListPage.produtoEncontrado("REFRIGERANTE COLA 2L"),
                "Produto deveria ser encontrado.");
    }
}