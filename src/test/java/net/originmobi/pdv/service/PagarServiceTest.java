package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
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

    @Mock private Fornecedor fornecedorMock;
    @Mock private PagarTipo pagarTipoMock;
    @Mock private PagarParcela parcelaMock;
    @Mock private Caixa caixaMock;
    @Mock private Usuario usuarioMock;
    @Mock private Aplicacao aplicacaoMock;
    
    @Captor
    private ArgumentCaptor<CaixaLancamento> lancamentoCaptor;

    @Captor
    private ArgumentCaptor<Pagar> pagarCaptor;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Listar: Deve retornar lista de contas a pagar")
    void deveListarContas() {
        Pagar pagar1 = mock(Pagar.class);
        when(pagarRepo.findAll()).thenReturn(Arrays.asList(pagar1));

        List<Pagar> resultado = service.listar();

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        verify(pagarRepo).findAll();
    }

    @Test
    @DisplayName("Cadastrar: Deve lançar despesa com sucesso")
    void deveCadastrarDespesaComSucesso() {
        Long codFornecedor = 1L;
        Double valor = 100.00;
        String obs = "Teste Despesa";
        LocalDate vencimento = LocalDate.now().plusDays(5);

        when(fornecedores.busca(codFornecedor)).thenReturn(Optional.of(fornecedorMock));
        when(pagarRepo.save(any(Pagar.class))).thenReturn(mock(Pagar.class));

        String resultado = service.cadastrar(codFornecedor, valor, obs, vencimento, pagarTipoMock);

        assertEquals("Despesa lançada com sucesso", resultado);
        
        verify(pagarRepo).save(pagarCaptor.capture());
        assertNotNull(pagarCaptor.getValue());
        verify(pagarParcelaServ).cadastrar(eq(valor), eq(valor), eq(0), any(), eq(vencimento), any(Pagar.class));
    }
    
    @Test
    @DisplayName("Cadastrar: Deve usar descrição do Tipo quando observação for vazia")
    void deveUsarDescricaoDoTipoSeObsVazia() {
        when(fornecedores.busca(anyLong())).thenReturn(Optional.of(fornecedorMock));
        when(pagarTipoMock.getDescricao()).thenReturn("Desc Padrão");
        
        service.cadastrar(1L, 50.0, "", LocalDate.now(), pagarTipoMock);
        
        verify(pagarTipoMock).getDescricao();
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
        verifyNoInteractions(pagarParcelaServ);
    }
    
    @Test
    @DisplayName("Cadastrar: Deve lançar exception se falhar ao cadastrar parcelas")
    void deveLancarExceptionSeFalharCadastrarParcela() {
        when(fornecedores.busca(anyLong())).thenReturn(Optional.of(fornecedorMock));
        when(pagarRepo.save(any(Pagar.class))).thenReturn(mock(Pagar.class));
        
        doThrow(new RuntimeException("Erro Parcelas")).when(pagarParcelaServ)
            .cadastrar(anyDouble(), anyDouble(), anyInt(), any(), any(), any());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.cadastrar(1L, 100.0, "Obs", LocalDate.now(), pagarTipoMock);
        });

        assertEquals("Erro ao lançar despesa, chame o suporte", exception.getMessage());
    }

    @Test
    @DisplayName("Quitar: Deve calcular matemática corretamente e salvar merge")
    void deveCalcularValoresMatematicosCorretamente() {
        Long codParcela = 10L;
        setupMocksBasicosQuitar(codParcela, 100.0, 1000.0);
        when(parcelaMock.getValor_pago()).thenReturn(10.0);
        
        try (MockedStatic<Aplicacao> appStatic = Mockito.mockStatic(Aplicacao.class)) {
            configurarMockAplicacao(appStatic);
            
            service.quitar(codParcela, 50.0, 5.0, 2.0, 1L);
            
            verify(parcelaMock).setValor_pago(62.0); 
            verify(parcelaMock).setValor_restante(45.0); 
            verify(parcelaMock).setValor_desconto(5.0);
            verify(parcelaMock).setValor_acrescimo(2.0);
            verify(pagarParcelaServ).merger(parcelaMock);
        }
    }

    @Test
    @DisplayName("Quitar: Deve zerar restante se desconto + pagamento superarem a dívida")
    void deveZerarRestanteQuandoDescontoTornaSaldoNegativo() {
        Long codParcela = 10L;
        setupMocksBasicosQuitar(codParcela, 100.0, 1000.0);
        
        try (MockedStatic<Aplicacao> appStatic = Mockito.mockStatic(Aplicacao.class)) {
            configurarMockAplicacao(appStatic);
            
            service.quitar(codParcela, 90.0, 20.0, 0.0, 1L);
            
            verify(parcelaMock).setValor_restante(0.0); 
            verify(parcelaMock).setQuitado(1);
            verify(pagarParcelaServ).merger(parcelaMock);
        }
    }

    @Test
    @DisplayName("Quitar: Deve marcar como quitado (1) se saldo restante for zero")
    void deveMarcarComoQuitadoSePagarTudo() {
        Long codParcela = 10L;
        setupMocksBasicosQuitar(codParcela, 100.0, 500.0);
        
        try (MockedStatic<Aplicacao> appStatic = Mockito.mockStatic(Aplicacao.class)) {
            configurarMockAplicacao(appStatic);
            
            service.quitar(codParcela, 100.0, 0.0, 0.0, 1L);
            
            verify(parcelaMock).setQuitado(1);
            verify(pagarParcelaServ).merger(parcelaMock);
        }
    }
    
    @Test
    @DisplayName("Quitar: NÃO deve marcar como quitado (0) se houver saldo restante")
    void naoDeveMarcarQuitadoSePagamentoParcial() {
        Long codParcela = 10L;
        setupMocksBasicosQuitar(codParcela, 100.0, 500.0);
        
        try (MockedStatic<Aplicacao> appStatic = Mockito.mockStatic(Aplicacao.class)) {
            configurarMockAplicacao(appStatic);
            
            service.quitar(codParcela, 50.0, 0.0, 0.0, 1L);
            
            verify(parcelaMock).setQuitado(0);
            verify(pagarParcelaServ).merger(parcelaMock);
        }
    }

    @Test
    @DisplayName("Quitar: Deve gerar Lançamento de Caixa com tipos corretos")
    void deveGerarLancamentoComTiposCorretos() throws Exception {
        Long codParcela = 10L;
        setupMocksBasicosQuitar(codParcela, 100.0, 500.0);
        
        try (MockedStatic<Aplicacao> appStatic = Mockito.mockStatic(Aplicacao.class)) {
            configurarMockAplicacao(appStatic);
            
            service.quitar(codParcela, 100.0, 0.0, 0.0, 1L);
            
            verify(lancamentos).lancamento(lancamentoCaptor.capture());
            CaixaLancamento lancamentoGerado = lancamentoCaptor.getValue();
            
            assertEquals(TipoLancamento.PAGAMENTO, lancamentoGerado.getTipo());
            assertEquals(EstiloLancamento.SAIDA, lancamentoGerado.getEstilo());
            
            try {
                Field fieldParcela = CaixaLancamento.class.getDeclaredField("parcelaPagar"); 
                fieldParcela.setAccessible(true);
                PagarParcela parcelaVinculada = (PagarParcela) fieldParcela.get(lancamentoGerado);
                assertEquals(parcelaMock, parcelaVinculada);
            } catch (NoSuchFieldException e) {
                System.out.println("Aviso: Campo parcelaPagar não encontrado para validação via reflection.");
            }
        }
    }

    @Test
    @DisplayName("Quitar: Deve bloquear pagamento maior que valor restante")
    void deveBloquearPagamentoExcedente() {
        Long codParcela = 10L;
        when(pagarParcelaServ.busca(codParcela)).thenReturn(Optional.of(parcelaMock));
        when(parcelaMock.getValor_restante()).thenReturn(100.0);
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.quitar(codParcela, 200.0, 0.0, 0.0, 1L);
        });
        
        assertEquals("Valor de pagamento inválido", ex.getMessage());
        verify(pagarParcelaServ, never()).merger(any());
    }

    @Test
    @DisplayName("Quitar: Deve bloquear se saldo do caixa for insuficiente")
    void deveBloquearSemSaldoCaixa() {
        Long codParcela = 10L;
        Long codCaixa = 5L;
        
        when(pagarParcelaServ.busca(codParcela)).thenReturn(Optional.of(parcelaMock));
        when(parcelaMock.getValor_restante()).thenReturn(100.0);
        lenient().when(parcelaMock.getValor_pago()).thenReturn(0.0);
        lenient().when(parcelaMock.getValor_desconto()).thenReturn(0.0);
        lenient().when(parcelaMock.getValor_acrescimo()).thenReturn(0.0);

        try (MockedStatic<Aplicacao> appStatic = Mockito.mockStatic(Aplicacao.class)) {
            configurarMockAplicacao(appStatic);
            
            when(caixas.busca(codCaixa)).thenReturn(Optional.of(caixaMock));
            when(caixaMock.getValor_total()).thenReturn(50.0); 

            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                service.quitar(codParcela, 100.0, 0.0, 0.0, codCaixa);
            });
            
            assertEquals("Saldo insuficiente para realizar este pagamento", ex.getMessage());
            
            verify(pagarParcelaServ).merger(parcelaMock); 
            verify(lancamentos, never()).lancamento(any());
        }
    }
    
    @Test
    @DisplayName("Quitar: Deve tratar erro no lançamento ao caixa mas ter tentado salvar parcela")
    void deveTratarErroNoLancamentoCaixa() {
        Long codParcela = 10L;
        setupMocksBasicosQuitar(codParcela, 100.0, 500.0);
        
        doThrow(new RuntimeException("Erro lancamento")).when(lancamentos).lancamento(any());
        
        try (MockedStatic<Aplicacao> appStatic = Mockito.mockStatic(Aplicacao.class)) {
            configurarMockAplicacao(appStatic);
            
            RuntimeException ex = assertThrows(RuntimeException.class, () -> {
                service.quitar(codParcela, 100.0, 0.0, 0.0, 1L);
            });
            
            assertEquals("Ocorreu um erro ao realizar o pagamento, chame o suporte", ex.getMessage());
            
            verify(pagarParcelaServ).merger(parcelaMock);
        }
    }
    
    private void setupMocksBasicosQuitar(Long codParcela, Double valorRestanteParcela, Double saldoCaixa) {
        when(pagarParcelaServ.busca(codParcela)).thenReturn(Optional.of(parcelaMock));
        when(parcelaMock.getValor_restante()).thenReturn(valorRestanteParcela);
        
        lenient().when(parcelaMock.getValor_pago()).thenReturn(0.0);
        lenient().when(parcelaMock.getValor_desconto()).thenReturn(0.0);
        lenient().when(parcelaMock.getValor_acrescimo()).thenReturn(0.0);

        when(caixas.busca(anyLong())).thenReturn(Optional.of(caixaMock));
        lenient().when(caixaMock.getValor_total()).thenReturn(saldoCaixa);
    }

    private void configurarMockAplicacao(MockedStatic<Aplicacao> appStatic) {
        appStatic.when(Aplicacao::getInstancia).thenReturn(aplicacaoMock);
        when(aplicacaoMock.getUsuarioAtual()).thenReturn("user_teste");
        lenient().when(usuarios.buscaUsuario("user_teste")).thenReturn(usuarioMock);
    }
}