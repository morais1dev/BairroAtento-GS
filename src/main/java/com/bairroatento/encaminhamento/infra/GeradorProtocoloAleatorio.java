package com.bairroatento.encaminhamento.infra;

import com.bairroatento.encaminhamento.domain.GeradorProtocolo;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Protocolo no formato BA-AAAA-OOOOOO-DD-XXXXXXXX (ano, ocorrência, destinatário, sufixo aleatório). */
@Component
public class GeradorProtocoloAleatorio implements GeradorProtocolo {

    @Override
    public String gerar(Long ocorrenciaId, Long destinatarioId) {
        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return String.format("BA-%d-%06d-%02d-%s", Year.now().getValue(), ocorrenciaId, destinatarioId, sufixo);
    }
}
