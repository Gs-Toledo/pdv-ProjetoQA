package net.originmobi.pdv.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
import net.originmobi.pdv.utilitarios.DataAtual;

@Service
public class PagarService {

    private static final Logger logger = LoggerFactory.getLogger(PagarService.class);

    private final PagarRepository pagarRepo;
    private final PagarParcelaService pagarParcelaServ;
    private final FornecedorService fornecedores;
    private final CaixaService caixas;
    private final UsuarioService usuarios;
    private final CaixaLancamentoService lancamentos;

    public PagarService(PagarRepository pagarRepo, PagarParcelaService pagarParcelaServ,
                        FornecedorService fornecedores, CaixaService caixas, 
                        UsuarioService usuarios, CaixaLancamentoService lancamentos) {
        this.pagarRepo = pagarRepo;
        this.pagarParcelaServ = pagarParcelaServ;
        this.fornecedores = fornecedores;
        this.caixas = caixas;
        this.usuarios = usuarios;
        this.lancamentos = lancamentos;
    }

    public List<Pagar> listar() {
        return pagarRepo.findAll();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String cadastrar(Long codFornecedor, Double valor, String obs, LocalDate vencimento, PagarTipo tipo) {
        LocalDate dataAtual = LocalDate.now();
        DataAtual dataTime = new DataAtual();

        String observacaoFinal = (obs == null || obs.isEmpty()) ? tipo.getDescricao() : obs;

        Fornecedor fornecedor = fornecedores.busca(codFornecedor)
                .orElseThrow(() -> new RuntimeException("Fornecedor não encontrado"));

        Pagar pagar = new Pagar(observacaoFinal, valor, dataAtual, fornecedor, tipo);

        try {
            pagarRepo.save(pagar);
        } catch (Exception e) {
            logger.error("Erro ao salvar despesa", e);
            throw new RuntimeException("Erro ao lançar despesa, chame o suporte", e);
        }

        try {
            pagarParcelaServ.cadastrar(valor, valor, 0, dataTime.dataAtualTimeStamp(), vencimento, pagar);
        } catch (Exception e) {
            logger.error("Erro ao cadastrar parcela", e);
            throw new RuntimeException("Erro ao lançar despesa (parcela), chame o suporte", e);
        }

        return "Despesa lançada com sucesso";
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public String quitar(Long codparcela, Double vlPago, Double vldesc, Double vlacre, Long codCaixa) {

        PagarParcela parcela = pagarParcelaServ.busca(codparcela)
                .orElseThrow(() -> new RuntimeException("Parcela não encontrada"));

        Double valorRestanteNaParcela = round(parcela.getValor_restante());
        
        if (vlPago > valorRestanteNaParcela) {
            throw new RuntimeException("Valor de pagamento inválido. O valor pago excede o restante.");
        }

        Double vlPagoAtual = parcela.getValor_pago() != null ? parcela.getValor_pago() : 0.0;
        Double vlDescontoAtual = parcela.getValor_desconto() != null ? parcela.getValor_desconto() : 0.0;
        Double vlAcrescimoAtual = parcela.getValor_acrescimo() != null ? parcela.getValor_acrescimo() : 0.0;

        Double vlquitado = (vlPago + vlacre) + vlPagoAtual;
        Double novoVlRestante = valorRestanteNaParcela - (vlPago + vldesc);
        Double vlDesconto = vlDescontoAtual + vldesc;
        Double vlAcrescimo = vlAcrescimoAtual + vlacre;

        novoVlRestante = novoVlRestante < 0 ? 0.0 : round(novoVlRestante);

        int quitado = novoVlRestante <= 0 ? 1 : 0;

        DataAtual dataAtual = new DataAtual();

        parcela.setValor_pago(vlquitado);
        parcela.setValor_restante(novoVlRestante);
        parcela.setValor_desconto(vlDesconto);
        parcela.setValor_acrescimo(vlAcrescimo);
        parcela.setQuitado(quitado);
        parcela.setData_pagamento(dataAtual.dataAtualTimeStamp());

        try {
            pagarParcelaServ.merger(parcela);
        } catch (Exception e) {
            logger.error("Erro ao realizar merge da parcela", e);
            throw new RuntimeException("Ocorreu um erro ao realizar o pagamento, chame o suporte", e);
        }

        Aplicacao aplicacao = Aplicacao.getInstancia();
        Usuario usuario = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());
        
        Caixa caixa = caixas.busca(codCaixa)
                .orElseThrow(() -> new RuntimeException("Caixa não encontrado"));

        if ((vlPago + vlacre) > caixa.getValor_total()) {
            throw new RuntimeException("Saldo insuficiente para realizar este pagamento");
        }

        try {
            CaixaLancamento lancamento = new CaixaLancamento("Referente a pagamento de despesas", 
                    vlPago + vlacre, TipoLancamento.PAGAMENTO, EstiloLancamento.SAIDA, caixa, usuario);

            lancamento.setParcelaPagar(parcela);
            lancamentos.lancamento(lancamento);
            
        } catch (Exception e) {
            logger.error("Erro ao realizar lançamento no caixa", e);
            throw new RuntimeException("Ocorreu um erro ao realizar o pagamento no caixa, chame o suporte", e);
        }

        return "Pagamento realizado com sucesso";
    }

    private Double round(Double value) {
        if (value == null) return 0.0;
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}