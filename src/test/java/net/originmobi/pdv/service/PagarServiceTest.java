package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Fornecedor;
import net.originmobi.pdv.model.Pagar;
import net.originmobi.pdv.model.PagarParcela;
import net.originmobi.pdv.model.PagarTipo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.PagarRepository;
import net.originmobi.pdv.singleton.Aplicacao;

@ExtendWith(MockitoExtension.class)
public class PagarServiceTest {

    @InjectMocks
    private PagarService service;

    @Mock
    private PagarRepository pagarRepo;

    @Mock
    private PagarParcelaService pagarParcelaServ;

    @Mock
    private FornecedorService fornecedores;

    @Mock
    private CaixaService caixas;

    @Mock
    private UsuarioService usuarios;

    @Mock
    private CaixaLancamentoService lancamentos;

    // Mocks de Entidades para cenários
    @Mock private Fornecedor fornecedorMock;
    @Mock private PagarTipo pagarTipoMock;
    @Mock private PagarParcela parcelaMock;
    @Mock private Caixa caixaMock;
    @Mock private Usuario usuarioMock;
    @Mock private Aplicacao aplicacaoMock;

    @BeforeEach
    void setUp() {
        // Configurações comuns se necessário
    }

    @Test
    @DisplayName("Listar: Deve retornar lista de contas a pagar")
    void deveListarContas() {
        Pagar pagar1 = mock(Pagar.class);
        when(pagarRepo.findAll()).thenReturn(Arrays.asList(pagar1));

        List<Pagar> resultado = service.listar();

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        verify(pagarRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("Cadastrar: Deve lançar despesa com sucesso quando dados válidos")
    void deveCadastrarDespesaComSucesso() {
        Long codFornecedor = 1L;
        Double valor = 100.00;
        String obs = "Teste Despesa";
        LocalDate vencimento = LocalDate.now().plusDays(5);

        when(fornecedores.busca(codFornecedor)).thenReturn(Optional.of(fornecedorMock));
        
        // Mock do save não precisa retornar nada específico além de não lançar erro
        when(pagarRepo.save(any(Pagar.class))).thenReturn(mock(Pagar.class));

        String resultado = service.cadastrar(codFornecedor, valor, obs, vencimento, pagarTipoMock);

        assertEquals("Despesa lançada com sucesso", resultado);
        verify(pagarRepo).save(any(Pagar.class));
        verify(pagarParcelaServ).cadastrar(eq(valor), eq(valor), eq(0), any(), eq(vencimento), any(Pagar.class));
    }

    @Test
    @DisplayName("Cadastrar: Deve usar descrição do Tipo quando observação for vazia")
    void deveUsarDescricaoDoTipoSeObsVazia() {
        when(fornecedores.busca(anyLong())).thenReturn(Optional.of(fornecedorMock));
        when(pagarTipoMock.getDescricao()).thenReturn("Descricao Padrão");

        service.cadastrar(1L, 50.0, "", LocalDate.now(), pagarTipoMock);

        // Verifica se o construtor do Pagar (capturado via ArgumentCaptor seria o ideal, mas verificamos o fluxo aqui)
        // Como o Pagar é instanciado dentro do método com 'new', focamos se o fluxo correu bem.
        verify(pagarTipoMock, times(1)).getDescricao();
        verify(pagarRepo).save(any(Pagar.class));
    }

    @Test
    @DisplayName("Cadastrar: Deve lançar RuntimeException se falhar ao salvar Pagar")
    void deveLancarExceptionErroSalvarPagar() {
        when(fornecedores.busca(anyLong())).thenReturn(Optional.of(fornecedorMock));
        doThrow(new RuntimeException("DB Error")).when(pagarRepo).save(any(Pagar.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.cadastrar(1L, 100.0, "Obs", LocalDate.now(), pagarTipoMock);
        });

        assertEquals("Erro ao lançar despesa, chame o suporte", exception.getMessage());
        // Garante que não tentou salvar parcela se falhou o pai
        verifyNoInteractions(pagarParcelaServ);
    }

    @Test
    @DisplayName("Quitar: Deve realizar quitação com sucesso (Mockando Singleton e DataAtual)")
    void deveQuitarParcelaComSucesso() {
        Long codParcela = 10L;
        Long codCaixa = 5L;
        Double valorPago = 100.0;
        Double valorDesconto = 0.0;
        Double valorAcrescimo = 0.0;

        // 1. Mock da Parcela
        when(pagarParcelaServ.busca(codParcela)).thenReturn(Optional.of(parcelaMock));
        when(parcelaMock.getValor_restante()).thenReturn(100.0);
        when(parcelaMock.getValor_pago()).thenReturn(0.0);
        when(parcelaMock.getValor_desconto()).thenReturn(0.0);
        when(parcelaMock.getValor_acrescimo()).thenReturn(0.0);

        // 2. Mock do Caixa (Saldo suficiente)
        when(caixas.busca(codCaixa)).thenReturn(Optional.of(caixaMock));
        when(caixaMock.getValor_total()).thenReturn(500.0);

        // 3. Mock Static do Singleton Aplicacao
        try (MockedStatic<Aplicacao> aplicacaoStatic = Mockito.mockStatic(Aplicacao.class)) {
            // Configura o comportamento do Singleton
            when(aplicacaoMock.getUsuarioAtual()).thenReturn("usuario_teste");
            aplicacaoStatic.when(Aplicacao::getInstancia).thenReturn(aplicacaoMock);

            when(usuarios.buscaUsuario("usuario_teste")).thenReturn(usuarioMock);

            String resultado = service.quitar(codParcela, valorPago, valorDesconto, valorAcrescimo, codCaixa);

            assertEquals("Pagamento realizado com sucesso", resultado);

            // Verificações
            verify(parcelaMock).setQuitado(1); // Pagou tudo, deve quitar
            verify(parcelaMock).setValor_restante(0.0);
            verify(pagarParcelaServ).merger(parcelaMock);
            verify(lancamentos).lancamento(any(CaixaLancamento.class));
        }
    }

    @Test
    @DisplayName("Quitar: Deve bloquear pagamento maior que o valor restante")
    void deveBloquearPagamentoMaiorQueRestante() {
        Long codParcela = 10L;
        Double valorPago = 200.0; // Tentando pagar 200
        Double valorRestante = 100.0; // Só deve 100

        when(pagarParcelaServ.busca(codParcela)).thenReturn(Optional.of(parcelaMock));
        when(parcelaMock.getValor_restante()).thenReturn(valorRestante);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.quitar(codParcela, valorPago, 0.0, 0.0, 1L);
        });

        assertEquals("Valor de pagamento inválido", ex.getMessage());
        verify(pagarParcelaServ, never()).merger(any());
    }

    @Test
    @DisplayName("Quitar: Deve bloquear pagamento se saldo do caixa for insuficiente")
    void deveBloquearSemSaldoNoCaixa() {
        Long codParcela = 10L;
        Long codCaixa = 5L;
        Double valorPago = 100.0;

        // Mock Parcela
        when(pagarParcelaServ.busca(codParcela)).thenReturn(Optional.of(parcelaMock));
        when(parcelaMock.getValor_restante()).thenReturn(100.0);
        when(parcelaMock.getValor_pago()).thenReturn(0.0);
        when(parcelaMock.getValor_desconto()).thenReturn(0.0);
        when(parcelaMock.getValor_acrescimo()).thenReturn(0.0);

        // Mock Static Application para passar da linha do usuario
        try (MockedStatic<Aplicacao> aplicacaoStatic = Mockito.mockStatic(Aplicacao.class)) {
            when(aplicacaoMock.getUsuarioAtual()).thenReturn("user");
            aplicacaoStatic.when(Aplicacao::getInstancia).thenReturn(aplicacaoMock);
            when(usuarios.buscaUsuario("user")).thenReturn(usuarioMock);

            // Mock Caixa com saldo INSUFICIENTE
            when(caixas.busca(codCaixa)).thenReturn(Optional.of(caixaMock));
            when(caixaMock.getValor_total()).thenReturn(50.0); // Só tem 50, quer pagar 100

            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                service.quitar(codParcela, valorPago, 0.0, 0.0, codCaixa);
            });

            assertEquals("Saldo insuficiente para realizar este pagamento", ex.getMessage());
            // Garante que não gerou lançamento financeiro
            verify(lancamentos, never()).lancamento(any());
        }
    }

    @Test
    @DisplayName("Quitar: Deve lançar erro genérico ao falhar no merge da parcela")
    void deveLancarErroAoFalharMergeDaParcela() {
        Long codParcela = 10L;
        
        when(pagarParcelaServ.busca(codParcela)).thenReturn(Optional.of(parcelaMock));
        when(parcelaMock.getValor_restante()).thenReturn(100.0);
        when(parcelaMock.getValor_pago()).thenReturn(0.0);
        
        // Simula erro no serviço de merge
        doThrow(new RuntimeException("DB Error")).when(pagarParcelaServ).merger(any());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.quitar(codParcela, 100.0, 0.0, 0.0, 1L);
        });

        assertEquals("Ocorreu um erro ao realizar o pagamento, chame o suporte", ex.getMessage());
    }
}