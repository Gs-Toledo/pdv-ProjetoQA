package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockConstruction;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import net.originmobi.pdv.controller.TituloService;
import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Parcela;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Receber;
import net.originmobi.pdv.model.Recebimento;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.RecebimentoRepository;
import net.originmobi.pdv.service.cartao.CartaoLancamentoService;
import net.originmobi.pdv.singleton.Aplicacao;

@ExtendWith(MockitoExtension.class)
class RecebimentoServiceTest {

    @InjectMocks
    private RecebimentoService service;

    @Mock
    private RecebimentoRepository recebimentos;

    @Mock
    private PessoaService pessoas;

    @Mock
    private RecebimentoParcelaService receParcelas;

    @Mock
    private ParcelaService parcelas;

    @Mock
    private CaixaService caixas;

    @Mock
    private UsuarioService usuarios;

    @Mock
    private CaixaLancamentoService lancamentos;

    @Mock
    private TituloService titulos;

    @Mock
    private CartaoLancamentoService cartaoLancamentos;

    private net.originmobi.pdv.model.TituloTipo criarTituloTipoModel(String sigla) {
        net.originmobi.pdv.model.TituloTipo tipo = new net.originmobi.pdv.model.TituloTipo();
        tipo.setSigla(sigla);
        return tipo;
    }

    @Test
    @DisplayName("Deve abrir recebimento com sucesso")
    void deveAbrirRecebimentoComSucesso() {
        Long codPessoa = 1L;
        String[] arrayParcelas = {"10", "20"};

        Pessoa pessoaMock = new Pessoa();
        pessoaMock.setCodigo(codPessoa);

        // Mocks das parcelas
        Parcela p1 = new Parcela();
        p1.setCodigo(10L);
        p1.setQuitado(0);
        p1.setValor_restante(100.0);
        Receber r1 = new Receber();
        r1.setPessoa(pessoaMock);
        p1.setReceber(r1);

        Parcela p2 = new Parcela();
        p2.setCodigo(20L);
        p2.setQuitado(0);
        p2.setValor_restante(50.0);
        Receber r2 = new Receber();
        r2.setPessoa(pessoaMock);
        p2.setReceber(r2);

        when(parcelas.busca(10L)).thenReturn(p1);
        when(parcelas.busca(20L)).thenReturn(p2);
        when(pessoas.buscaPessoa(codPessoa)).thenReturn(Optional.of(pessoaMock));

        when(recebimentos.save(any(Recebimento.class))).thenAnswer(invocation -> {
            Recebimento r = invocation.getArgument(0);
            r.setCodigo(500L);
            return r;
        });

        String resultado = service.abrirRecebimento(codPessoa, arrayParcelas);

        assertEquals("500", resultado);

        ArgumentCaptor<Recebimento> captor = ArgumentCaptor.forClass(Recebimento.class);
        verify(recebimentos).save(captor.capture());
        assertEquals(150.0, captor.getValue().getValor_total());
    }

    @Test
    @DisplayName("Deve lançar erro ao tentar abrir recebimento de parcela já quitada")
    void deveLancarErroParcelaQuitada() {
        Long codPessoa = 1L;
        String[] arrayParcelas = {"10"};

        Parcela p1 = new Parcela();
        p1.setCodigo(10L);
        p1.setQuitado(1);

        when(parcelas.busca(10L)).thenReturn(p1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.abrirRecebimento(codPessoa, arrayParcelas);
        });

        assertEquals("Parcela 10 já esta quitada, verifique.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar erro se a parcela pertencer a outra pessoa")
    void deveLancarErroParcelaDeOutraPessoa() {
        Long codPessoa = 1L;
        String[] arrayParcelas = {"10"};

        Pessoa outraPessoa = new Pessoa();
        outraPessoa.setCodigo(99L);

        Parcela p1 = new Parcela();
        p1.setCodigo(10L);
        p1.setQuitado(0);

        Receber receber = new Receber();
        receber.setPessoa(outraPessoa);
        p1.setReceber(receber);

        when(parcelas.busca(10L)).thenReturn(p1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.abrirRecebimento(codPessoa, arrayParcelas);
        });

        assertEquals("A parcela 10 não pertence ao cliente selecionado", ex.getMessage());
    }

    @Test
    @DisplayName("Deve processar recebimento em DINHEIRO com sucesso")
    void deveProcessarRecebimentoDinheiro() {
        Long codReceber = 100L;
        Long codTitulo = 5L;
        Double vlRecebido = 100.00;

        Recebimento recebimento = new Recebimento();
        recebimento.setCodigo(codReceber);
        recebimento.setValor_total(100.00);

        Titulo titulo = new Titulo();
        titulo.setCodigo(codTitulo);
        titulo.setTipo(criarTituloTipoModel("DINHEIRO"));

        Parcela parcela = new Parcela();
        parcela.setCodigo(10L);
        parcela.setValor_restante(100.00);

        Caixa caixa = new Caixa();

        when(recebimentos.findById(codReceber)).thenReturn(Optional.of(recebimento));
        when(titulos.busca(codTitulo)).thenReturn(Optional.of(titulo));
        when(receParcelas.parcelasDoReceber(codReceber)).thenReturn(Arrays.asList(parcela));
        when(usuarios.buscaUsuario(any())).thenReturn(new Usuario());
        when(caixas.caixaAberto()).thenReturn(Optional.of(caixa));

        try (MockedConstruction<Aplicacao> mocked = mockConstruction(Aplicacao.class,
                (mock, context) -> {
                    when(mock.getUsuarioAtual()).thenReturn("usuario_teste");
                })) {

            String resultado = service.receber(codReceber, vlRecebido, 0.0, 0.0, codTitulo);
            assertEquals("Recebimento realizado com sucesso", resultado);
        }

        verify(parcelas).receber(10L, 100.00, 0.0, 0.0);
        verify(lancamentos).lancamento(any(CaixaLancamento.class));
        verify(cartaoLancamentos, never()).lancamento(anyDouble(), any(Optional.class));
        verify(recebimentos).save(recebimento);
    }

    @Test
    @DisplayName("Deve processar recebimento em CARTÃO DÉBITO com sucesso")
    void deveProcessarRecebimentoCartao() {
        Long codReceber = 100L;
        Long codTitulo = 6L;
        Double vlRecebido = 50.00;

        Recebimento recebimento = new Recebimento();
        recebimento.setCodigo(codReceber);
        recebimento.setValor_total(50.00);

        Titulo titulo = new Titulo();
        titulo.setCodigo(codTitulo);
        titulo.setTipo(criarTituloTipoModel(TituloTipo.CARTDEB.toString()));

        Parcela parcela = new Parcela();
        parcela.setCodigo(20L);
        parcela.setValor_restante(50.00);

        when(recebimentos.findById(codReceber)).thenReturn(Optional.of(recebimento));
        when(titulos.busca(codTitulo)).thenReturn(Optional.of(titulo));
        when(receParcelas.parcelasDoReceber(codReceber)).thenReturn(Arrays.asList(parcela));
        when(usuarios.buscaUsuario(any())).thenReturn(new Usuario());

        try (MockedConstruction<Aplicacao> mocked = mockConstruction(Aplicacao.class,
                (mock, context) -> {
                    when(mock.getUsuarioAtual()).thenReturn("usuario_teste");
                })) {

            service.receber(codReceber, vlRecebido, 0.0, 0.0, codTitulo);
        }

        verify(cartaoLancamentos).lancamento(anyDouble(), any(Optional.class));
        verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
    }

    @Test
    @DisplayName("Deve lançar erro se recebimento já estiver fechado")
    void deveLancarErroRecebimentoFechado() {
        Long codReceber = 100L;
        Long codTitulo = 5L;

        Recebimento recebimento = new Recebimento();
        recebimento.setData_processamento(Timestamp.from(Instant.now()));

        when(recebimentos.findById(codReceber)).thenReturn(Optional.of(recebimento));
        when(titulos.busca(codTitulo)).thenReturn(Optional.of(new Titulo()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.receber(codReceber, 10.0, 0.0, 0.0, codTitulo);
        });

        assertEquals("Recebimento já esta fechado", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar erro se valor recebido for maior que o total")
    void deveLancarErroValorSuperior() {
        Long codReceber = 100L;
        Long codTitulo = 5L;

        Recebimento recebimento = new Recebimento();
        recebimento.setValor_total(100.00);

        Titulo titulo = new Titulo();
        titulo.setTipo(criarTituloTipoModel("DINHEIRO"));

        when(recebimentos.findById(codReceber)).thenReturn(Optional.of(recebimento));
        when(titulos.busca(codTitulo)).thenReturn(Optional.of(titulo));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.receber(codReceber, 200.00, 0.0, 0.0, codTitulo);
        });

        assertEquals("Valor de recebimento é superior aos títulos", ex.getMessage());
    }

    @Test
    @DisplayName("Deve remover recebimento com sucesso")
    void deveRemoverRecebimento() {
        Long codReceber = 100L;
        Recebimento recebimento = new Recebimento();

        when(recebimentos.findById(codReceber)).thenReturn(Optional.of(recebimento));

        String resultado = service.remover(codReceber);

        assertEquals("removido com sucesso", resultado);
        verify(recebimentos).deleteById(codReceber);
    }
}