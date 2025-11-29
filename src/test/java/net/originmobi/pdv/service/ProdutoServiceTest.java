package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import net.originmobi.pdv.filter.ProdutoFilter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
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

    private Date dataValidadeExemplo;

    // Injeta os Mocks acima na instância real do ProdutoService
    @InjectMocks
    private ProdutoService service;
    private final Long COD_NOVO = 0L;
    private final Long COD_EXISTENTE = 1L;

    @BeforeEach
    void setUp() {
        // Inicializa a data de validade de exemplo (java.util.Date)
        dataValidadeExemplo = new Date(); 
    }

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

    // Testes do método 'merger' 
    // Teste para inserção de novo produto quando codprod == 0 E sem tributação

    @Test
    void merger_QuandoCodprodEhZero_DeveInserirNovoProdutoComSucesso() {
        String resultado = service.merger( 
            0L, 
            1L, 2L, 3L, 0, "Novo Produto Teste", 
            10.0, 20.0, dataValidadeExemplo, 
            "SIM", "ATIVO", "UN", ProdutoSubstTributaria.NAO, 
            "12345678", "12345", 1L, 1L, "SIM"
        );

        verify(produtos, times(1)).insere(
            anyLong(), anyLong(), anyLong(), anyInt(), anyString(), 
            anyDouble(), anyDouble(), any(java.util.Date.class), anyString(), 
            anyString(), anyString(), anyInt(), any(java.sql.Date.class), 
            anyString(), anyString(), anyLong(), anyLong(), anyString()
        );
        
        verify(produtos, never()).atualiza(any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any());

        assertEquals("Produdo cadastrado com sucesso", resultado);
    }

    // Teste para atualização de produto existente quando codprod != 0 E sem tributação
    @Test
    void merger_QuandoCodprodNaoEhZero_DeveAtualizarProdutoComSucesso() {
        Long codProdutoExistente = 100L; 
        
        String resultado = service.merger(
            codProdutoExistente, 
            1L, 2L, 3L, 0, "Produto Atualizado", 
            15.0, 25.0, dataValidadeExemplo, 
            "SIM", "ATIVO", "UN", ProdutoSubstTributaria.NAO, 
            "12345678", "12345", 1L, 1L, "SIM"
        );

        verify(produtos, times(1)).atualiza(
            eq(codProdutoExistente), 
            anyLong(), anyLong(), anyLong(), anyInt(), anyString(), 
            anyDouble(), anyDouble(), any(java.util.Date.class), anyString(), 
            anyString(), anyString(), anyInt(), anyString(), anyString(), 
            anyLong(), anyLong(), anyString()
        );
        
        verify(produtos, never()).insere(
            anyLong(), anyLong(), anyLong(), anyInt(), anyString(), 
            anyDouble(), anyDouble(), any(java.util.Date.class), anyString(), 
            anyString(), anyString(), anyInt(), any(java.sql.Date.class), 
            anyString(), anyString(), anyLong(), anyLong(), anyString()
        );

        assertEquals("Produto atualizado com sucesso", resultado);
    }

    // Teste para erro ao cadastrar novo produto quando codprod == 0
    @Test
    void merger_QuandoCodprodEhZero_DeveRetornarErroAoCadastrar() {
        doThrow(new RuntimeException("Simulação de Erro de DB")).when(produtos).insere(
            anyLong(), anyLong(), anyLong(), anyInt(), anyString(), 
            anyDouble(), anyDouble(), any(java.util.Date.class), anyString(), 
            anyString(), anyString(), anyInt(), any(java.sql.Date.class), 
            anyString(), anyString(), anyLong(), anyLong(), anyString()
        );

        String resultado = service.merger(
            0L, 
            1L, 2L, 3L, 0, "Produto Falho", 
            10.0, 20.0, dataValidadeExemplo, 
            "SIM", "ATIVO", "UN", ProdutoSubstTributaria.NAO, 
            "12345678", "12345", 1L, 1L, "SIM"
        );

        verify(produtos, times(1)).insere(
            anyLong(), anyLong(), anyLong(), anyInt(), anyString(), 
            anyDouble(), anyDouble(), any(java.util.Date.class), anyString(), 
            anyString(), anyString(), anyInt(), any(java.sql.Date.class), 
            anyString(), anyString(), anyLong(), anyLong(), anyString()
        );
        
        verify(produtos, never()).atualiza(any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any());

        assertEquals("Erro a cadastrar produto, chame o suporte", resultado);
    }


    // Teste para erro ao atualizar produto existente quando codprod != 0 E tem tributação
    @Test
    void merger_QuandoCodprodNaoEhZero_DeveRetornarErroAoAtualizar() {
        Long codProdutoExistente = 101L;

        doThrow(new RuntimeException("Simulação de Erro de DB ou Tributação Inválida")).when(produtos).atualiza(
            eq(codProdutoExistente), 
            anyLong(), anyLong(), anyLong(), anyInt(), anyString(), 
            anyDouble(), anyDouble(), any(java.util.Date.class), anyString(), 
            anyString(), anyString(), anyInt(), anyString(), anyString(), 
            anyLong(), anyLong(), anyString()
        );

        String resultado = service.merger(
            codProdutoExistente, 
            1L, 2L, 3L, 0, "Produto Tributado", 
            15.0, 25.0, dataValidadeExemplo, 
            "SIM", "ATIVO", "UN", ProdutoSubstTributaria.SIM, 
            "99999999", "66666", 5L, 2L, "SIM" 
        );

        verify(produtos, times(1)).atualiza(
            eq(codProdutoExistente), 
            anyLong(), anyLong(), anyLong(), anyInt(), anyString(), 
            anyDouble(), anyDouble(), any(java.util.Date.class), anyString(), 
            anyString(), anyString(), anyInt(), anyString(), anyString(), 
            anyLong(), anyLong(), anyString()
        );
        
        verify(produtos, never()).insere(
            anyLong(), anyLong(), anyLong(), anyInt(), anyString(), 
            anyDouble(), anyDouble(), any(java.util.Date.class), anyString(), 
            anyString(), anyString(), anyInt(), any(java.sql.Date.class), 
            anyString(), anyString(), anyLong(), anyLong(), anyString()
        );

        assertEquals("Erro a atualizar produto, chame o suporte", resultado);
    }

    //Teste para buscar produtos 
    //teste para buscar produtos com codigo existente
    @Test
    void buscaProduto_deveRetornarOptionalPreenchidoParaCodigoExistente() {
        Long codigoExistente = 20L;
        Produto produtoMock = new Produto();
        produtoMock.setCodigo(codigoExistente);
        
        when(produtos.findById(codigoExistente)).thenReturn(Optional.of(produtoMock));

        Optional<Produto> resultado = service.buscaProduto(codigoExistente);

        assertTrue(resultado.isPresent());
        assertEquals(codigoExistente, resultado.get().getCodigo());
        
        verify(produtos, times(1)).findById(codigoExistente);
    }

    // teste para buscar produto com código inexistente
    @Test
    void buscaProduto_deveRetornarOptionalVazioParaCodigoInexistente() {
        Long codigoInexistente = 999L;
        
        when(produtos.findById(codigoInexistente)).thenReturn(Optional.empty());

        Optional<Produto> resultado = service.buscaProduto(codigoInexistente);

        assertFalse(resultado.isPresent());
        
        verify(produtos, times(1)).findById(codigoInexistente);
    }

    //Teste filter
    //teste para filtrar produtos por descrição contendo um termo específico
    @Test
    void filter_deveFiltrarPorDescricaoContendo() {
        String termoBusca = "Biscoito";
        ProdutoFilter filter = new ProdutoFilter();
        filter.setDescricao(termoBusca);
        
        Pageable pageableMock = mock(Pageable.class);
        
        Produto p1 = new Produto(); p1.setDescricao("Biscoito Cream Cracker");
        Produto p2 = new Produto(); p2.setDescricao("Biscoito Maizena");
        List<Produto> listaMock = List.of(p1, p2);
        Page<Produto> paginaMock = new PageImpl<>(listaMock, pageableMock, listaMock.size());

        when(produtos.findByDescricaoContaining(eq(termoBusca), eq(pageableMock))).thenReturn(paginaMock);

        Page<Produto> resultado = service.filter(filter, pageableMock);

        verify(produtos, times(1)).findByDescricaoContaining(eq(termoBusca), eq(pageableMock));
        
        assertEquals(2, resultado.getTotalElements());
        assertEquals("Biscoito Cream Cracker", resultado.getContent().get(0).getDescricao());
    }

    //teste para filtrar produtos quando a descrição for nula
    @Test
    void filter_deveRetornarTodosProdutosQuandoDescricaoForNula() {
        ProdutoFilter filter = new ProdutoFilter();
        filter.setDescricao(null);
        
        Pageable pageableMock = mock(Pageable.class);
        
        Page<Produto> paginaMock = mock(Page.class);

        String coringa = "%";
        when(produtos.findByDescricaoContaining(eq(coringa), eq(pageableMock))).thenReturn(paginaMock);

        service.filter(filter, pageableMock);

        verify(produtos, times(1)).findByDescricaoContaining(eq(coringa), eq(pageableMock));
        
        verify(produtos, never()).findByDescricaoContaining(eq(null), any());
    }

}