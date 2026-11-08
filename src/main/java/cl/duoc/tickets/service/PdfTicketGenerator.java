package cl.duoc.tickets.service;

import cl.duoc.tickets.model.Ticket;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Genera el PDF personalizado de cada ticket. */
@Component
public class PdfTicketGenerator {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public byte[] generar(Ticket ticket) {
        try {
            return construir(ticket);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del ticket " + ticket.getCodigo(), e);
        }
    }

    private byte[] construir(Ticket ticket) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(doc, salida);
        doc.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12);

        Paragraph encabezado = new Paragraph(ticket.getEvento().getNombre(), titulo);
        encabezado.setAlignment(Element.ALIGN_CENTER);
        doc.add(encabezado);

        Paragraph codigo = new Paragraph("Ticket " + ticket.getCodigo(), subtitulo);
        codigo.setAlignment(Element.ALIGN_CENTER);
        codigo.setSpacingAfter(16);
        doc.add(codigo);

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(90);
        fila(tabla, "Evento", ticket.getEvento().getCodigo());
        fila(tabla, "Lugar", valor(ticket.getEvento().getLugar()));
        fila(tabla, "Fecha del evento", ticket.getEvento().getFecha() == null
                ? "-" : ticket.getEvento().getFecha().format(FORMATO));
        fila(tabla, "Usuario", ticket.getUsuario());
        fila(tabla, "Asiento", valor(ticket.getAsiento()));
        fila(tabla, "Precio", "$" + ticket.getPrecio().toPlainString());
        fila(tabla, "Estado", ticket.getEstado().name());
        doc.add(tabla);

        Paragraph pie = new Paragraph("Generado: " + LocalDateTime.now().format(FORMATO),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9));
        pie.setAlignment(Element.ALIGN_RIGHT);
        pie.setSpacingBefore(16);
        doc.add(pie);

        doc.close();
        return salida.toByteArray();
    }

    private void fila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        celdaEtiqueta.setPadding(6);
        tabla.addCell(celdaEtiqueta);
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor,
                FontFactory.getFont(FontFactory.HELVETICA, 11)));
        celdaValor.setPadding(6);
        tabla.addCell(celdaValor);
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }
}
