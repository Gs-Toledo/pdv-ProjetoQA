package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.originmobi.pdv.enumerado.TelefoneTipo;
import net.originmobi.pdv.filter.PessoaFilter;
import net.originmobi.pdv.model.Cidade;
import net.originmobi.pdv.model.Endereco;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.model.Telefone;
import net.originmobi.pdv.repository.PessoaRepository;

@ExtendWith(MockitoExtension.class)
class PessoaServiceTest {

    @InjectMocks
    private PessoaService service;

    @Mock
    private PessoaRepository repository;

    @Mock
    private CidadeService cidadeService;

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private TelefoneService telefoneService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Captor
    private ArgumentCaptor<Pessoa> pessoaCaptor;

    @Captor
    private ArgumentCaptor<Endereco> enderecoCaptor;

    @Captor
    private ArgumentCaptor<Telefone> telefoneCaptor;

    @Test
    @DisplayName("Lista: Deve retornar dados exatos")
    void testLista() {
        Pessoa p = new Pessoa();
        when(repository.findAll()).thenReturn(Arrays.asList(p));
        List<Pessoa> result = service.lista();
        assertEquals(1, result.size());
        assertSame(p, result.get(0));
    }

    @Test
    @DisplayName("Busca: Deve retornar pessoa exata")
    void testBusca() {
        Pessoa p = new Pessoa();
        when(repository.findByCodigoIn(1L)).thenReturn(p);
        Pessoa result = service.busca(1L);
        assertSame(p, result);
    }

    @Test
    @DisplayName("BuscaPessoa: Deve retornar Optional preenchido")
    void testBuscaPessoa() {
        Pessoa p = new Pessoa();
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        Optional<Pessoa> result = service.buscaPessoa(1L);
        assertTrue(result.isPresent());
        assertSame(p, result.get());
    }

    @Test
    @DisplayName("Filter: Nome Nulo deve buscar por '%'")
    void testFilterNomeNulo() {
        PessoaFilter filter = new PessoaFilter();
        filter.setNome(null);
        when(repository.findByNomeContaining("%")).thenReturn(Collections.emptyList());
        
        service.filter(filter);
        
        verify(repository).findByNomeContaining("%");
    }

    @Test
    @DisplayName("Filter: Nome Preenchido deve buscar pelo nome")
    void testFilterNomeInformado() {
        PessoaFilter filter = new PessoaFilter();
        filter.setNome("Ana");
        when(repository.findByNomeContaining("Ana")).thenReturn(Collections.emptyList());
        
        service.filter(filter);
        
        verify(repository).findByNomeContaining("Ana");
    }

    @Test
    @DisplayName("Cadastro Novo: Validação RIGOROSA de todos os campos")
    void testCadastrarCompleto() throws ParseException {
        // ARRANGE
        Cidade cidade = new Cidade();
        cidade.setCodigo(50L);
        when(cidadeService.busca(50L)).thenReturn(Optional.of(cidade));
        
        Endereco endSalvo = new Endereco();
        endSalvo.setCodigo(100L); // Simulando ID gerado
        when(enderecoService.cadastrar(any())).thenReturn(endSalvo);
        
        Telefone telSalvo = new Telefone();
        telSalvo.setCodigo(200L); // Simulando ID gerado
        when(telefoneService.cadastrar(any())).thenReturn(telSalvo);
        
        when(repository.findByCpfcnpjContaining(anyString())).thenReturn(null);

        String retorno = service.cadastrar(0L, "João Silva", "Jão", "111.222.333-44", "01/01/2000", 
                "Cliente VIP", 0L, 50L, "Rua A", "Centro", "10", "99999-000", "Ao lado da padaria",
                0L, "3333-0000", "FIXO", redirectAttributes);

        assertEquals("Pessoa salva com sucesso", retorno);
        verify(enderecoService).cadastrar(enderecoCaptor.capture());
        Endereco endCapturado = enderecoCaptor.getValue();
        assertNull(endCapturado.getCodigo());
        assertEquals("Rua A", endCapturado.getRua());
        assertEquals("Centro", endCapturado.getBairro());
        assertEquals("10", endCapturado.getNumero());
        assertEquals("99999-000", endCapturado.getCep());
        assertEquals("Ao lado da padaria", endCapturado.getReferencia());
        assertEquals(cidade, endCapturado.getCidade());

        verify(telefoneService).cadastrar(telefoneCaptor.capture());
        Telefone telCapturado = telefoneCaptor.getValue();
        assertNull(telCapturado.getCodigo());
        assertEquals("3333-0000", telCapturado.getFone());
        assertEquals(TelefoneTipo.FIXO, telCapturado.getTipo());

        verify(repository).save(pessoaCaptor.capture());
        Pessoa pessoaSalva = pessoaCaptor.getValue();
        
        assertNull(pessoaSalva.getCodigo());
        assertEquals("João Silva", pessoaSalva.getNome());
        assertEquals("Jão", pessoaSalva.getApelido());
        assertEquals("111.222.333-44", pessoaSalva.getCpfcnpj());
        assertEquals("Cliente VIP", pessoaSalva.getObservacao());
        assertEquals(endSalvo, pessoaSalva.getEndereco()); 
        assertEquals(1, pessoaSalva.getTelefone().size());
        assertEquals(telSalvo, pessoaSalva.getTelefone().get(0)); 
        assertNotNull(pessoaSalva.getData_cadastro());
        assertEquals(Date.valueOf(LocalDate.now()).toString(), pessoaSalva.getData_cadastro().toString());

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(pessoaSalva.getData_nascimento());
        assertEquals(2000, cal.get(java.util.Calendar.YEAR));
        assertEquals(0, cal.get(java.util.Calendar.MONTH));
        assertEquals(1, cal.get(java.util.Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("Cadastro Atualização: Deve preservar IDs")
    void testCadastrarAtualizacaoIds() throws ParseException {
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());
        when(telefoneService.cadastrar(any())).thenReturn(new Telefone());

        service.cadastrar(10L, "Nome", "Ap", "CPF", "01/01/2000", "Obs",
                20L, 50L, "R", "B", "1", "C", "Ref",
                30L, "F", "CELULAR", redirectAttributes);

        verify(enderecoService).cadastrar(enderecoCaptor.capture());
        assertEquals(20L, enderecoCaptor.getValue().getCodigo());

        verify(telefoneService).cadastrar(telefoneCaptor.capture());
        assertEquals(30L, telefoneCaptor.getValue().getCodigo());
        assertEquals(TelefoneTipo.CELULAR, telefoneCaptor.getValue().getTipo());

        verify(repository).save(pessoaCaptor.capture());
        assertEquals(10L, pessoaCaptor.getValue().getCodigo());
    }

    @Test
    @DisplayName("Erro: CPF Duplicado (Novo Cadastro)")
    void testErroCpfDuplicado() {
        when(repository.findByCpfcnpjContaining("123")).thenReturn(new Pessoa());
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.cadastrar(0L, "N", "A", "123", "01/01/2000", "O", 
                    0L, 1L, "R", "B", "1", "C", "Ref", 0L, "F", "FIXO", redirectAttributes);
        });
        
        assertEquals("Já existe uma pessoa cadastrada com este CPF/CNPJ, verifique", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Sucesso: CPF Existente (Atualização)")
    void testSucessoCpfExistenteNaAtualizacao() throws ParseException {
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());
        when(telefoneService.cadastrar(any())).thenReturn(new Telefone());
        
        lenient().when(repository.findByCpfcnpjContaining("123")).thenReturn(new Pessoa());

        service.cadastrar(10L, "N", "A", "123", "01/01/2000", "O", 
                0L, 1L, "R", "B", "1", "C", "Ref", 0L, "F", "FIXO", redirectAttributes);

        verify(repository).save(any());
    }

    @Test
    @DisplayName("Lógica Default Telefone: Qualquer coisa vira CELULAR")
    void testTelefoneDefault() throws ParseException {
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());
        when(telefoneService.cadastrar(any())).thenReturn(new Telefone());

        service.cadastrar(0L, "N", "A", "C", "01/01/2000", "O", 
                0L, 1L, "R", "B", "1", "C", "Ref", 
                0L, "F", "BATATA", redirectAttributes);

        verify(telefoneService).cadastrar(telefoneCaptor.capture());
        assertEquals(TelefoneTipo.CELULAR, telefoneCaptor.getValue().getTipo());
    }

    @Test
    @DisplayName("Exception: Captura e relança RuntimeException customizada")
    void testTratamentoExcecao() throws ParseException {
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());
        when(telefoneService.cadastrar(any())).thenReturn(new Telefone());
        doThrow(new RuntimeException("DB Error")).when(repository).save(any());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.cadastrar(0L, "N", "A", "C", "01/01/2000", "O", 
                    0L, 1L, "R", "B", "1", "C", "Ref", 0L, "F", "FIXO", redirectAttributes);
        });

        assertEquals("Erro ao tentar cadastrar pessoa, chame o suporte", ex.getMessage());
    }

    @Test
    @DisplayName("Parse Date: Formato inválido")
    void testDataInvalida() {
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());

        assertThrows(ParseException.class, () -> {
            service.cadastrar(0L, "N", "A", "C", "DATA-ERRADA", "O", 
                    0L, 1L, "R", "B", "1", "C", "Ref", 0L, "F", "FIXO", redirectAttributes);
        });
    }
}