package net.originmobi.pdv.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.filter.BancoFilter;
import net.originmobi.pdv.filter.CaixaFilter;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import net.originmobi.pdv.singleton.Aplicacao;

@Service
public class CaixaService {

    private static final Logger logger = LoggerFactory.getLogger(CaixaService.class);

    @Autowired
    private CaixaRepository caixas;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private CaixaLancamentoService lancamentos;

    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public Long cadastro(Caixa caixa) {
        
        validarAbertura(caixa);

        Aplicacao aplicacao = Aplicacao.getInstancia();
        Usuario usuarioLogado = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());

        Double vlabertura = caixa.getValor_abertura() == null ? 0.0 : caixa.getValor_abertura();
        caixa.setValor_abertura(vlabertura);

        configurarDescricao(caixa);

        caixa.setUsuario(usuarioLogado);
        caixa.setData_cadastro(java.sql.Date.valueOf(LocalDate.now()));

        sanitizarDadosBanco(caixa);

        try {
            caixas.save(caixa);
        } catch (Exception e) {
            throw new IllegalStateException("Erro no processo de abertura, chame o suporte técnico", e);
        }

        processarLancamentoInicial(caixa, usuarioLogado);

        return caixa.getCodigo();
    }

    private void validarAbertura(Caixa caixa) {
        if (CaixaTipo.CAIXA.equals(caixa.getTipo()) && caixaIsAberto()) {
            throw new IllegalStateException("Existe caixa de dias anteriores em aberto, favor verifique");
        }
        if (caixa.getValor_abertura() != null && caixa.getValor_abertura() < 0) {
            throw new IllegalArgumentException("Valor informado é inválido");
        }
    }

    private void configurarDescricao(Caixa caixa) {
        if (caixa.getDescricao() != null && !caixa.getDescricao().isEmpty()) {
            return;
        }
        
        if (CaixaTipo.CAIXA.equals(caixa.getTipo())) {
            caixa.setDescricao("Caixa diário");
        } else if (CaixaTipo.COFRE.equals(caixa.getTipo())) {
            caixa.setDescricao("Cofre");
        } else if (CaixaTipo.BANCO.equals(caixa.getTipo())) {
            caixa.setDescricao("Banco");
        }
    }

    private void sanitizarDadosBanco(Caixa caixa) {
        if (CaixaTipo.BANCO.equals(caixa.getTipo())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Sanitizando dados bancários. Agência: {}, Conta: {}", caixa.getAgencia(), caixa.getConta());
            }
            if (caixa.getAgencia() != null) caixa.setAgencia(caixa.getAgencia().replaceAll("\\D", ""));
            if (caixa.getConta() != null) caixa.setConta(caixa.getConta().replaceAll("\\D", ""));
        }
    }

    private void processarLancamentoInicial(Caixa caixa, Usuario usuario) {
        if (caixa.getValor_abertura() > 0) {
            try {
                String observacao = gerarObservacaoLancamento(caixa.getTipo());
                CaixaLancamento lancamento = new CaixaLancamento(observacao, caixa.getValor_abertura(),
                        TipoLancamento.SALDOINICIAL, EstiloLancamento.ENTRADA, caixa, usuario);
                lancamentos.lancamento(lancamento);
            } catch (Exception e) {
                throw new IllegalStateException("Erro no processo de lançamento inicial, chame o suporte", e);
            }
        } else {
            caixa.setValor_total(0.0);
        }
    }

    private String gerarObservacaoLancamento(CaixaTipo tipo) {
        if (CaixaTipo.CAIXA.equals(tipo)) return "Abertura de caixa";
        if (CaixaTipo.COFRE.equals(tipo)) return "Abertura de cofre";
        return "Abertura de banco";
    }

    public String fechaCaixa(Long idCaixa, String senha) {
        Aplicacao aplicacao = Aplicacao.getInstancia();
        Usuario usuarioLogado = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (senha == null || senha.isEmpty()) {
            return "Favor, informe a senha";
        }

        if (encoder.matches(senha, usuarioLogado.getSenha())) {
            return processarFechamento(idCaixa);
        } else {
            return "Senha incorreta, favor verifique";
        }
    }

    private String processarFechamento(Long idCaixa) {
        Caixa caixaAtual = caixas.findById(idCaixa)
            .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado"));

        if (caixaAtual.getData_fechamento() != null) {
            throw new IllegalStateException("Caixa já está fechado");
        }

        Double valorTotal = caixaAtual.getValor_total() == null ? 0.0 : caixaAtual.getValor_total();

        caixaAtual.setData_fechamento(new Timestamp(System.currentTimeMillis()));
        caixaAtual.setValor_fechamento(valorTotal);

        try {
            caixas.save(caixaAtual);
            return "Caixa fechado com sucesso";
        } catch (Exception e) {
            throw new IllegalStateException("Ocorreu um erro ao fechar o caixa, chame o suporte", e);
        }
    }

    public boolean caixaIsAberto() {
        return caixas.caixaAberto().isPresent();
    }

    public List<Caixa> listaTodos() {
        return caixas.findByCodigoOrdenado();
    }

    public List<Caixa> listarCaixas(CaixaFilter filter) {
        if (filter.getData_cadastro() != null && !filter.getData_cadastro().isEmpty()) {
            String dataFormatada = filter.getData_cadastro().replace("/", "-");
            return caixas.buscaCaixasPorDataAbertura(Date.valueOf(dataFormatada));
        }
        return caixas.listaCaixasAbertos();
    }

    public Optional<Caixa> caixaAberto() {
        return caixas.caixaAberto();
    }

    public List<Caixa> caixasAbertos() {
        return caixas.caixasAbertos();
    }

    public Optional<Caixa> busca(Long codigo) {
        return caixas.findById(codigo);
    }

    public Optional<Caixa> buscaCaixaUsuario(String nomeUsuario) {
        Usuario usu = usuarios.buscaUsuario(nomeUsuario);
        return Optional.ofNullable(caixas.findByCaixaAbertoUsuario(usu.getCodigo()));
    }

    public List<Caixa> listaBancos() {
        return caixas.buscaBancos(CaixaTipo.BANCO);
    }

    public List<Caixa> listaCaixasAbertosTipo(CaixaTipo tipo) {
        return caixas.buscaCaixaTipo(tipo);
    }

    public List<Caixa> listaBancosAbertosTipoFilterBanco(CaixaTipo tipo, BancoFilter filter) {
        if (filter.getData_cadastro() != null && !filter.getData_cadastro().isEmpty()) {
            String dataFormatada = filter.getData_cadastro().replace("/", "-");
            return caixas.buscaCaixaTipoData(tipo, Date.valueOf(dataFormatada));
        }
        return caixas.buscaCaixaTipo(CaixaTipo.BANCO);
    }
}