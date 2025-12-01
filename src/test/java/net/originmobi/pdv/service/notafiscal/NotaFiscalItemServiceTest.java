package net.originmobi.pdv.service.notafiscal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.notafiscal.NotaFiscalTipo;
import net.originmobi.pdv.enumerado.produto.ProdutoSubstTributaria;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.model.Tributacao;
import net.originmobi.pdv.model.NotaFiscal;
import net.originmobi.pdv.model.TributacaoRegra;
import net.originmobi.pdv.model.Endereco;
import net.originmobi.pdv.model.Estado;
import net.originmobi.pdv.model.Cidade;
import net.originmobi.pdv.model.Pessoa; 
import net.originmobi.pdv.model.CstCsosn;
import net.originmobi.pdv.model.CFOP;
import net.originmobi.pdv.model.ModBcIcms;
import net.originmobi.pdv.model.NotaFiscalItem;
import net.originmobi.pdv.model.NotaFiscalItemImposto;
import net.originmobi.pdv.model.NotaFiscalTotais;
import net.originmobi.pdv.service.ProdutoService;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalItemRepository;

@ExtendWith(MockitoExtension.class)
class NotaFiscalItemServiceTest {

    @InjectMocks
    private NotaFiscalItemService service;

    @Mock
    private ProdutoService produtos;

    @Mock
    private NotaFiscalService notas;

    @Mock
    private NotaFiscalItemRepository itemServer;

    @Mock
    private NotaFiscalItemImpostoService impostos;

    @Mock
    private NotaFiscalTotaisServer totais;
    
    //testa se falha se o produto nao for encontrado
    @Test
    void TC_01_insere_deveFalharSeProdutoNaoForEncontrado() {
        Long COD_PROD_INEXISTENTE = 99L;

        when(produtos.buscaProduto(COD_PROD_INEXISTENTE)).thenReturn(Optional.empty());

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.insere(COD_PROD_INEXISTENTE, 1L, 1, NotaFiscalTipo.SAIDA);
        });

        assertTrue(excecao.getMessage().contains("Nenhum produto encontrado, favor verifique"));
    }
    
    //testa se falha se o produto nao tiver tributacao
    @Test
    void TC_02_insere_deveFalharSeProdutoNaoTiverTributacao() {
        Long COD_PROD = 1L;
        Produto produtoSemTributacao = new Produto();
        
        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produtoSemTributacao));
        when(notas.busca(1L)).thenReturn(Optional.of(new NotaFiscal()));

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });

        assertTrue(excecao.getMessage().contains("Produto sem tributação, favor verifique"));
    }
    
    //testa falha se o produto nao tiver ncm
    @Test
    void TC_03_insere_deveFalharSeProdutoNaoTiverNCM() {
        Long COD_PROD = 1L;
        Produto produtoSemNCM = new Produto();
        produtoSemNCM.setTributacao(new Tributacao());
        produtoSemNCM.setNcm("");

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produtoSemNCM));
        when(notas.busca(1L)).thenReturn(Optional.of(new NotaFiscal()));

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });

        assertTrue(excecao.getMessage().contains("Produto sem código NCM, favor verifique"));
    }

    //testa falha se o produto nao tiver unidade
    @Test
    void TC_04_insere_deveFalharSeProdutoNaoTiverUnidade() {
        Long COD_PROD = 1L;
        Produto produtoSemUnidade = new Produto();
        produtoSemUnidade.setTributacao(new Tributacao());
        produtoSemUnidade.setNcm("0101.01.01");
        
  
        produtoSemUnidade.setSubtributaria(ProdutoSubstTributaria.NAO); 
        
        produtoSemUnidade.setUnidade(""); 

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produtoSemUnidade));
        when(notas.busca(1L)).thenReturn(Optional.of(new NotaFiscal()));

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });

        assertTrue(excecao.getMessage().contains("Produto sem unidade, favor verifique"));
    }

    @Test
    void TC_05_insere_deveFalharSeProdutoComSTSemCEST() {
        Long COD_PROD = 1L;
        Produto produtoSemCEST = new Produto();
        produtoSemCEST.setTributacao(new Tributacao());
        produtoSemCEST.setNcm("0101.01.01");
        produtoSemCEST.setUnidade("UN");
        
        produtoSemCEST.setSubtributaria(ProdutoSubstTributaria.SIM); 
        produtoSemCEST.setCest(""); 

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produtoSemCEST));
        when(notas.busca(1L)).thenReturn(Optional.of(new NotaFiscal()));

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });

        assertTrue(excecao.getMessage().contains("Produto de substituição tributária sem código CEST, favor verifique"));
    }

    @Test
    void TC_06_insere_deveFalharSeProdutoSemRegraDeSaida() {
        Long COD_PROD = 1L;
        
        Produto produtoSemRegraSaida = new Produto();
        produtoSemRegraSaida.setNcm("0101.01.01");
        produtoSemRegraSaida.setUnidade("UN");
        produtoSemRegraSaida.setSubtributaria(ProdutoSubstTributaria.NAO);

        Tributacao tributacaoApenasEntrada = new Tributacao();
        TributacaoRegra regraEntrada = new TributacaoRegra();
        regraEntrada.setTipo(EntradaSaida.ENTRADA); // Apenas entrada
        
        tributacaoApenasEntrada.setRegra(Collections.singletonList(regraEntrada));
        produtoSemRegraSaida.setTributacao(tributacaoApenasEntrada);

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produtoSemRegraSaida));
        when(notas.busca(1L)).thenReturn(Optional.of(new NotaFiscal()));

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });

        assertTrue(excecao.getMessage().contains("Tributação sem regra de saída, verifique"));
    }

    @Test
    void TC_07_insere_deveFalharSeProdutoSemRegraDeEntrada() {
        Long COD_PROD = 1L;
        
        Produto produtoSemRegraEntrada = new Produto();
        produtoSemRegraEntrada.setNcm("0101.01.01");
        produtoSemRegraEntrada.setUnidade("UN");
        produtoSemRegraEntrada.setSubtributaria(ProdutoSubstTributaria.NAO);

        Tributacao tributacaoApenasSaida = new Tributacao();
        TributacaoRegra regraSaida = new TributacaoRegra();
        regraSaida.setTipo(EntradaSaida.SAIDA); // Apenas regras de SAIDA
        
        tributacaoApenasSaida.setRegra(Collections.singletonList(regraSaida));
        produtoSemRegraEntrada.setTributacao(tributacaoApenasSaida);

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produtoSemRegraEntrada));
        when(notas.busca(1L)).thenReturn(Optional.of(new NotaFiscal()));

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.ENTRADA); 
        });

        assertTrue(excecao.getMessage().contains("Tributação sem regra de entrada, verifique"));
    }

    @Test
    void TC_08_insere_deveExecutarComSucessoNotaSaidaERegraEncontrada() {
        Long COD_PROD = 1L;
        String UF_DESTINATARIO = "SP";

        Estado estado = new Estado();
        estado.setSigla(UF_DESTINATARIO);

        Cidade cidade = new Cidade();
        cidade.setEstado(estado);

        Endereco endereco = new Endereco();
        endereco.setCidade(cidade);

        Pessoa destinatario = new Pessoa();
        destinatario.setEndereco(endereco);

        NotaFiscal notaFiscal = new NotaFiscal();
        notaFiscal.setDestinatario(destinatario);
        notaFiscal.setTipo(NotaFiscalTipo.SAIDA);
        notaFiscal.setItens(Collections.emptyList());
        notaFiscal.setTotais(new NotaFiscalTotais());
        notaFiscal.setCodigo(1L);

        CstCsosn cst = new CstCsosn();
        cst.setCst_csosn("0");

        CFOP cfop = new CFOP();
        cfop.setCfop("5102");

        TributacaoRegra regraCorreta = new TributacaoRegra();
        regraCorreta.setUf(estado);
        regraCorreta.setTipo(EntradaSaida.SAIDA);
        regraCorreta.setCst_csosn(cst);
        regraCorreta.setCfop(cfop);

        Tributacao tributacao = new Tributacao();
        tributacao.setRegra(Collections.singletonList(regraCorreta));

        ModBcIcms modBcIcms = new ModBcIcms();
        modBcIcms.setTipo(1);

        Produto produtoValido = new Produto();
        produtoValido.setNcm("0101.01.01");
        produtoValido.setUnidade("UN");
        produtoValido.setSubtributaria(ProdutoSubstTributaria.NAO);
        produtoValido.setTributacao(tributacao);
        produtoValido.setValor_venda(10.0);
        produtoValido.setModBcIcms(modBcIcms);

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produtoValido));
        when(notas.busca(1L)).thenReturn(Optional.of(notaFiscal));
        when(impostos.calcula(any(), anyDouble(), any(), anyChar(), anyInt()))
                .thenReturn(new NotaFiscalItemImposto());
        when(itemServer.save(any(NotaFiscalItem.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });
    }



    @Test
    void TC_09_insere_deveAtualizarItemQuandoProdutoJaExistirNaNota() {
        Long COD_PROD = 1L;
        String UF_DESTINATARIO = "SP";

        Estado estado = new Estado();
        estado.setSigla(UF_DESTINATARIO);

        Cidade cidade = new Cidade();
        cidade.setEstado(estado);

        Endereco endereco = new Endereco();
        endereco.setCidade(cidade);

        Pessoa destinatario = new Pessoa();
        destinatario.setEndereco(endereco);

        NotaFiscalItemImposto impostoExistente = new NotaFiscalItemImposto();
        impostoExistente.setCodigo(10L);

        NotaFiscalItem itemExistente = new NotaFiscalItem();
        itemExistente.setCodigo(20L);
        itemExistente.setCodProd(COD_PROD);
        itemExistente.setQtd(2);
        itemExistente.setImpostos(impostoExistente);

        NotaFiscal notaFiscal = new NotaFiscal();
        notaFiscal.setDestinatario(destinatario);
        notaFiscal.setTipo(NotaFiscalTipo.SAIDA);
        notaFiscal.setItens(Collections.singletonList(itemExistente));
        notaFiscal.setTotais(new NotaFiscalTotais());
        notaFiscal.setCodigo(1L);

        CstCsosn cst = new CstCsosn();
        cst.setCst_csosn("0");

        CFOP cfop = new CFOP();
        cfop.setCfop("5102");

        TributacaoRegra regra = new TributacaoRegra();
        regra.setUf(estado);
        regra.setTipo(EntradaSaida.SAIDA);
        regra.setCst_csosn(cst);
        regra.setCfop(cfop);

        Tributacao tributacao = new Tributacao();
        tributacao.setRegra(Collections.singletonList(regra));

        ModBcIcms modBcIcms = new ModBcIcms();
        modBcIcms.setTipo(1);

        Produto produto = new Produto();
        produto.setNcm("0101.01.01");
        produto.setUnidade("UN");
        produto.setSubtributaria(ProdutoSubstTributaria.NAO);
        produto.setTributacao(tributacao);
        produto.setValor_venda(10.0);
        produto.setModBcIcms(modBcIcms);

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produto));
        when(notas.busca(1L)).thenReturn(Optional.of(notaFiscal));
        when(impostos.calcula(any(), anyDouble(), any(), anyChar(), anyInt()))
                .thenReturn(new NotaFiscalItemImposto());
        when(itemServer.save(any(NotaFiscalItem.class))).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });
    };

    @Test
    void TC_10_insere_deveFalharQuandoNaoExistirRegraParaUF() {
        Long COD_PROD = 1L;

        Estado estadoRegra = new Estado();
        estadoRegra.setSigla("RJ");

        Estado estadoDestinatario = new Estado();
        estadoDestinatario.setSigla("SP");

        Cidade cidade = new Cidade();
        cidade.setEstado(estadoDestinatario);

        Endereco endereco = new Endereco();
        endereco.setCidade(cidade);

        Pessoa destinatario = new Pessoa();
        destinatario.setEndereco(endereco);

        NotaFiscal notaFiscal = new NotaFiscal();
        notaFiscal.setDestinatario(destinatario);
        notaFiscal.setTipo(NotaFiscalTipo.SAIDA);
        notaFiscal.setItens(Collections.emptyList());
        notaFiscal.setTotais(new NotaFiscalTotais());

        CstCsosn cst = new CstCsosn();
        cst.setCst_csosn("0");

        CFOP cfop = new CFOP();
        cfop.setCfop("5102");

        TributacaoRegra regraIncompativel = new TributacaoRegra();
        regraIncompativel.setUf(estadoRegra);
        regraIncompativel.setTipo(EntradaSaida.SAIDA);
        regraIncompativel.setCst_csosn(cst);
        regraIncompativel.setCfop(cfop);

        Tributacao tributacao = new Tributacao();
        tributacao.setRegra(Collections.singletonList(regraIncompativel));

        ModBcIcms modBcIcms = new ModBcIcms();
        modBcIcms.setTipo(1);

        Produto produto = new Produto();
        produto.setNcm("0101.01.01");
        produto.setUnidade("UN");
        produto.setSubtributaria(ProdutoSubstTributaria.NAO);
        produto.setTributacao(tributacao);
        produto.setValor_venda(10.0);
        produto.setModBcIcms(modBcIcms);

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produto));
        when(notas.busca(1L)).thenReturn(Optional.of(notaFiscal));

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });

        assertTrue(excecao.getMessage().contains("Nenhuma regra de tributação cadastrada para a UF do destinatário"));
    }

    @Test
    void TC_11_insere_deveFalharQuandoErroAoSalvarItem() {
        Long COD_PROD = 1L;
        String UF_DESTINATARIO = "SP";

        Estado estado = new Estado();
        estado.setSigla(UF_DESTINATARIO);

        Cidade cidade = new Cidade();
        cidade.setEstado(estado);

        Endereco endereco = new Endereco();
        endereco.setCidade(cidade);

        Pessoa destinatario = new Pessoa();
        destinatario.setEndereco(endereco);

        NotaFiscal notaFiscal = new NotaFiscal();
        notaFiscal.setDestinatario(destinatario);
        notaFiscal.setTipo(NotaFiscalTipo.SAIDA);
        notaFiscal.setItens(Collections.emptyList());
        notaFiscal.setTotais(new NotaFiscalTotais());
        notaFiscal.setCodigo(1L);

        CstCsosn cst = new CstCsosn();
        cst.setCst_csosn("0");

        CFOP cfop = new CFOP();
        cfop.setCfop("5102");

        TributacaoRegra regra = new TributacaoRegra();
        regra.setUf(estado);
        regra.setTipo(EntradaSaida.SAIDA);
        regra.setCst_csosn(cst);
        regra.setCfop(cfop);

        Tributacao tributacao = new Tributacao();
        tributacao.setRegra(Collections.singletonList(regra));

        ModBcIcms modBcIcms = new ModBcIcms();
        modBcIcms.setTipo(1);

        Produto produto = new Produto();
        produto.setNcm("0101.01.01");
        produto.setUnidade("UN");
        produto.setSubtributaria(ProdutoSubstTributaria.NAO);
        produto.setTributacao(tributacao);
        produto.setValor_venda(10.0);
        produto.setModBcIcms(modBcIcms);

        when(produtos.buscaProduto(COD_PROD)).thenReturn(Optional.of(produto));
        when(notas.busca(1L)).thenReturn(Optional.of(notaFiscal));
        when(impostos.calcula(any(), anyDouble(), any(), anyChar(), anyInt()))
                .thenReturn(new NotaFiscalItemImposto());
        when(itemServer.save(any(NotaFiscalItem.class))).thenThrow(new RuntimeException("Erro banco"));

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.insere(COD_PROD, 1L, 1, NotaFiscalTipo.SAIDA);
        });

        assertTrue(excecao.getMessage().contains("Erro ao salvar item na nota, chame o suporte"));
    }

    @Test
    void TC_12_remove_deveRemoverItemEAtualizarTotais() {
        Long COD_ITEM = 10L;
        Long COD_NOTA = 1L;

        NotaFiscal notaFiscal = new NotaFiscal();
        notaFiscal.setTotais(new NotaFiscalTotais());

        when(notas.busca(COD_NOTA)).thenReturn(Optional.of(notaFiscal));

        assertDoesNotThrow(() -> {
            service.remove(COD_ITEM, COD_NOTA);
        });
    }

    @Test
    void TC_13_remove_deveFalharQuandoErroAoRemoverItem() {
        Long COD_ITEM = 10L;
        Long COD_NOTA = 1L;

        doThrow(new RuntimeException("Erro banco")).when(itemServer).deleteById(COD_ITEM);

        RuntimeException excecao = assertThrows(RuntimeException.class, () -> {
            service.remove(COD_ITEM, COD_NOTA);
        });

        assertTrue(excecao.getMessage().contains("Erro ao tentar remover o item da nota, chame o suporte"));
    }
    
    @Test
    void TC_14_buscaItensNota_deveRetornarListaDoRepositorio() {
        Long COD_NOTA = 1L;

        List<Object> lista = Collections.singletonList(new Object());

        when(itemServer.findByNotaFiscalCodigoEquals(COD_NOTA)).thenReturn(lista);

        List<Object> resultado = service.buscaItensNota(COD_NOTA);

        assertTrue(resultado.size() == 1);
    }



}