package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.model.TituloTipo;
import net.originmobi.pdv.enumerado.VendaSituacao;
import net.originmobi.pdv.filter.VendaFilter;
import net.originmobi.pdv.model.*;
import net.originmobi.pdv.repository.VendaRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;
import net.originmobi.pdv.singleton.Aplicacao;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @InjectMocks
    private VendaService service;

    @Mock private VendaRepository vendas;
    @Mock private UsuarioService usuarios;
    @Mock private VendaProdutoService vendaProdutos;
    @Mock private PagamentoTipoService formaPagamentos;
    @Mock private CaixaService caixas;
    @Mock private ReceberService receberService;
    @Mock private ParcelaService parcelas;
    @Mock private CaixaLancamentoService lancamentos;
    @Mock private TituloService tituloService;
    @Mock private CartaoLancamentoService cartaoLancamento;
    @Mock private ProdutoService produtos;

    @Test
    @DisplayName("Deve abrir uma NOVA venda (código null) mockando a classe estática Aplicacao")
    void deveAbrirNovaVenda() {
        Venda venda = new Venda();
        venda.setCodigo(null);

        Usuario usuarioMock = new Usuario();
        usuarioMock.setUser("Admin");

        try (MockedStatic<Aplicacao> aplicacaoStatic = Mockito.mockStatic(Aplicacao.class)) {
            Aplicacao appInstancia = mock(Aplicacao.class);
            aplicacaoStatic.when(Aplicacao::getInstancia).thenReturn(appInstancia);
            when(appInstancia.getUsuarioAtual()).thenReturn("admin");

            when(usuarios.buscaUsuario("admin")).thenReturn(usuarioMock);

            service.abreVenda(venda);

            verify(vendas).save(venda);
            assertEquals(VendaSituacao.ABERTA, venda.getSituacao());
            assertEquals(0.00, venda.getValor_produtos());
            assertEquals(usuarioMock, venda.getUsuario());
        }
    }

    @Test
    @DisplayName("Deve capturar exceção silenciosamente ao falhar SAVE de nova venda")
    void deveTratarErroAoSalvarNovaVenda() {
        Venda venda = new Venda();
        venda.setCodigo(null);

        try (MockedStatic<Aplicacao> appMock = Mockito.mockStatic(Aplicacao.class)) {
            Aplicacao app = mock(Aplicacao.class);
            appMock.when(Aplicacao::getInstancia).thenReturn(app);
            when(app.getUsuarioAtual()).thenReturn("admin");
            when(usuarios.buscaUsuario("admin")).thenReturn(new Usuario());

            doThrow(new RuntimeException("Erro de conexão")).when(vendas).save(any(Venda.class));

            Long resultado = service.abreVenda(venda);

            assertNull(resultado);

            verify(vendas).save(venda);
        }
    }

    @Test
    @DisplayName("Deve capturar exceção silenciosamente ao falhar UPDATE de venda existente")
    void deveTratarErroAoAtualizarVenda() {
        Venda venda = new Venda();
        venda.setCodigo(10L);
        venda.setPessoa(new Pessoa());
        venda.setObservacao("Obs");

        doThrow(new RuntimeException("Erro ao atualizar")).when(vendas).updateDadosVenda(any(), anyString(), anyLong());

        Long resultado = service.abreVenda(venda);

        assertEquals(10L, resultado);

        verify(vendas).updateDadosVenda(any(), anyString(), eq(10L));
    }

    @Test
    @DisplayName("Deve atualizar venda existente (código preenchido)")
    void deveAtualizarVendaExistente() {
        Venda venda = new Venda();
        venda.setCodigo(10L);
        Pessoa pessoa = new Pessoa();
        venda.setPessoa(pessoa);
        venda.setObservacao("Obs Atualizada");

        service.abreVenda(venda);

        verify(vendas).updateDadosVenda(pessoa, "Obs Atualizada", 10L);
        verify(vendas, never()).save(any());
    }

    @Test
    @DisplayName("Cenário IF: Deve buscar por Código quando o filtro possui código preenchido")
    void deveBuscarPorCodigoQuandoFiltroTemId() {
        Long codigoPesquisa = 123L;

        VendaFilter filter = new VendaFilter();
        filter.setCodigo(codigoPesquisa);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Venda> pageMock = new PageImpl<>(Collections.emptyList());
        when(vendas.findByCodigoIn(codigoPesquisa, pageable)).thenReturn(pageMock);

        Page<Venda> resultado = service.busca(filter, "ABERTA", pageable);

        verify(vendas).findByCodigoIn(codigoPesquisa, pageable);

        verify(vendas, never()).findBySituacaoEquals(any(), any());

        assertEquals(pageMock, resultado);
    }

    @Test
    @DisplayName("Cenário ELSE: Deve buscar por Situação ABERTA quando filtro NÃO tem código")
    void deveBuscarPorSituacaoAbertaQuandoFiltroSemId() {
        VendaFilter filter = new VendaFilter();
        filter.setCodigo(null);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Venda> pageMock = new PageImpl<>(Collections.emptyList());

        when(vendas.findBySituacaoEquals(VendaSituacao.ABERTA, pageable)).thenReturn(pageMock);

        service.busca(filter, "ABERTA", pageable);

        verify(vendas).findBySituacaoEquals(VendaSituacao.ABERTA, pageable);

        verify(vendas, never()).findByCodigoIn(anyLong(), any());
    }

    @Test
    @DisplayName("Cenário ELSE: Deve buscar por Situação FECHADA quando string não é 'ABERTA'")
    void deveBuscarPorSituacaoFechadaQuandoFiltroSemId() {
        VendaFilter filter = new VendaFilter();
        filter.setCodigo(null);

        Pageable pageable = PageRequest.of(0, 10);

        when(vendas.findBySituacaoEquals(VendaSituacao.FECHADA, pageable)).thenReturn(Page.empty());

        service.busca(filter, "FECHADA", pageable);

        verify(vendas).findBySituacaoEquals(VendaSituacao.FECHADA, pageable);
    }

    @Test
    void deveAdicionarProdutoQuandoVendaAberta() {
        when(vendas.verificaSituacao(1L)).thenReturn("ABERTA");

        String resultado = service.addProduto(1L, 100L, 50.0);

        assertEquals("ok", resultado);
        verify(vendaProdutos).salvar(any(VendaProduto.class));
    }

    @Test
    void naoDeveAdicionarProdutoQuandoVendaFechada() {
        when(vendas.verificaSituacao(1L)).thenReturn("FECHADA");

        String resultado = service.addProduto(1L, 100L, 50.0);

        assertEquals("Venda fechada", resultado);
        verify(vendaProdutos, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve capturar exceção silenciosamente ao falhar adição de produto")
    void deveTratarErroAoAdicionarProduto() {
        Long codVenda = 1L;
        Long codProd = 50L;
        Double valor = 10.0;

        when(vendas.verificaSituacao(codVenda)).thenReturn(VendaSituacao.ABERTA.toString());

       doThrow(new RuntimeException("Erro de banco de dados"))
                .when(vendaProdutos).salvar(any(VendaProduto.class));

        String resultado = service.addProduto(codVenda, codProd, valor);

        assertEquals("ok", resultado);

        verify(vendaProdutos).salvar(any(VendaProduto.class));
    }

    @Test
    @DisplayName("Cenário 1 (Novo): Deve retornar mensagem específica se Venda não for encontrada")
    void deveRetornarErroQuandoVendaNaoExiste() {
        // --- ARRANGE ---
        Long codVenda = 99L;
        Long posicaoProd = 5L;

        // Simula o repositório retornando null
        when(vendas.findByCodigoEquals(codVenda)).thenReturn(null);

        // --- ACT ---
        String resultado = service.removeProduto(posicaoProd, codVenda);

        // --- ASSERT ---
        assertEquals("Venda não encontrada", resultado);

        // Garante que não tentou remover nada no service de produtos
        verify(vendaProdutos, never()).removeProduto(anyLong());
    }

    @Test
    @DisplayName("Cenário 2 (Sucesso): Deve remover produto quando Venda está ABERTA")
    void deveRemoverProdutoComSucesso() {
        // --- ARRANGE ---
        Long codVenda = 1L;
        Long posicaoProd = 10L;

        Venda venda = new Venda();
        venda.setSituacao(VendaSituacao.ABERTA);

        when(vendas.findByCodigoEquals(codVenda)).thenReturn(venda);

        // --- ACT ---
        String resultado = service.removeProduto(posicaoProd, codVenda);

        // --- ASSERT ---
        assertEquals("ok", resultado);

        // Verifica se a remoção foi chamada
        verify(vendaProdutos).removeProduto(posicaoProd);
    }

    @Test
    @DisplayName("Cenário 3 (Bloqueio): NÃO deve remover produto quando Venda está FECHADA")
    void naoDeveRemoverProdutoSeVendaFechada() {
        Long codVenda = 1L;
        Long posicaoProd = 10L;

        Venda venda = new Venda();
        venda.setSituacao(VendaSituacao.FECHADA);

        when(vendas.findByCodigoEquals(codVenda)).thenReturn(venda);

        String resultado = service.removeProduto(posicaoProd, codVenda);

        assertEquals("Venda fechada", resultado);

        verify(vendaProdutos, never()).removeProduto(anyLong());
    }

    @Test
    @DisplayName("Cenário 4 (Exception): Erro interno deve cair no catch e retornar 'ok'")
    void deveTratarErroInterno() {
        Long codVenda = 1L;
        Long posicaoProd = 10L;

        doThrow(new RuntimeException("Erro de conexão"))
                .when(vendas).findByCodigoEquals(codVenda);

        String resultado = service.removeProduto(posicaoProd, codVenda);

        assertEquals("ok", resultado);

        verify(vendaProdutos, never()).removeProduto(anyLong());
    }

    @Test
    @DisplayName("Cenário: Pagamento em DINHEIRO (À Vista) com Sucesso")
    void deveFecharVendaNoDinheiro() {
        Long codVenda = 1L;
        Venda venda = criarVendaMock(true);
        when(vendas.findByCodigoEquals(codVenda)).thenReturn(venda);

        PagamentoTipo pagTipo = new PagamentoTipo();
        pagTipo.setFormaPagamento("00");
        when(formaPagamentos.busca(anyLong())).thenReturn(pagTipo);

        Titulo titulo = new Titulo();
        TituloTipo tipoDin = new TituloTipo();
        tipoDin.setSigla("DIN");
        titulo.setTipo(tipoDin);
        when(tituloService.busca(anyLong())).thenReturn(Optional.of(titulo));

        when(caixas.caixaIsAberto()).thenReturn(true);
        Caixa caixa = new Caixa();
        when(caixas.caixaAberto()).thenReturn(Optional.of(caixa));

        try (MockedStatic<Aplicacao> aplicacaoStatic = Mockito.mockStatic(Aplicacao.class)) {
            Aplicacao appInstancia = mock(Aplicacao.class);
            aplicacaoStatic.when(Aplicacao::getInstancia).thenReturn(appInstancia);
            when(appInstancia.getUsuarioAtual()).thenReturn("user_test");
            when(usuarios.buscaUsuario("user_test")).thenReturn(new Usuario());

            String resultado = service.fechaVenda(
                    codVenda,
                    1L,
                    100.0,
                    0.0,
                    0.0,
                    new String[]{"100"},
                    new String[]{"10"}
            );

            assertEquals("Venda finalizada com sucesso", resultado);

            verify(receberService).cadastrar(any(Receber.class));
            verify(lancamentos).lancamento(any(CaixaLancamento.class));
            verify(cartaoLancamento, never()).lancamento(anyDouble(), any());
            verify(produtos).movimentaEstoque(codVenda, EntradaSaida.SAIDA);
        }
    }

    @Test
    @DisplayName("Cenário: Pagamento em CARTÃO DE CRÉDITO")
    void deveFecharVendaNoCartao() {
        Long codVenda = 1L;
        Venda venda = criarVendaMock(true);
        when(vendas.findByCodigoEquals(codVenda)).thenReturn(venda);

        PagamentoTipo pagTipo = new PagamentoTipo();
        pagTipo.setFormaPagamento("00");
        when(formaPagamentos.busca(anyLong())).thenReturn(pagTipo);

        Titulo titulo = new Titulo();
        TituloTipo tipoCart = new TituloTipo();
        tipoCart.setSigla("CARTCRED");
        titulo.setTipo(tipoCart);
        when(tituloService.busca(anyLong())).thenReturn(Optional.of(titulo));

        service.fechaVenda(codVenda, 1L, 100.0, 0.0, 0.0, new String[]{"100"}, new String[]{"20"});

        verify(cartaoLancamento).lancamento(eq(100.0), eq(Optional.of(titulo)));
        verify(lancamentos, never()).lancamento(any());
    }

    @Test
    @DisplayName("Cenário: Pagamento A PRAZO (Duas Parcelas)")
    void deveFecharVendaAPrazo() {
        Long codVenda = 1L;
        Venda venda = criarVendaMock(true);
        when(vendas.findByCodigoEquals(codVenda)).thenReturn(venda);

        PagamentoTipo pagTipo = new PagamentoTipo();
        pagTipo.setFormaPagamento("30/60");
        when(formaPagamentos.busca(anyLong())).thenReturn(pagTipo);

        Titulo titulo = new Titulo();
        TituloTipo tipo = new TituloTipo();
        tipo.setSigla("BOLETO");
        titulo.setTipo(tipo);
        when(tituloService.busca(anyLong())).thenReturn(Optional.of(titulo));

        service.fechaVenda(
                codVenda, 1L, 100.0, 0.0, 0.0,
                new String[]{"50", "50"},
                new String[]{"1", "1"}
        );

        verify(parcelas, times(2)).gerarParcela(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(Receber.class), anyInt(), anyInt(), any(Timestamp.class), any(java.sql.Date.class)
        );
    }

    @Test
    @DisplayName("Erro: Venda Fechada")
    void deveLancarErroSeVendaJaFechada() {
        Venda venda = criarVendaMock(false);
        when(vendas.findByCodigoEquals(1L)).thenReturn(venda);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{}, new String[]{})
        );
        assertEquals("venda fechada", ex.getMessage());
    }

    @Test
    @DisplayName("Erro: Valor das parcelas não bate com total (Dinheiro)")
    void deveLancarErroSeValorParcelasIncorreto() {
        Venda venda = criarVendaMock(true);
        when(vendas.findByCodigoEquals(1L)).thenReturn(venda);
        when(caixas.caixaIsAberto()).thenReturn(true);

        PagamentoTipo pag = new PagamentoTipo();
        pag.setFormaPagamento("00");
        when(formaPagamentos.busca(anyLong())).thenReturn(pag);

        Titulo t = new Titulo();
        t.setTipo(new TituloTipo());
        t.getTipo().setSigla("DIN");
        when(tituloService.busca(anyLong())).thenReturn(Optional.of(t));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.fechaVenda(1L, 1L, 100.0, 0.0, 0.0, new String[]{"90"}, new String[]{"1"})
        );

        assertEquals("Valor das parcelas diferente do valor total de produtos, verifique", ex.getMessage());
    }

    private Venda criarVendaMock(boolean aberta) {
        Venda venda = mock(Venda.class);
        when(venda.isAberta()).thenReturn(aberta);
        if (aberta) {
            when(venda.getPessoa()).thenReturn(new Pessoa());
        }
        return venda;
    }
}