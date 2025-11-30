package net.originmobi.pdv.selenium.pages.notafiscal;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class NotaFiscalPage {

    private WebDriver driver;
    private WebDriverWait wait;

    private By btnNovaNota = By.xpath("//a[contains(@href, '/notafiscal/form')]");

    private By inputNatureza = By.id("natureza");

    private By selectDestinatario = By.name("destinatario");

    private By btnCriarNota = By.className("btn-cria-nota");

    public NotaFiscalPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void visitarListagem() {
        driver.get("http://localhost:8080/notafiscal");
    }

    public void irParaNovaNota() {
        wait.until(ExpectedConditions.elementToBeClickable(btnNovaNota)).click();
    }

    public void preencherNatureza(String texto) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(inputNatureza));
        input.clear();
        input.sendKeys(texto);
    }

    public void selecionarDestinatario(String nomeDestinatario) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(selectDestinatario));

        Select dropdown = new Select(element);

        dropdown.selectByVisibleText(nomeDestinatario);
    }

    public void clicarCriarNota() {
        wait.until(ExpectedConditions.elementToBeClickable(btnCriarNota)).click();
    }

    public boolean isPaginaEdicao() {

        return driver.getCurrentUrl().matches(".*\\/notafiscal\\/\\d+.*");
    }
}