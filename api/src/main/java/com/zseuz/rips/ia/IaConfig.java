package com.zseuz.rips.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

/**
 * Cliente de la API de Claude. Solo se crea si hay clave; sin ella la aplicación arranca igual y
 * la explicación responde 503 diciendo qué falta.
 */
@Configuration
class IaConfig {

    /** Reutilizable (mantiene sus conexiones); Spring lo cierra al apagar la aplicación. */
    @Bean
    @ConditionalOnExpression("!'${rips.ia.api-key:}'.isBlank()")
    AnthropicClient anthropicClient(@Value("${rips.ia.api-key}") String clave) {
        return AnthropicOkHttpClient.builder().apiKey(clave).build();
    }
}
