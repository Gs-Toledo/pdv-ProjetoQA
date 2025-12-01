package net.originmobi.pdv.service.notafiscal;

import net.originmobi.pdv.dto.ImpostoDTO;
import net.originmobi.pdv.exceptions.CalculoImpostoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.originmobi.pdv.model.NotaFiscalItemImposto;
import net.originmobi.pdv.model.TributacaoRegra;
import net.originmobi.pdv.repository.notafiscal.NotaFiscalItemImpostoRepository;

@Service
public class NotaFiscalItemImpostoService {

	@Autowired
	private NotaFiscalItemImpostoRepository impostos;

	public void cadastro(NotaFiscalItemImposto imposto) {
		impostos.save(imposto);
	}

    public NotaFiscalItemImposto merger(ImpostoDTO impostoDTO) {
        int orig = Character.getNumericValue(impostoDTO.getOrigin());
        int vlCstCofins = Integer.parseInt(impostoDTO.getCstCofins());
        int vlCstPis = Integer.parseInt(impostoDTO.getCstPis());

        NotaFiscalItemImposto imposto = new NotaFiscalItemImposto(
                orig,
                vlCstCofins,
                impostoDTO.getModBcIcms(),
                impostoDTO.getBcIcms(),
                impostoDTO.getAliqIcms(),
                impostoDTO.getVlIcms(),
                vlCstPis,
                impostoDTO.getBcPis(),
                impostoDTO.getPis(),
                impostoDTO.getVlPis(),
                impostoDTO.getBcCofins(),
                impostoDTO.getAliqCofins(),
                impostoDTO.getVlCofins(),
                impostoDTO.getCst(),
                impostoDTO.getCstIpi(),
                impostoDTO.getVbcIpi(),
                impostoDTO.getpIpi(),
                impostoDTO.getvIpi()
        );

        if (impostoDTO.getCodImposto() != null) {
            imposto.setCodigo(impostoDTO.getCodImposto());
        }

        try {
            impostos.save(imposto);
        } catch (Exception e) {
            throw new CalculoImpostoException("Erro ao lançar impostos na nota, chame o suporte");
        }

        return imposto;
    }

    public NotaFiscalItemImposto calcula(Long codimposto, Double vlTotal, TributacaoRegra regra, char origin, int modBcIcms) {

        ImpostoDTO dto = new ImpostoDTO();
        dto.setCodImposto(codimposto);
        dto.setOrigin(origin);
        dto.setModBcIcms(modBcIcms);

        int cst = Integer.parseInt(regra.getCst_csosn().getCst_csosn());
        dto.setCst(cst);
        dto.setCstCofins(regra.getCst_cofins().getCst());
        dto.setCstPis(regra.getCst_pis().getCst());

        dto.setBcIcms(vlTotal);
        dto.setAliqIcms(regra.getAliq_icms());
        dto.setVlIcms((vlTotal * dto.getAliqIcms()) / 100);

        dto.setBcPis(vlTotal);
        dto.setPis(regra.getPis());

        Double calcVlPis = (dto.getBcPis() * dto.getPis()) / 100;
        dto.setVlPis(calcVlPis);

        dto.setBcCofins(vlTotal);
        dto.setAliqCofins(regra.getCofins());

        Double calcVlCofins = (dto.getBcCofins() * dto.getAliqCofins()) / 100;
        dto.setVlCofins(calcVlCofins);

        dto.setCstIpi(Integer.parseInt(regra.getCst_ipi().getCst()));
        dto.setVbcIpi(vlTotal);
        dto.setpIpi(regra.getAliq_ipi());

        Double calcVIpi = (dto.getVbcIpi() * dto.getpIpi()) / 100;
        dto.setvIpi(calcVIpi);

        try {
            return merger(dto);
        } catch (Exception e) {
            throw new CalculoImpostoException("Erro ao calcular os impostos da nota", e);
        }
    }

}
