package net.originmobi.pdv.selenium.pages.produto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ProdutoFormPage {

    private WebDriver driver;
    private WebDriverWait wait;
    private By formProduto = By.id("form_produto");
    private By descricaoField = By.id("descricao");
    private By fornecedorSelect = By.id("fornecedor");
    private By categoriaSelect = By.id("categoria");
    private By grupoSelect = By.id("grupo");
    private By valorCustoField = By.id("valorCusto");
    private By valorVendaField = By.id("valorVenda");
    private By unidadeField = By.id("unidade");
    private By balancaSelect = By.id("balanca");
    private By ativoSelect = By.id("ativo");
    private By controlaEstoqueSelect = By.name("controla_estoque");
    private By vendavelSelect = By.id("vendavel");
    private By stSelect = By.id("st");
    private By ncmField = By.id("ncm");
    private By cestField = By.id("cest");
    private By tributacaoSelect = By.id("tributacao");
    private By modBcSelect = By.id("modbc");
    private By codigoField = By.id("codigo");
    private By btnEnviar = By.name("enviar");
    private By mensagemSucesso = By.className("alert-success");
    private By mensagemErro = By.className("alert-danger");

    public ProdutoFormPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(formProduto));
            wait.until(ExpectedConditions.presenceOfElementLocated(fornecedorSelect));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void preencherDescricao(String descricao) {
        driver.findElement(descricaoField).sendKeys(descricao);
    }

    public void selecionarFornecedor(int indice) {
        selecionarOpcaoSegura(fornecedorSelect, indice);
    }

    public void selecionarCategoria(int indice) {
        selecionarOpcaoSegura(categoriaSelect, indice);
    }

    public void selecionarGrupo(int indice) {
        selecionarOpcaoSegura(grupoSelect, indice);
    }

    public void selecionarTributacao(int indice) {
        selecionarOpcaoSegura(tributacaoSelect, indice);
    }

    public void selecionarModalidadeBC(int indice) {
        selecionarOpcaoSegura(modBcSelect, indice);
    }

    public void preencherValorCusto(String valor) {
        driver.findElement(valorCustoField).sendKeys(valor);
    }

    public void preencherValorVenda(String valor) {
        WebElement campo = driver.findElement(valorVendaField);
        campo.clear();
        campo.sendKeys(valor);
    }

    public void preencherUnidade(String unidade) {
        driver.findElement(unidadeField).sendKeys(unidade);
    }

    public void preencherNCM(String ncm) {
        driver.findElement(ncmField).sendKeys(ncm);
    }

    public void preencherCEST(String cest) {
        driver.findElement(cestField).sendKeys(cest);
    }

    public void configurarBalanca(String opcao) {
        new Select(driver.findElement(balancaSelect)).selectByVisibleText(opcao);
    }

    public void configurarAtivo(String opcao) {
        new Select(driver.findElement(ativoSelect)).selectByVisibleText(opcao);
    }

    public void configurarControlaEstoque(String opcao) {
        new Select(driver.findElement(controlaEstoqueSelect)).selectByVisibleText(opcao);
    }

    public void configurarVendavel(String opcao) {
        new Select(driver.findElement(vendavelSelect)).selectByVisibleText(opcao);
    }

    public void configurarST(int indice) {
        selecionarOpcaoSegura(stSelect, indice);
    }

    public void submeterFormulario() {
        driver.findElement(btnEnviar).click();
    }

    public boolean mensagemSucessoExibida() {
        try {
            WebElement mensagem = wait.until(ExpectedConditions.visibilityOfElementLocated(mensagemSucesso));
            return mensagem.getText().contains("sucesso");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean mensagemErroExibida() {
        try {
            WebElement erro = driver.findElement(mensagemErro);
            return erro.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean codigoPreenchido() {
        try {
            wait.until(ExpectedConditions.attributeToBeNotEmpty(driver.findElement(codigoField), "value"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void cadastrarProdutoCompleto(String descricao, String custo, String venda) {
        preencherDescricao(descricao);
        selecionarFornecedor(1);
        selecionarCategoria(1);
        selecionarGrupo(1);
        preencherValorCusto(custo);
        preencherValorVenda(venda);
        preencherUnidade("UN");
        configurarBalanca("NAO");
        configurarAtivo("ATIVO");
        configurarControlaEstoque("SIM");
        configurarVendavel("SIM");
        configurarST(1);
        preencherNCM("22021000");
        preencherCEST("0300100");
        selecionarTributacao(1);
        selecionarModalidadeBC(1);
        submeterFormulario();

        try {
            wait.until(driver ->
                    driver.getCurrentUrl().contains("/produto") ||
                            mensagemSucessoExibida()
            );
        } catch (Exception e) {
            System.out.println("Aviso: Time-out aguardando confirmação de cadastro.");
        }
    }

    private void selecionarOpcaoSegura(By elementLocator, int indicePreferencial) {
        WebElement element = driver.findElement(elementLocator);
        Select select = new Select(element);
        int tamanho = select.getOptions().size();

        if (tamanho <= indicePreferencial) {
            if (tamanho > 0) {
                select.selectByIndex(0);
                System.out.println("Aviso: Dropdown " + elementLocator + " sem índice " + indicePreferencial + ". Usando índice 0.");
            } else {
                throw new RuntimeException("ERRO DE DADOS: O Dropdown " + elementLocator + " está totalmente vazio!");
            }
        } else {
            select.selectByIndex(indicePreferencial);
        }
    }
}