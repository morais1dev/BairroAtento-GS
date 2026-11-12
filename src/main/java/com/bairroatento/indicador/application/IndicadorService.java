package com.bairroatento.indicador.application;

import com.bairroatento.indicador.domain.IndicadorImpacto;
import com.bairroatento.indicador.infra.IndicadorConsultas;
import com.bairroatento.shared.domain.RegraNegocioException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;

@Service
public class IndicadorService {

    private final IndicadorConsultas consultas;

    public IndicadorService(IndicadorConsultas consultas) {
        this.consultas = consultas;
    }

    /**
     * @param periodo "AAAA-MM" (mês inteiro) ou "AAAA-MM-DD/AAAA-MM-DD" (intervalo inclusivo); opcional
     */
    public IndicadorImpacto impacto(String bairro, String periodo) {
        LocalDate inicio = null;
        LocalDate fim = null;
        if (periodo != null && !periodo.isBlank()) {
            try {
                String p = periodo.trim();
                if (p.contains("/")) {
                    String[] partes = p.split("/", -1);
                    if (partes.length != 2) throw new DateTimeParseException("periodo", p, 0);
                    inicio = LocalDate.parse(partes[0]);
                    fim = LocalDate.parse(partes[1]);
                } else {
                    YearMonth m = YearMonth.parse(p);
                    inicio = m.atDay(1);
                    fim = m.atEndOfMonth();
                }
            } catch (DateTimeParseException e) {
                throw new RegraNegocioException(
                        "periodo inválido; use AAAA-MM ou AAAA-MM-DD/AAAA-MM-DD");
            }
            if (fim.isBefore(inicio)) {
                throw new RegraNegocioException("periodo inválido: fim anterior ao início");
            }
        }
        return IndicadorImpacto.calcular(consultas.resumos(bairro, inicio, fim));
    }
}
