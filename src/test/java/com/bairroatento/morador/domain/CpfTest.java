package com.bairroatento.morador.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bairroatento.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

class CpfTest {

    @Test
    void cpfValidoComMascaraEHashDoSeed() {
        Cpf cpf = Cpf.de("969.430.400-81");
        assertThat(cpf.mascara()).isEqualTo("***.***.***-81");
        assertThat(cpf.hash()).isEqualTo("0721fa1db6683f1d514565851b213775ee8c5a9ebe3cfd74a9722432275e0a34");
        assertThat(Cpf.de("96943040081").hash()).isEqualTo(cpf.hash());
        assertThat(cpf.toString()).doesNotContain("969");
    }

    @Test
    void digitoVerificadorErradoEInvalido() {
        assertThatThrownBy(() -> Cpf.de("969.430.400-82")).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void sequenciaRepetidaEInvalida() {
        assertThatThrownBy(() -> Cpf.de("111.111.111-11")).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void formatoInvalido() {
        assertThatThrownBy(() -> Cpf.de("1234")).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> Cpf.de(null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> Cpf.de("abc.def.ghi-jk")).isInstanceOf(RegraNegocioException.class);
    }
}
