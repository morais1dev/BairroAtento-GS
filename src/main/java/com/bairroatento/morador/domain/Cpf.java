package com.bairroatento.morador.domain;

import com.bairroatento.shared.domain.RegraNegocioException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Value object de CPF: valida formato e dígitos verificadores; nunca expõe o número puro. */
public final class Cpf {

    private final String digitos;

    private Cpf(String digitos) {
        this.digitos = digitos;
    }

    public static Cpf de(String texto) {
        if (texto == null || !texto.matches("\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) {
            throw new RegraNegocioException("CPF com formato inválido");
        }
        String d = texto.replaceAll("\\D", "");
        if (d.chars().distinct().count() == 1
                || digito(d, 9) != d.charAt(9) - '0'
                || digito(d, 10) != d.charAt(10) - '0') {
            throw new RegraNegocioException("CPF inválido");
        }
        return new Cpf(d);
    }

    private static int digito(String d, int n) {
        int soma = 0;
        for (int i = 0; i < n; i++) {
            soma += (d.charAt(i) - '0') * (n + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** SHA-256 hexadecimal dos 11 dígitos. */
    public String hash() {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(digitos.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Máscara ***.***.***-XX com apenas os 2 últimos dígitos. */
    public String mascara() {
        return "***.***.***-" + digitos.substring(9);
    }

    @Override
    public String toString() {
        return mascara();
    }
}
