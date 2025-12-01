package net.originmobi.pdv.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.originmobi.pdv.enumerado.ajuste.AjusteStatus;
import net.originmobi.pdv.utilitarios.RegraNegocioException;
import net.originmobi.pdv.model.Ajuste;
import net.originmobi.pdv.model.AjusteProduto;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.repository.AjusteProdutoRepository;

@Service
public class AjusteProdutoService {

    // Correção: Uso de Logger ao invés de System.out
    private static final Logger logger = LoggerFactory.getLogger(AjusteProdutoService.class);

    @Autowired
    private AjusteProdutoRepository ajusteprodutos;

    @Autowired
    private ProdutoService produtos;

    @Autowired
    private AjusteService ajustes;

    public List<AjusteProduto> listaProdutosAjuste(Long codAjuste) {
        return ajusteprodutos.findByAjusteCodigoEquals(codAjuste);
    }

    public int buscaProdAjust(Long codAjuste, Long codProd) {
        return ajusteprodutos.buscaProdAjuste(codAjuste, codProd);
    }

    // Correção: Nome do parâmetro qtd_alteracao para qtdAlteracao (camelCase)
    public String addProduto(Long codAjuste, Long codProd, int qtdAlteracao) {
        Optional<Ajuste> ajusteOpt = ajustes.busca(codAjuste);

        // Correção: Bug do Optional. Verifica se existe antes de pegar o valor.
        if (!ajusteOpt.isPresent()) {
            // Correção: Exception específica
            throw new RegraNegocioException("Ajuste não encontrado.");
        }

        Ajuste ajuste = ajusteOpt.get();

        if (AjusteStatus.PROCESSADO.equals(ajuste.getStatus())) {
            // Correção: Exception específica
            throw new RegraNegocioException("Ajuste já está processado");
        }

        Produto produto = produtos.busca(codProd);

        // Correção: Nome da variável estoque_aqual para estoqueAtual
        int estoqueAtual = produto.getEstoque().getQtd();

        int tem = buscaProdAjust(codAjuste, codProd);

        if (tem > 0) {
            throw new RegraNegocioException("Este produto já existe neste ajuste");
        }

        if (qtdAlteracao == 0) {
            throw new RegraNegocioException("Quantidade inválida");
        }

        // Correção: Nome da variável novo_estoque para novoEstoque
        int novoEstoque = estoqueAtual + qtdAlteracao;

        try {
            ajusteprodutos.insereProduto(codAjuste, codProd, estoqueAtual, qtdAlteracao, novoEstoque);
        } catch (Exception e) {
            // Correção: Logger ao invés de System.out e passando a exceção completa
            logger.error("Erro ao inserir produto no ajuste", e);
            throw new RegraNegocioException("Erro ao tentar inserir produto no ajuste, chame o suporte");
        }

        return "Ajuste processado com sucesso";
    }

    public String removeProduto(Long codAjuste, Long codItem) {
        Optional<Ajuste> ajusteOpt = ajustes.busca(codAjuste);

        // Correção: Bug do Optional
        if (!ajusteOpt.isPresent()) {
            throw new RegraNegocioException("Ajuste não encontrado.");
        }

        // Correção: Verificação segura do status
        if (AjusteStatus.PROCESSADO.equals(ajusteOpt.get().getStatus())) {
            throw new RegraNegocioException("Ajuste já está processado");
        }

        try {
            ajusteprodutos.removeProduto(codAjuste, codItem);
        } catch (Exception e) {
            // Correção: Logger
            logger.error("Erro ao remover produto do ajuste", e);
            throw new RegraNegocioException("Erro ao tentar remover produto do ajuste, chame o suporte");
        }

        return "Produto removido com sucesso";
    }
}