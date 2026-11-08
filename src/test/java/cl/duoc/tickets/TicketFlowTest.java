package cl.duoc.tickets;

import cl.duoc.tickets.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el flujo completo con H2 y S3 simulado:
 * crear evento, generar ticket (EFS + S3), modificar, estadisticas y eliminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketFlowTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private S3StorageService s3;

    @Test
    void flujoCompletoDeUnTicket() throws Exception {
        mvc.perform(post("/api/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"evento123","nombre":"Concierto Duoc","lugar":"Santiago",
                                 "fecha":"2026-12-15T20:00:00","capacidad":100,"precio":25000}
                                """))
                .andExpect(status().isCreated());

        String respuesta = mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventoCodigo":"evento123","usuario":"usuarioX","asiento":"A1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RESERVADO"))
                .andExpect(jsonPath("$.subidoS3").value(true))
                .andReturn().getResponse().getContentAsString();

        String codigo = respuesta.replaceAll(".*\"codigo\":\"(ticket\\d+)\".*", "$1");
        String id = respuesta.replaceAll(".*\"id\":(\\d+).*", "$1");
        String key = "evento123/usuarioX/" + codigo + ".pdf";

        // El PDF quedo en EFS y se subio a S3 con la estructura evento/usuario/ticket
        String rutaEfs = respuesta.replaceAll(".*\"rutaEfs\":\"([^\"]+)\".*", "$1");
        assertThat(Files.exists(Path.of(rutaEfs))).isTrue();
        verify(s3).subir(eq(key), any());

        mvc.perform(put("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"asiento":"B7","estado":"PAGADO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asiento").value("B7"))
                .andExpect(jsonPath("$.estado").value("PAGADO"));
        verify(s3, times(2)).subir(eq(key), any());

        mvc.perform(get("/api/tickets").param("usuario", "usuarioX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/estadisticas/eventos/evento123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventas").value(1))
                .andExpect(jsonPath("$.reservas").value(0));

        mvc.perform(delete("/api/tickets/" + id))
                .andExpect(status().isOk());
        verify(s3).eliminar(key);
        assertThat(Files.exists(Path.of(rutaEfs))).isFalse();
    }
}
