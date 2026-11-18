package com.rapipedido;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Evidencia de la táctica de bajo acoplamiento: el build falla si un módulo usa clases internas de
 * otro o si aparece una dependencia circular. Además genera diagramas C4 de componentes en
 * {@code target/spring-modulith-docs}.
 */
class ModularidadTest {

    private final ApplicationModules modulos = ApplicationModules.of(RapiPedidoApplication.class);

    @Test
    void losModulosRespetanSusLimites() {
        modulos.verify();
    }

    @Test
    void generaDocumentacionDeModulos() {
        new Documenter(modulos)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
