package net.originmobi.pdv.sistema;

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
import net.originmobi.pdv.selenium.pages.produto.ProdutoFormPage;
import net.originmobi.pdv.selenium.pages.produto.ProdutoListPage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProdutoSistemaTest {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static LoginPage loginPage;
    private static ProdutoListPage produtoListPage;
    private static ProdutoFormPage produtoFormPage;
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
        produtoListPage = new ProdutoListPage(driver, wait);
        produtoFormPage = new ProdutoFormPage(driver, wait);

        loginPage.navigateTo(BASE_URL + "/login");
        assertTrue(loginPage.isLoginPageDisplayed(), "Página de Login não foi exibida.");
        loginPage.login("gerente", "123");
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @Order(1)
    public void testF_PROD_001_CadastroProdutoSucesso() {
        driver.get(BASE_URL + "/produto/form");
        assertTrue(produtoFormPage.isPageLoaded(), "Formulário de produto não foi carregado.");

        produtoFormPage.cadastrarProdutoCompleto("REFRIGERANTE COLA 2L", "5,00", "8,00");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        driver.get(BASE_URL + "/produto");
        assertTrue(produtoListPage.isPageLoaded(), "Página de listagem não foi carregada.");
        produtoListPage.buscarProduto("REFRIGERANTE COLA 2L");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(produtoListPage.produtoEncontrado("REFRIGERANTE COLA 2L"),
                "Produto cadastrado não foi encontrado na busca.");
        assertTrue(produtoListPage.valorEncontrado("R$ 8,00"),
                "Valor de venda não foi encontrado na listagem.");
    }

    @Test
    @Order(2)
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
                "Deveria exibir erro ou permanecer no formulário ao cadastrar produto com prejuízo.");
    }

    @Test
    @Order(3)
    public void testF_PROD_ERR_003_CamposObrigatorios() {
        driver.get(BASE_URL + "/produto/form");
        assertTrue(produtoFormPage.isPageLoaded(), "Formulário de produto não foi carregado.");

        produtoFormPage.preencherValorCusto("10,00");
        produtoFormPage.preencherValorVenda("20,00");
        produtoFormPage.submeterFormulario();

        assertTrue(driver.getCurrentUrl().contains("/produto"),
                "Deveria permanecer na página de produto ao submeter formulário incompleto.");
    }

    @Test
    @Order(4)
    public void testF_PROD_004_ConsultaProduto() {
        driver.get(BASE_URL + "/produto");
        assertTrue(produtoListPage.isPageLoaded(), "Página de listagem não foi carregada.");

        produtoListPage.buscarProduto("COLA");
        assertTrue(produtoListPage.produtoEncontrado("REFRIGERANTE COLA 2L"),
                "Deveria encontrar o produto cadastrado no teste 1.");
    }

    @Test
    @Order(5)
    public void testF_PROD_005_AtualizacaoProduto() {
        driver.get(BASE_URL + "/produto");
        assertTrue(produtoListPage.isPageLoaded(), "Página de listagem não foi carregada.");

        produtoListPage.buscarProduto("COLA");
        assertTrue(produtoListPage.produtoEncontrado("REFRIGERANTE COLA 2L"),
                "Produto deve existir antes da edição.");

        produtoListPage.clicarEditar();
        assertTrue(produtoFormPage.codigoPreenchido(),
                "Formulário de edição não foi carregado corretamente.");

        produtoFormPage.preencherValorVenda("8,50");
        produtoFormPage.submeterFormulario();

        assertTrue(produtoFormPage.mensagemSucessoExibida(),
                "Mensagem de sucesso não foi exibida após atualização.");

        driver.get(BASE_URL + "/produto");
        produtoListPage.buscarProduto("COLA");
        assertTrue(produtoListPage.valorEncontrado("8,50"),
                "Valor de venda não foi atualizado na listagem.");
    }

    @Test
    @Order(6)
    public void testNF_SEG_001_AcessoNegadoSemLogin() {
        driver.manage().deleteAllCookies();

        driver.get(BASE_URL + "/produto/form");

        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Falha de Segurança: Usuário não autenticado acessou página protegida!");

        assertTrue(loginPage.isLoginPageDisplayed(),
                "Página de login não foi exibida após tentativa de acesso sem autenticação.");

        loginPage.login("gerente", "123");
    }

    @Test
    @Order(7)
    public void testNF_PERF_001_TempoRespostaBusca() {
        driver.get(BASE_URL + "/produto");
        assertTrue(produtoListPage.isPageLoaded(), "Página de listagem não foi carregada.");

        long inicio = System.currentTimeMillis();

        produtoListPage.buscarProduto("COLA");

        long fim = System.currentTimeMillis();
        long tempoTotal = fim - inicio;

        System.out.println("Tempo de resposta da busca: " + tempoTotal + "ms");

        assertTrue(tempoTotal < 2000,
                "Falha de Desempenho: Busca demorou mais de 2 segundos (" + tempoTotal + "ms)");

        assertTrue(produtoListPage.produtoEncontrado("REFRIGERANTE COLA 2L"),
                "Produto deveria ser encontrado na busca de performance.");
    }
}