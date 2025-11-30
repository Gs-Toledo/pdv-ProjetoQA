package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import net.originmobi.pdv.enumerado.notafiscal.NotaFiscalTipo;
import net.originmobi.pdv.model.Empresa;
import net.originmobi.pdv.model.EmpresaParametro;
import net.originmobi.pdv.model.NotaFiscal;
import net.originmobi.pdv.model.NotaFiscalTotais;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalRepository;
import net.originmobi.pdv.service.notafiscal.NotaFiscalService;
import net.originmobi.pdv.service.notafiscal.NotaFiscalTotaisServer;
import net.originmobi.pdv.xml.nfe.GeraXmlNfe;

@ExtendWith(MockitoExtension.class)
class NotaFiscalServiceTest {

    @Mock
    private NotaFiscalRepository notasFiscais;
    @Mock
    private EmpresaService empresas;
    @Mock
    private NotaFiscalTotaisServer notaTotais;
    @Mock
    private PessoaService pessoas;
    @Mock
    private GeraXmlNfe geraXmlNfe;

    @InjectMocks
    private NotaFiscalService service;

    @Captor
    private ArgumentCaptor<NotaFiscal> notaFiscalCaptor;

    @Captor
    private ArgumentCaptor<NotaFiscalTotais> totaisCaptor;

    // --- TESTES DE LISTAGEM E BUSCA ---

    @Test
    @DisplayName("Lista: Deve retornar lista vazia se repositório vazio")
    void lista_DeveRetornarVazio() {
        when(notasFiscais.findAll()).thenReturn(Collections.emptyList());
        List<NotaFiscal> resultado = service.lista();
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Lista: Deve retornar lista populada")
    void lista_DeveRetornarListaDeNotas() {
        NotaFiscal nf = new NotaFiscal();
        when(notasFiscais.findAll()).thenReturn(Arrays.asList(nf));
        List<NotaFiscal> resultado = service.lista();
        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("Busca: Deve retornar Optional vazio se não existir")
    void busca_DeveRetornarVazioSeNaoExistir() {
        when(notasFiscais.findById(anyLong())).thenReturn(Optional.empty());
        Optional<NotaFiscal> resultado = service.busca(1L);
        assertFalse(resultado.isPresent());
    }

    @Test
    @DisplayName("Busca: Deve retornar nota se existir")
    void busca_DeveRetornarNotaQuandoExistir() {
        NotaFiscal nf = new NotaFiscal();
        when(notasFiscais.findById(1L)).thenReturn(Optional.of(nf));
        Optional<NotaFiscal> resultado = service.busca(1L);
        assertTrue(resultado.isPresent());

        // Verifica se chamou o repositório com o ID correto (Mata mutante que troca o
        // ID)
        verify(notasFiscais).findById(eq(1L));
    }

    // --- TESTES DO CADASTRAR ---

    @Test
    @DisplayName("Cadastrar: Sucesso - Valida fluxo completo, valores fixos e chamadas")
    void cadastrar_DeveValidarFluxoHappyPath() {
        Long codDestinatario = 50L;
        String naturezaOperacao = "Natureza Teste";
        NotaFiscalTipo tipoNota = NotaFiscalTipo.SAIDA;

        Empresa empresa = empresaValida(1, 2);
        Pessoa pessoa = new Pessoa();
        pessoa.setCodigo(codDestinatario);

        // 1. Configuração dos Mocks Estritos (usando eq() para garantir que os
        // argumentos são passados corretamente)
        when(empresas.verificaEmpresaCadastrada()).thenReturn(Optional.of(empresa));
        when(pessoas.buscaPessoa(eq(codDestinatario))).thenReturn(Optional.of(pessoa));
        when(notasFiscais.buscaUltimaNota(1)).thenReturn(100L);

        // Mock do save
        when(notasFiscais.save(any(NotaFiscal.class))).thenAnswer(inv -> {
            NotaFiscal nf = inv.getArgument(0);
            nf.setCodigo(123L);
            return nf;
        });

        // 2. Execução
        String resultado = service.cadastrar(codDestinatario, naturezaOperacao, tipoNota);

        // 3. Validações
        assertEquals("123", resultado);

        // Verifica Totais
        verify(notaTotais).cadastro(totaisCaptor.capture());
        NotaFiscalTotais totaisSalvos = totaisCaptor.getValue();
        assertEquals(0.0, totaisSalvos.getV_prod(), "Valor Prod deve ser 0.0");
        assertEquals(0.0, totaisSalvos.getV_desc(), "Valor Desc deve ser 0.0");

        // Verifica Nota Fiscal
        verify(notasFiscais).save(notaFiscalCaptor.capture());
        NotaFiscal notaSalva = notaFiscalCaptor.getValue();

        // Mata mutante: Verifica se o número foi pego do buscaUltimaNota
        assertEquals(100L, notaSalva.getNumero());

        // Mata mutante: Verifica se os parametros de entrada foram usados
        assertEquals(naturezaOperacao, notaSalva.getNatureza_operacao()); // Verifica se usou a String passada
        assertEquals(tipoNota, notaSalva.getTipo()); // Verifica se usou o Enum passado
        assertEquals(codDestinatario, notaSalva.getDestinatario().getCodigo()); // Verifica se vinculou a pessoa correta

        // Valores Hardcoded / Calculados
        assertEquals(55, ReflectionTestUtils.getField(notaSalva, "modelo"));
        assertEquals(1, ReflectionTestUtils.getField(notaSalva, "tipo_emissao"));
        assertEquals("0.0.1-beta", ReflectionTestUtils.getField(notaSalva, "verProc"));
        assertEquals(4L, notaSalva.getFreteTipo().getCodigo());
        assertEquals(1L, notaSalva.getFinalidade().getCodigo());
        assertEquals(2, ReflectionTestUtils.getField(notaSalva, "tipo_ambiente"));
    }

    @Test
    @DisplayName("Cadastrar: Deve lançar exceção se Empresa não existe")
    void cadastrar_SemEmpresa() {
        when(empresas.verificaEmpresaCadastrada()).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cadastrar(1L, "Venda", NotaFiscalTipo.SAIDA));
        assertEquals("Nenhuma empresa cadastrada, verifique", ex.getMessage());
    }

    @Test
    @DisplayName("Cadastrar: Deve lançar exceção se Pessoa não existe")
    void cadastrar_SemPessoa() {
        when(empresas.verificaEmpresaCadastrada()).thenReturn(Optional.of(empresaValida(1, 1)));
        when(pessoas.buscaPessoa(anyLong())).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cadastrar(1L, "Venda", NotaFiscalTipo.SAIDA));
        assertEquals("Favor, selecione o destinatário", ex.getMessage());
    }

    @Test
    @DisplayName("Cadastrar: Deve lançar exceção se Série for 0")
    void cadastrar_SerieZero() {
        when(empresas.verificaEmpresaCadastrada()).thenReturn(Optional.of(empresaValida(0, 1)));
        when(pessoas.buscaPessoa(anyLong())).thenReturn(Optional.of(new Pessoa()));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cadastrar(1L, "Venda", NotaFiscalTipo.SAIDA));
        assertEquals("Não existe série cadastrada para o modelo 55, verifique", ex.getMessage());
    }

    @Test
    @DisplayName("Cadastrar: Deve capturar erro ao salvar Totais e relançar RuntimeException")
    void cadastrar_ErroSalvarTotais() {
        when(empresas.verificaEmpresaCadastrada()).thenReturn(Optional.of(empresaValida(1, 1)));
        when(pessoas.buscaPessoa(anyLong())).thenReturn(Optional.of(new Pessoa()));

        doThrow(new RuntimeException("DB Error")).when(notaTotais).cadastro(any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cadastrar(1L, "Venda", NotaFiscalTipo.SAIDA));
        assertEquals("Erro ao cadastrar a nota, chame o suporte", ex.getMessage());
    }

    @Test
    @DisplayName("Cadastrar: Deve capturar erro ao salvar Nota e relançar RuntimeException")
    void cadastrar_ErroSalvarNota() {
        when(empresas.verificaEmpresaCadastrada()).thenReturn(Optional.of(empresaValida(1, 1)));
        when(pessoas.buscaPessoa(anyLong())).thenReturn(Optional.of(new Pessoa()));
        when(notasFiscais.buscaUltimaNota(anyInt())).thenReturn(1L);

        when(notasFiscais.save(any())).thenThrow(new RuntimeException("DB Error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cadastrar(1L, "Venda", NotaFiscalTipo.SAIDA));
        assertEquals("Erro ao cadastrar a nota, chame o suporte", ex.getMessage());
    }

    // --- TESTES DO GERA DV ---

    @Test
    @DisplayName("GeraDV: Resto 0 deve retornar 0")
    void geraDV_RestoZero() {
        assertEquals(0, service.geraDV("11111111"));
    }

    @Test
    @DisplayName("GeraDV: Deve resetar peso quando chegar a 10")
    void geraDV_ResetPeso() {
        String codigoLongo = "1234567890123";
        Integer resultado = service.geraDV(codigoLongo);
        assertNotNull(resultado);
        assertTrue(resultado >= 0 && resultado <= 9);
    }

    @Test
    void geraDV_ErroException() {
        assertEquals(0, service.geraDV(null));
        assertEquals(0, service.geraDV(""));
        assertEquals(0, service.geraDV("123A5")); // Teste novo de validação
    }

    @Test
    @DisplayName("GeraDV: Deve calcular corretamente digito verificador (Lógica Real)")
    void geraDV_CalculoReal() {
        assertEquals(9, service.geraDV("12345678"));
    }

    // --- TESTES DO XML ---

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("SalvaXML: Deve criar arquivo quando caminho é absoluto")
    void salvaXML_CaminhoAbsoluto() throws IOException {
        ReflectionTestUtils.setField(service, "CAMINHO_XML", tempDir.toAbsolutePath().toString());
        String chave = "NFeTestAbsoluto";
        String conteudo = "<xml>teste</xml>";

        service.salvaXML(conteudo, chave);

        Path arquivoEsperado = tempDir.resolve(chave + ".xml");
        assertTrue(Files.exists(arquivoEsperado));

        List<String> lines = Files.readAllLines(arquivoEsperado);
        String conteudoLido = String.join("", lines);
        assertEquals(conteudo, conteudoLido);
    }

    @Test
    @DisplayName("SalvaXML: Tenta usar caminho relativo (cobre o 'else' do isAbsolute)")
    void salvaXML_CaminhoRelativo() {
        // Define um caminho relativo. O código vai tentar resolver com
        // Paths.get(contexto, CAMINHO_XML)
        // Mesmo que falhe ao gravar no disco (permissão), o código passará pelo ELSE,
        // aumentando a cobertura
        ReflectionTestUtils.setField(service, "CAMINHO_XML", "temp_xml_relativo_test");

        // Não esperamos exceção, pois o método engole exceções
        assertDoesNotThrow(() -> service.salvaXML("<xml></xml>", "chaveRelativa"));

        // Limpeza (caso tenha criado arquivo na raiz do projeto)
        try {
            File f = new File("temp_xml_relativo_test/chaveRelativa.xml");
            if (f.exists())
                f.delete();
            File d = new File("temp_xml_relativo_test");
            if (d.exists())
                d.delete();
        } catch (Exception e) {
        }
    }

    @Test
    @DisplayName("SalvaXML: Deve lançar exceção em caso de erro IO")
    void salvaXML_Exception() {
        // null vai causar erro na criação do Path ou FileWriter
        ReflectionTestUtils.setField(service, "CAMINHO_XML", null);
        assertThrows(RuntimeException.class, () -> service.salvaXML("xml", "chave"));
    }

    @Test
    @DisplayName("RemoveXML: Deve apagar arquivo se existir (Caminho Absoluto)")
    void removeXml_Sucesso() throws IOException {
        ReflectionTestUtils.setField(service, "CAMINHO_XML", tempDir.toAbsolutePath().toString());
        String chave = "DeleteMe";
        Path arquivo = tempDir.resolve(chave + ".xml");
        Files.createFile(arquivo);

        assertTrue(Files.exists(arquivo));

        service.removeXml(chave);

        assertFalse(Files.exists(arquivo));
    }

    @Test
    @DisplayName("RemoveXML: Tenta remover com caminho relativo (cobre o 'else' do isAbsolute)")
    void removeXml_CaminhoRelativo() {
        ReflectionTestUtils.setField(service, "CAMINHO_XML", "temp_xml_relativo_del");
        assertDoesNotThrow(() -> service.removeXml("chaveInexistente"));
    }

    @Test
    @DisplayName("RemoveXML: Deve tratar erro silenciosamente")
    void removeXml_Erro() {
        ReflectionTestUtils.setField(service, "CAMINHO_XML", null);
        assertDoesNotThrow(() -> service.removeXml("qualquer_coisa"));
    }

    // --- OUTROS MÉTODOS ---

    @Test
    @DisplayName("Emitir: Deve gerar XML, atribuir chave e salvar")
    void emitir_FluxoCompleto() {
        NotaFiscal nf = new NotaFiscal();
        String chaveGerada = "CHAVE_ACESSO_MOCK";

        when(geraXmlNfe.gerarXML(nf)).thenReturn(chaveGerada);

        service.emitir(nf);

        assertEquals(chaveGerada, nf.getChave_acesso());
        verify(notasFiscais).save(nf);
    }

    @Test
    @DisplayName("TotalEmitidas: Deve repassar valor do repositório")
    void totalNotaFiscalEmitidas_Check() {
        when(notasFiscais.totalNotaFiscalEmitidas()).thenReturn(42);
        assertEquals(42, service.totalNotaFiscalEmitidas());
    }

    // --- Helpers ---

    private Empresa empresaValida(int serie, int ambiente) {
        Empresa empresa = new Empresa();
        EmpresaParametro p = new EmpresaParametro();
        p.setSerie_nfe(serie);
        p.setAmbiente(ambiente);
        empresa.setParametro(p);
        return empresa;
    }
}