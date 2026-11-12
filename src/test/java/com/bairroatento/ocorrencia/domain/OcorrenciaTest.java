package com.bairroatento.ocorrencia.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bairroatento.shared.domain.ConflitoException;
import com.bairroatento.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

class OcorrenciaTest {

    private Ocorrencia nova() {
        return new Ocorrencia("buraco", "Buraco grande", "Rua A, 10", "Centro", null, null, 1L);
    }

    @Test
    void nasceAbertaSemConfirmacoes() {
        Ocorrencia o = nova();
        assertThat(o.getStatus()).isEqualTo("ABERTA");
        assertThat(o.getContadorConfirmacoes()).isZero();
    }

    @Test
    void camposObrigatorios() {
        assertThatThrownBy(() -> new Ocorrencia("buraco", "d", " ", "Centro", null, null, 1L))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new Ocorrencia("buraco", "d", "r", null, null, null, 1L))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new Ocorrencia("buraco", "d", "r", "b", 91.0, null, 1L))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void confirmacaoIncrementaEUnicaPorMorador() {
        Ocorrencia o = nova();
        o.confirmar(2L);
        o.confirmar(3L);
        assertThat(o.getContadorConfirmacoes()).isEqualTo(2);
        assertThatThrownBy(() -> o.confirmar(2L)).isInstanceOf(ConflitoException.class);
        assertThat(o.getContadorConfirmacoes()).isEqualTo(2);
    }
}
