package net.originmobi.pdv.selenium.pages.fornecedor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class FornecedorFormPage {
    private WebDriver driver;

    public FornecedorFormPage(WebDriver driver) {
        this.driver = driver;
    }

    public void preencherNome(String nome) {
        driver.findElement(By.id("nome")).clear();
        driver.findElement(By.id("nome")).sendKeys(nome);
    }

    public void preencherNomeFantasia(String nome) {
        driver.findElement(By.id("nomefantasia")).clear();
        driver.findElement(By.id("nomefantasia")).sendKeys(nome);
    }

    public void preencherCnpj(String cnpj) {
        driver.findElement(By.id("cnpj")).clear();
        driver.findElement(By.id("cnpj")).sendKeys(cnpj);
    }

    public void preencherTelefone(String telefone) {
        driver.findElement(By.id("fone")).clear();
        driver.findElement(By.id("fone")).sendKeys(telefone);
    }

    // Endereço
    public void preencherEndereco(String cidade, String rua, String bairro, String numero, String cep) {
        Select selectCidade = new Select(driver.findElement(By.id("cidade")));
        selectCidade.selectByVisibleText(cidade);

        driver.findElement(By.id("rua")).clear();
        driver.findElement(By.id("rua")).sendKeys(rua);

        driver.findElement(By.id("bairro")).clear();
        driver.findElement(By.id("bairro")).sendKeys(bairro);

        driver.findElement(By.id("numero")).clear();
        driver.findElement(By.id("numero")).sendKeys(numero);

        driver.findElement(By.id("cep")).clear();
        driver.findElement(By.id("cep")).sendKeys(cep);
    }

    public void salvar() {
        driver.findElement(By.cssSelector("input[type='submit']")).click();
    }

    public boolean mensagemSucessoVisivel() {
        return driver.getPageSource().contains("sucesso");
    }

    public boolean mensagemErroCnpjDuplicado() {
        return driver.getPageSource().contains("CNPJ já cadastrado");
    }
}
