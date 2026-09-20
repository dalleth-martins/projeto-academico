package com.tcc.accountservice.metrics;

import com.tcc.accountservice.enums.AccountOperationStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Métricas de negócio do account-service.
 * Ficam disponíveis no Prometheus como:
 *  - account_pix_results_total{status}
 *  - account_pix_processing_seconds_*{resultado}  (resultado = sucesso | erro)
 */
@Component
@RequiredArgsConstructor
public class AccountMetrics {

    private final MeterRegistry meterRegistry;

    /** Resultado final de uma transação Pix (CONCLUIDO, FALHOU_SALDO_INSUFICIENTE, ...). */
    public void registrarResultado(AccountOperationStatus status) {
        Counter.builder("account.pix.results")
                .description("Resultados finais das transações Pix processadas pelo account-service")
                .tag("status", status.name())
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample iniciarProcessamento() {
        return Timer.start(meterRegistry);
    }

    /**
     * "sucesso": a mensagem foi processada e o resultado publicado.
     * "erro": houve exceção; a mensagem será reentregue (retry) ou irá para a DLQ.
     */
    public void finalizarProcessamento(Timer.Sample amostra, String resultado) {
        amostra.stop(
                Timer.builder("account.pix.processing")
                        .description("Tempo de processamento de uma mensagem de transação Pix")
                        .tag("resultado", resultado)
                        .publishPercentileHistogram()
                        .register(meterRegistry)
        );
    }
}
