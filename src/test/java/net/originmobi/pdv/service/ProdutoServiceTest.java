package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.produto.ProdutoControleEstoque;
import net.originmobi.pdv.enumerado.produto.ProdutoSubstTributaria;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.repository.ProdutoRepository;


//Mocks 
@ExtendWith(MockitoExtension.class)
public class ProdutoServiceTest {

    // Simula o acesso ao banco de dados
    @Mock
    private ProdutoRepository produtos;

    // Simula o serviço de VendaProduto
    @Mock
    private VendaProdutoService vendaProdutos;

    // --- Classe a ser testada ---

    // Injeta os Mocks acima na instância real do ProdutoService
    @InjectMocks
    private ProdutoService service;
    private final Long COD_NOVO = 0L;
    private final Long COD_EXISTENTE = 1L;

    // --- Teste de Inicialização ---

    @Test
    void serviceDeveSerInicializado() {
        // Garante que o serviço foi criado e que os mocks foram injetados corretamente
        assertNotNull(service, "O ProdutoService não deve ser nulo.");
    }

    //testes movimentaEstoque
    //Simular a lista de produtos vendidos no caminho feliz

    @Test
    void movimentaEstoque_deveDarBaixaComSucesso() {
    Long codVendaTeste = 50L;
    Long codProdutoTeste = 10L;
    int qtdVendida = 5;
    int qtdEstoque = 20;


    List<Object[]> produtosVendidos = Collections.singletonList(
        new Object[]{codProdutoTeste, qtdVendida}
    );
    when(vendaProdutos.buscaQtdProduto(codVendaTeste)).thenReturn(produtosVendidos);

    Produto produto = new Produto();
    produto.setControla_estoque(ProdutoControleEstoque.SIM);
    when(produtos.findByCodigoIn(codProdutoTeste)).thenReturn(produto);

    when(produtos.saldoEstoque(codProdutoTeste)).thenReturn(qtdEstoque); // Saldo > Qtd vendida

    service.movimentaEstoque(codVendaTeste, EntradaSaida.SAIDA);

    verify(produtos, times(1)).movimentaEstoque(
        eq(codProdutoTeste), 
        eq(EntradaSaida.SAIDA.toString()), 
        eq(qtdVendida), 
        anyString(), 
        any(java.sql.Date.class)
    );
    }

    //Simular o saldo insuficiente para o produto vendido
    @Test
    void movimentaEstoque_deveLancarExcecaoPorFaltaDeEstoque() {
        Long codVendaTeste = 51L;
        Long codProdutoTeste = 11L;
        int qtdVendida = 10;
        int qtdEstoque = 5; 

        List<Object[]> produtosVendidos = Collections.singletonList(
            new Object[]{codProdutoTeste, qtdVendida}
        );
        when(vendaProdutos.buscaQtdProduto(codVendaTeste)).thenReturn(produtosVendidos);

        Produto produto = new Produto();
        produto.setControla_estoque(ProdutoControleEstoque.SIM);
        when(produtos.findByCodigoIn(codProdutoTeste)).thenReturn(produto);

        when(produtos.saldoEstoque(codProdutoTeste)).thenReturn(qtdEstoque); 

        assertThrows(RuntimeException.class, () -> {
            service.movimentaEstoque(codVendaTeste, EntradaSaida.SAIDA);
        });

        verify(produtos, never()).movimentaEstoque(any(), any(), anyInt(), any(), any());
    }

    //Simular o produto que não controla estoque
    @Test
    void movimentaEstoque_naoDeveMovimentarSeNaoControlarEstoque() {
        Long codVendaTeste = 52L;
        Long codProdutoTeste = 12L;
        int qtdVendida = 10;
        
        List<Object[]> produtosVendidos = Collections.singletonList(
            new Object[]{codProdutoTeste, qtdVendida}
        );
        when(vendaProdutos.buscaQtdProduto(codVendaTeste)).thenReturn(produtosVendidos);

        Produto produto = new Produto();
        produto.setControla_estoque(ProdutoControleEstoque.NAO); 
        when(produtos.findByCodigoIn(codProdutoTeste)).thenReturn(produto);

        service.movimentaEstoque(codVendaTeste, EntradaSaida.SAIDA);

        verify(produtos, never()).movimentaEstoque(any(), any(), anyInt(), any(), any());
    }

    //testes ajusteEstoque
    //testar ajuste de estoque para produto que controla estoque
    @Test
    void ajusteEstoque_deveMovimentarEstoqueSeProdutoControlar() {
        Long codProdutoTeste = 15L;
        int qtdAjuste = 50;
        
        Produto produto = new Produto();
        produto.setControla_estoque(ProdutoControleEstoque.SIM); 
        when(produtos.findByCodigoIn(codProdutoTeste)).thenReturn(produto);

        service.ajusteEstoque(
            codProdutoTeste, 
            qtdAjuste, 
            EntradaSaida.ENTRADA, 
            "Ajuste Manual", 
            java.sql.Date.valueOf(java.time.LocalDate.now()));

        verify(produtos, times(1)).movimentaEstoque(
            eq(codProdutoTeste), 
            eq(EntradaSaida.ENTRADA.toString()), 
            eq(qtdAjuste), 
            eq("Ajuste Manual"), 
            any(java.sql.Date.class));
    }

    //testar ajuste de estoque para produto que não controla estoque
    @Test
    void ajusteEstoque_deveLancarExcecaoSeProdutoNaoControlarEstoque() {
        Long codProdutoTeste = 16L;
        
        Produto produto = new Produto();
        produto.setControla_estoque(ProdutoControleEstoque.NAO); 
        when(produtos.findByCodigoIn(codProdutoTeste)).thenReturn(produto);

        assertThrows(RuntimeException.class, () -> {
            service.ajusteEstoque(
                codProdutoTeste, 
                10, 
                EntradaSaida.ENTRADA, 
                "Ajuste Manual", 
                java.sql.Date.valueOf(java.time.LocalDate.now()));
        });

        verify(produtos, never()).movimentaEstoque(any(), any(), anyInt(), any(), any());
    }

}