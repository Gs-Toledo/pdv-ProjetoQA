package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    @DisplayName("Deve retornar lista completa de pessoas")
    void testLista() {
        // Arrange
        Pessoa p1 = new Pessoa();
        when(repository.findAll()).thenReturn(Arrays.asList(p1));

        // Act
        List<Pessoa> resultado = service.lista();

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar pessoa por código In")
    void testBusca() {
        // Arrange
        Long codigo = 1L;
        Pessoa pessoaMock = new Pessoa();
        when(repository.findByCodigoIn(codigo)).thenReturn(pessoaMock);

        // Act
        Pessoa resultado = service.busca(codigo);

        // Assert
        assertNotNull(resultado);
        verify(repository).findByCodigoIn(codigo);
    }

    @Test
    @DisplayName("Deve retornar Optional de pessoa pelo ID")
    void testBuscaPessoa() {
        // Arrange
        Long codigo = 1L;
        when(repository.findById(codigo)).thenReturn(Optional.of(new Pessoa()));

        // Act
        Optional<Pessoa> resultado = service.buscaPessoa(codigo);

        // Assert
        assertTrue(resultado.isPresent());
        verify(repository).findById(codigo);
    }

    @Test
    @DisplayName("Deve filtrar por nome quando filtro possui valor")
    void testFilterComNome() {
        // Arrange
        PessoaFilter filter = new PessoaFilter();
        filter.setNome("Maria");
        when(repository.findByNomeContaining("Maria")).thenReturn(new ArrayList<>());

        // Act
        service.filter(filter);

        // Assert
        verify(repository).findByNomeContaining("Maria");
    }

    @Test
    @DisplayName("Deve filtrar com '%' quando nome no filtro é nulo")
    void testFilterComNomeNulo() {
        // Arrange
        PessoaFilter filter = new PessoaFilter();
        filter.setNome(null); // Cenário de teste
        when(repository.findByNomeContaining("%")).thenReturn(new ArrayList<>());

        // Act
        service.filter(filter);

        // Assert
        verify(repository).findByNomeContaining("%");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar cadastrar nova pessoa com CPF já existente")
    void testCadastrarCpfDuplicado() throws ParseException {
        // Arrange
        String cpf = "123.456.789-00";
        // Mocka o retorno indicando que já existe alguém com esse CPF
        when(repository.findByCpfcnpjContaining(cpf)).thenReturn(new Pessoa());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.cadastrar(0L, "Nome", "Apelido", cpf, "01/01/2000", "Obs", 
                    1L, 1L, "Rua", "Bairro", "123", "CEP", "Ref", 1L, "Fone", "FIXO", redirectAttributes);
        });

        assertEquals("Já existe uma pessoa cadastrada com este CPF/CNPJ, verifique", exception.getMessage());
        verify(repository, never()).save(any(Pessoa.class));
    }

    @Test
    @DisplayName("Deve cadastrar nova pessoa com sucesso (Caminho Feliz)")
    void testCadastrarSucesso() throws ParseException {
        // Arrange
        Long codPessoa = 0L; // 0 indica novo cadastro
        String dataNascimento = "10/05/1990";
        String tipoTelefone = "FIXO";
        
        // Mock das dependências necessárias para montar o objeto
        Cidade cidadeMock = new Cidade();
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(cidadeMock));
        
        Endereco enderecoSalvo = new Endereco();
        when(enderecoService.cadastrar(any(Endereco.class))).thenReturn(enderecoSalvo);
        
        Telefone telefoneSalvo = new Telefone();
        when(telefoneService.cadastrar(any(Telefone.class))).thenReturn(telefoneSalvo);
        
        when(repository.findByCpfcnpjContaining(anyString())).thenReturn(null); // CPF não existe

        // Act
        String resultado = service.cadastrar(codPessoa, "João Silva", "João", "111.222.333-44", dataNascimento, 
                "Observacao", 10L, 20L, "Rua A", "Centro", "10", "99999-000", "Perto da praça", 
                5L, "3333-4444", tipoTelefone, redirectAttributes);

        // Assert
        assertEquals("Pessoa salva com sucesso", resultado);
        
        // Verifica se o save foi chamado e captura o argumento para validações extras
        verify(repository).save(argThat(pessoa -> {
            return pessoa.getNome().equals("João Silva") && 
                   pessoa.getEndereco().equals(enderecoSalvo) &&
                   pessoa.getTelefone().size() == 1 &&
                   pessoa.getData_nascimento() != null; 
        }));
    }
    
    @Test
    @DisplayName("Deve atualizar pessoa existente (codpessoa != 0) e definir tipo Celular")
    void testAtualizarPessoaCelular() throws ParseException {
        // Arrange
        Long codPessoa = 50L; // ID existente
        String tipoTelefone = "CELULAR"; // Testando lógica do ternário
        
        Cidade cidadeMock = new Cidade();
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(cidadeMock));
        when(enderecoService.cadastrar(any(Endereco.class))).thenReturn(new Endereco());
        when(telefoneService.cadastrar(any(Telefone.class))).thenReturn(new Telefone());

        // Act
        service.cadastrar(codPessoa, "Maria Souza", "Maria", "999.888.777-66", "20/02/1995", 
                "Obs", 1L, 1L, "Rua B", "Bairro", "20", "CEP", "Ref", 1L, "9999-8888", tipoTelefone, redirectAttributes);

        // Assert
        // Verifica se o TelefoneTipo foi setado corretamente como CELULAR (devido ao ternário no service)
        verify(telefoneService).cadastrar(argThat(fone -> fone.getTipo().equals(TelefoneTipo.CELULAR)));
        
        // Verifica se o ID foi setado na pessoa para atualização
        verify(repository).save(argThat(pessoa -> pessoa.getCodigo().equals(50L)));
    }

    @Test
    @DisplayName("Deve lançar RuntimeException customizada quando o repositório falhar ao salvar")
    void testErroAoSalvar() throws ParseException {
        // Arrange
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any(Endereco.class))).thenReturn(new Endereco());
        when(telefoneService.cadastrar(any(Telefone.class))).thenReturn(new Telefone());
        
        // Simula erro genérico no banco
        doThrow(new RuntimeException("Database error")).when(repository).save(any(Pessoa.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.cadastrar(0L, "Teste", "T", "000", "01/01/2000", "Obs", 
                    1L, 1L, "R", "B", "1", "C", "R", 1L, "F", "FIXO", redirectAttributes);
        });

        assertEquals("Erro ao tentar cadastrar pessoa, chame o suporte", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção quando cidade não é encontrada")
    void testCadastrarCidadeNaoEncontrada() throws ParseException {
        // Arrange
        when(cidadeService.busca(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> {
            service.cadastrar(0L, "João", "J", "111", "01/01/2000", "Obs",
                    1L, 1L, "Rua", "Bairro", "1", "Cep", "Ref",
                    1L, "98888-1111", "FIXO", redirectAttributes);
        });

        assertNotNull(ex);
        verify(repository, never()).save(any(Pessoa.class));
    }

    @Test
    @DisplayName("Tipo de telefone inválido deve cair no default CELULAR")
    void testCadastrarTelefoneTipoInvalido() throws ParseException {
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());
        when(telefoneService.cadastrar(any())).thenReturn(new Telefone());

        service.cadastrar(0L, "Teste", "T", "111", "01/01/2000", "Obs",
                1L, 1L, "Rua", "Bairro", "1", "CEP", "Ref",
                1L, "99999-0000", "SATELITE", redirectAttributes);

        verify(telefoneService).cadastrar(argThat(f -> f.getTipo().equals(TelefoneTipo.CELULAR)));
    }


    @Test
    @DisplayName("Deve lançar ParseException quando data de nascimento é inválida")
    void testCadastrarDataInvalida() {
        // Permite chegar até o parse
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());

        String dataInvalida = "31-02-2020";

        assertThrows(ParseException.class, () -> {
            service.cadastrar(0L, "Teste", "T", "111", dataInvalida, "Obs",
                    1L, 1L, "Rua", "Bairro", "1", "CEP", "Ref",
                    1L, "9999", "FIXO", redirectAttributes);
        });
    }

    @Test
    @DisplayName("Deve cadastrar pessoa mesmo sem telefone")
    void testCadastrarSemTelefone() throws ParseException {
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());
        when(repository.findByCpfcnpjContaining(anyString())).thenReturn(null);

        String retorno = service.cadastrar(0L, "Ana", "A", "111", "01/01/1990", "Obs",
                1L, 1L, "Rua", "Bairro", "1", "CEP", "Ref",
                1L, "", "FIXO", redirectAttributes);

        assertEquals("Pessoa salva com sucesso", retorno);
        verify(repository).save(any(Pessoa.class));
    }

    @Test
    @DisplayName("Deve atualizar pessoa mantendo telefone existente")
    void testAtualizarMantendoTelefone() throws ParseException {
        Pessoa pessoaExistente = new Pessoa();
        pessoaExistente.setCodigo(10L);
        Telefone telefone = new Telefone();
        pessoaExistente.setTelefone(Arrays.asList(telefone));

        // mocks necessários para chegar até o save
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(new Cidade()));
        when(enderecoService.cadastrar(any())).thenReturn(new Endereco());
        when(telefoneService.cadastrar(any())).thenReturn(telefone);

        service.cadastrar(10L, "Carlos", "C", "555", "10/10/1990", "Obs",
                1L, 1L, "Rua", "Bairro", "1", "CEP", "Ref",
                1L, "7777-8888", "FIXO", redirectAttributes);

        verify(repository).save(argThat(p -> p.getCodigo().equals(10L)));
    }


    @Test
    @DisplayName("Deve retornar Optional vazio quando pessoa não é encontrada")
    void testBuscaPessoaNaoEncontrada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<Pessoa> resultado = service.buscaPessoa(99L);

        assertFalse(resultado.isPresent());
    }

}