package net.originmobi.pdv.service.cartao;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.originmobi.pdv.enumerado.TituloTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.enumerado.cartao.CartaoSituacao;
import net.originmobi.pdv.enumerado.cartao.CartaoTipo;
import net.originmobi.pdv.filter.CartaoFilter;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Titulo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.model.cartao.CartaoLancamento;
import net.originmobi.pdv.model.cartao.MaquinaCartao;
import net.originmobi.pdv.repository.cartao.CartaoLancamentoRepository;
import net.originmobi.pdv.service.CaixaLancamentoService;
import net.originmobi.pdv.service.UsuarioService;
import net.originmobi.pdv.singleton.Aplicacao;
import net.originmobi.pdv.utilitarios.DataAtual;

@Service
public class CartaoLancamentoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CartaoLancamentoService.class);

	@Autowired
	private CartaoLancamentoRepository repository;

	@Autowired
	private CaixaLancamentoService caixaLancamentos;

	@Autowired
	private UsuarioService usuarios;


	public void lancamento(Double vlParcela, Optional<Titulo> titulo) {
		Double taxa = 0.0;
		Double vlTaxa;
		Double vlLiqParcela;


		Double taxaAnte;
		Double vlTaxaAnte;
		Double vlLiqAnt;

		CartaoTipo tipo = null;
		int dias = 0;

		Titulo tituloObjeto = titulo.orElseThrow( () -> new RuntimeException("Título de cartão necessário para realizar o lançamento."));

		// verifica se é debito ou crédito e pega os valores corretos do titulo
		if (tituloObjeto.getTipo().getSigla().equals(TituloTipo.CARTDEB.toString())) { // Correção aplicada
			taxa = tituloObjeto.getMaquina().getTaxa_debito();
			dias = tituloObjeto.getMaquina().getDias_debito();
			tipo = CartaoTipo.DEBITO;

		} else if (tituloObjeto.getTipo().getSigla().equals(TituloTipo.CARTCRED.toString())) {
			taxa = tituloObjeto.getMaquina().getTaxa_credito();
			dias = tituloObjeto.getMaquina().getDias_credito();
			tipo = CartaoTipo.CREDITO;
		}

		vlTaxa= (vlParcela * taxa) / 100;
		vlLiqParcela = vlParcela - vlTaxa;

		taxaAnte = tituloObjeto.getMaquina().getTaxa_antecipacao();
		vlTaxaAnte = (vlParcela * taxaAnte) / 100;
		vlLiqAnt = vlParcela - vlTaxaAnte;

		MaquinaCartao maquinaCartao = tituloObjeto.getMaquina();

		DataAtual data = new DataAtual();
		LocalDate dataAtual = LocalDate.now();
		String dataRecebimento = data.DataAtualIncrementa(dias);

		CartaoLancamento lancamento = new CartaoLancamento(
				vlParcela,
				taxa,
				vlTaxa,
				vlLiqParcela,
				taxaAnte,
				vlTaxaAnte,
				vlLiqAnt,
				maquinaCartao,
				tipo,
				CartaoSituacao.APROCESSAR,
				Date.valueOf(dataRecebimento),
				Date.valueOf(dataAtual)
		);

		try {
			repository.save(lancamento);
		} catch (Exception e) {
			LOGGER.error("Erro ao salvar lançamento de cartão: {}", e.getMessage(), e);
		}
	}

public List<CartaoLancamento> listar(CartaoFilter filter) {
		String situacao = filter.getSituacao() == null ? "%" : filter.getSituacao().toString();
		String tipo = filter.getTipo() == null ? "%" : filter.getTipo().toString();
		String dataRecebimento = filter.getData_recebimento() == null || filter.getData_recebimento().isEmpty() ? "%"
				: filter.getData_recebimento().replace("/", "-");
		return repository.buscaLancamentos(situacao, tipo, dataRecebimento);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String processar(CartaoLancamento cartaoLancamento) {

		if (cartaoLancamento.getSituacao().equals(CartaoSituacao.PROCESSADO))
			throw new RuntimeException("Registro já processado");

		if (cartaoLancamento.getSituacao().equals(CartaoSituacao.ANTECIPADO))
			throw new RuntimeException("Registro já foi antecipado");

		Double valor = cartaoLancamento.getVlLiqParcela();
		TipoLancamento tipo = TipoLancamento.RECEBIMENTO;
		EstiloLancamento estilo = EstiloLancamento.ENTRADA;
		Caixa banco = cartaoLancamento.getMaquina_cartao().getBanco();

		Aplicacao aplicacao = Aplicacao.getInstancia();
		Usuario usuario = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());

		CaixaLancamento lancamento = new CaixaLancamento("Referênte a processamento de cartão", valor, tipo, estilo,
				banco, usuario);

		try {
			caixaLancamentos.lancamento(lancamento);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao tentar realizar o processamento, chame o suporte");
		}

		try {
			cartaoLancamento.setSituacao(CartaoSituacao.PROCESSADO);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao tentar realizar o processamento, chame o suporte");
		}

		return "Processamento realizado com sucesso";
	}

	public String antecipar(CartaoLancamento cartaoLancamento) {
		if (cartaoLancamento.getSituacao().equals(CartaoSituacao.PROCESSADO))
			throw new RuntimeException("Registro já processado");

		if (cartaoLancamento.getSituacao().equals(CartaoSituacao.ANTECIPADO))
			throw new RuntimeException("Registro já foi antecipado");

		Double valor = cartaoLancamento.getVlLiqAntecipacao();
		TipoLancamento tipo = TipoLancamento.RECEBIMENTO;
		EstiloLancamento estilo = EstiloLancamento.ENTRADA;
		Caixa banco = cartaoLancamento.getMaquina_cartao().getBanco();

		Aplicacao aplicacao = Aplicacao.getInstancia();
		Usuario usuario = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());

		CaixaLancamento lancamento = new CaixaLancamento(
				"Referênte a antecipação de cartão código " + cartaoLancamento.getCodigo(), valor, tipo, estilo, banco,
				usuario);

		try {
			caixaLancamentos.lancamento(lancamento);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao tentar realizar a antecipação, chame o suporte");
		}

		try {
			cartaoLancamento.setSituacao(CartaoSituacao.ANTECIPADO);
			repository.save(cartaoLancamento);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao tentar realizar a antecipação, chame o suporte");
		}

		return "Antecipação realizada com sucesso";
	}

}
