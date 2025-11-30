package net.originmobi.pdv.service.notafiscal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.originmobi.pdv.enumerado.notafiscal.NotaFiscalTipo;
import net.originmobi.pdv.model.Empresa;
import net.originmobi.pdv.model.FreteTipo;
import net.originmobi.pdv.model.NotaFiscal;
import net.originmobi.pdv.model.NotaFiscalFinalidade;
import net.originmobi.pdv.model.NotaFiscalTotais;
import net.originmobi.pdv.model.Pessoa;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalRepository;
import net.originmobi.pdv.service.EmpresaService;
import net.originmobi.pdv.service.PessoaService;
import net.originmobi.pdv.xml.nfe.GeraXmlNfe;

@Service
public class NotaFiscalService {

	@Autowired
	private NotaFiscalRepository notasFiscais;

	@Autowired
	private EmpresaService empresas;

	@Autowired
	private NotaFiscalTotaisServer notaTotais;

	@Autowired
	private PessoaService pessoas;

	@Autowired
	private GeraXmlNfe geraXmlNfe;

	@Value("${nfe.xml.path:/tmp/nfe}")
	private String CAMINHO_XML;

	public List<NotaFiscal> lista() {
		return notasFiscais.findAll();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String cadastrar(Long coddesti, String natureza, NotaFiscalTipo tipo) {
		Empresa empresa = empresas.verificaEmpresaCadastrada()
				.orElseThrow(() -> new RuntimeException("Nenhuma empresa cadastrada, verifique"));

		Pessoa pessoa = pessoas.buscaPessoa(coddesti)
				.orElseThrow(() -> new RuntimeException("Favor, selecione o destinatário"));

		if (empresa.getParametro().getSerie_nfe() == 0) {
			throw new RuntimeException("Não existe série cadastrada para o modelo 55, verifique");
		}

		// Prepara objetos auxiliares
		FreteTipo frete = new FreteTipo();
		frete.setCodigo(4L);

		NotaFiscalFinalidade finalidade = new NotaFiscalFinalidade();
		finalidade.setCodigo(1L);

		// Dados fixos e do parâmetro
		int modelo = 55;
		int serie = empresa.getParametro().getSerie_nfe();
		int tipoEmissao = 1;
		int tipoAmbiente = empresa.getParametro().getAmbiente();
		String verProc = "0.0.1-beta";
		Date cadastro = Date.valueOf(LocalDate.now());

		// Cadastra totais (Propaga erro se falhar, sem try-catch desnecessário)
		NotaFiscalTotais totais = new NotaFiscalTotais(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		try {
			notaTotais.cadastro(totais);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao cadastrar a nota, chame o suporte", e);
		}

		// Cadastra nota
		try {
			Long numeroNota = notasFiscais.buscaUltimaNota(serie);
			NotaFiscal notaFiscal = new NotaFiscal(numeroNota, modelo, tipo, natureza, serie, empresa,
					pessoa, tipoEmissao, verProc, frete, finalidade, totais, tipoAmbiente, cadastro);

			NotaFiscal notaSalva = notasFiscais.save(notaFiscal);
			return notaSalva.getCodigo().toString();

		} catch (Exception e) {
			throw new RuntimeException("Erro ao cadastrar a nota, chame o suporte", e);
		}
	}

	public Integer geraDV(String codigo) {
		if (codigo == null || codigo.isEmpty())
			return 0;

		// Remove caracteres não numéricos para evitar erro de parse
		if (!codigo.matches("\\d+"))
			return 0;

		int total = 0;
		int peso = 2;

		for (int i = 0; i < codigo.length(); i++) {
			total += (codigo.charAt((codigo.length() - 1) - i) - '0') * peso;
			peso++;
			if (peso == 10) {
				peso = 2;
			}
		}
		int resto = total % 11;
		return (resto == 0 || resto == 1) ? 0 : (11 - resto);
	}

	public void salvaXML(String xml, String chaveNfe) {
		try {
			Path arquivoDestino = resolveArquivo(chaveNfe);

			// Garante diretórios
			File dirFile = arquivoDestino.getParent().toFile();
			if (!dirFile.exists()) {
				if (!dirFile.mkdirs()) {
					throw new IOException("Falha ao criar diretório: " + dirFile.getAbsolutePath());
				}
			}

			// Escreve arquivo (Try-with-resources fecha automaticamente)
			try (PrintWriter out = new PrintWriter(new FileWriter(arquivoDestino.toFile()))) {
				out.write(xml);
			}

			System.out.println("Arquivo gravado com sucesso em " + arquivoDestino.toString());

		} catch (IOException e) {
			// Converte erro de IO checado em não-checado para não sujar a assinatura,
			// mas PERMITE que o erro suba para ser testado.
			throw new RuntimeException("Erro ao salvar XML", e);
		}
	}

	public void removeXml(String chave_acesso) {
		try {
			Path arquivo = resolveArquivo(chave_acesso);
			System.out.println("XML para deletar " + arquivo.toString());
			Files.deleteIfExists(arquivo);
		} catch (IOException e) {
			System.out.println("Erro ao deletar XML " + e);
		}
	}

	// Método auxiliar para evitar duplicação de lógica de caminho
	private Path resolveArquivo(String nomeArquivo) throws IOException {

		if (CAMINHO_XML == null) {
			throw new IOException("Caminho do XML não configurado (null)");
		}

		Path diretorioBase;
		if (Paths.get(CAMINHO_XML).isAbsolute()) {
			diretorioBase = Paths.get(CAMINHO_XML);
		} else {
			String contexto = new File(".").getCanonicalPath();
			diretorioBase = Paths.get(contexto, CAMINHO_XML);
		}
		return diretorioBase.resolve(nomeArquivo + ".xml");
	}

	public Optional<NotaFiscal> busca(Long codnota) {
		return notasFiscais.findById(codnota);
	}

	public void emitir(NotaFiscal notaFiscal) {
		String chaveNfe = geraXmlNfe.gerarXML(notaFiscal);
		notaFiscal.setChave_acesso(chaveNfe);
		notasFiscais.save(notaFiscal);
	}

	public int totalNotaFiscalEmitidas() {
		return notasFiscais.totalNotaFiscalEmitidas();
	}
}