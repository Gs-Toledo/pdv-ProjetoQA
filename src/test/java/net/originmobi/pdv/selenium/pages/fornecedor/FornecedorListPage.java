package net.originmobi.pdv.selenium.pages.fornecedor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class FornecedorListPage {
    private WebDriver driver;

    public FornecedorListPage(WebDriver driver) {
        this.driver = driver;
    }

    public WebElement getBuscaInput() {
        return driver.findElement(By.cssSelector("input[placeholder='Buscar fornecedor']"));
    }

    public void buscarPorNome(String nome) {
        getBuscaInput().clear();
        getBuscaInput().sendKeys(nome);
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    public void abrirCadastro() {
        driver.findElement(By.linkText("Novo")).click();
    }

    public boolean existeFornecedorNaLista(String nome) {
        return driver.getPageSource().contains(nome);
    }

    public void editarFornecedorPorNome(String nome) {
        WebElement row = driver.findElement(By.xpath("//td[text()='" + nome + "']/parent::tr"));
        row.findElement(By.cssSelector("a span.glyphicon-pencil")).click();
    }
}
