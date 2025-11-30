package net.originmobi.pdv.selenium.pages.produto;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ProdutoListPage {

    private WebDriver driver;
    private WebDriverWait wait;
    private By pageHeader = By.xpath("//h1[contains(text(),'Produto')]");
    private By novoProdutoButton = By.xpath("//div[@id='btn-padrao']/a[contains(text(),'Novo')]");
    private By campoBusca = By.name("descricao");
    private By btnBuscar = By.xpath("//button[@type='submit']");
    private By tabelaProdutos = By.tagName("table");
    private By corpoTabela = By.tagName("tbody");
    private By btnEditar = By.className("glyphicon-pencil");

    public ProdutoListPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public boolean isPageLoaded() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(pageHeader)).isDisplayed()
                    || wait.until(ExpectedConditions.visibilityOfElementLocated(novoProdutoButton)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void buscarProduto(String descricao) {
        WebElement campo = wait.until(ExpectedConditions.visibilityOfElementLocated(campoBusca));
        campo.clear();
        campo.sendKeys(descricao);
        driver.findElement(btnBuscar).click();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean produtoEncontrado(String descricao) {
        try {
            WebElement tabela = wait.until(ExpectedConditions.presenceOfElementLocated(tabelaProdutos));

            if (tabela == null || tabela.getText().isEmpty()) {
                tabela = wait.until(ExpectedConditions.presenceOfElementLocated(corpoTabela));
            }

            String textoTabela = tabela.getText();
            System.out.println("=== CONTEÚDO DA TABELA ===");
            System.out.println(textoTabela);
            System.out.println("==========================");

            return textoTabela.contains(descricao);
        } catch (Exception e) {
            System.out.println("Erro ao buscar produto: " + e.getMessage());
            System.out.println("=== HTML DA PÁGINA ===");
            System.out.println(driver.getPageSource());
            System.out.println("======================");
            return false;
        }
    }

    public boolean valorEncontrado(String valor) {
        try {
            WebElement tabela = wait.until(ExpectedConditions.visibilityOfElementLocated(corpoTabela));
            return tabela.getText().contains(valor);
        } catch (Exception e) {
            return false;
        }
    }

    public void clicarEditar() {
        WebElement btnEdit = wait.until(ExpectedConditions.elementToBeClickable(btnEditar));
        btnEdit.click();
    }

    public void clickNovoProduto() {
        wait.until(ExpectedConditions.elementToBeClickable(novoProdutoButton)).click();
    }
}