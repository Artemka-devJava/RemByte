package com.rembyte.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rembyte.model.CompanySettings;
import com.rembyte.model.Order;
import com.rembyte.model.OrderAttachment;
import com.rembyte.repository.OrderAttachmentRepository;
import com.rembyte.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Генерация «Акта приёма оборудования в ремонт» (PDF) по заказу.
 * <p>
 * Шапка (логотип + реквизиты компании) берётся из тех же настроек, что и
 * чек/квитанция ({@link CompanySettingsService}). Тело минимальное: что приняли,
 * от кого, примерная стоимость, срок — по договорённости. Готовый PDF
 * складывается в заказ как вложение {@link OrderAttachment.AttachmentType#ACT}
 * (одна актуальная копия на заказ).
 */
@Service
public class AcceptanceActService {

    private static final Logger log = LoggerFactory.getLogger(AcceptanceActService.class);

    /** Фиксированное имя файла акта в заказе — новая генерация заменяет старую копию. */
    public static final String ACT_STORED_NAME = "acceptance-act.pdf";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final OrderRepository orderRepository;
    private final OrderAttachmentRepository attachmentRepository;
    private final CompanySettingsService companySettings;

    private volatile BaseFont baseFont;
    private volatile byte[] logoBytes;
    private volatile boolean logoLoaded;

    public AcceptanceActService(OrderRepository orderRepository,
                                OrderAttachmentRepository attachmentRepository,
                                CompanySettingsService companySettings) {
        this.orderRepository = orderRepository;
        this.attachmentRepository = attachmentRepository;
        this.companySettings = companySettings;
    }

    /**
     * Отрендерить акт по заказу и сохранить его в заказ (заменив предыдущую копию).
     * @return байты PDF
     */
    @Transactional
    public byte[] renderAndStore(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Заказ не найден: " + orderId));

        byte[] pdf = render(order);

        OrderAttachment att = attachmentRepository
                .findByOrderIdAndStoredName(orderId, ACT_STORED_NAME)
                .orElseGet(OrderAttachment::new);
        att.setOrder(order);
        att.setStoredName(ACT_STORED_NAME);
        att.setOriginalName("Акт приёмки " + safe(order.getOrderNumber()) + ".pdf");
        att.setContentType("application/pdf");
        att.setAttachmentType(OrderAttachment.AttachmentType.ACT);
        att.setContent(pdf);
        attachmentRepository.save(att);

        return pdf;
    }

    /** Чистый рендер PDF без сохранения — вынесен для тестов. */
    public byte[] render(Order order) {
        CompanySettings company = companySettings.current();

        Document doc = new Document(PageSize.A4, 56, 56, 48, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf = font();
            Font h1 = new Font(bf, 15, Font.BOLD);
            Font body = new Font(bf, 11, Font.NORMAL);
            Font foot = new Font(bf, 8, Font.NORMAL, Color.GRAY);

            doc.add(buildHeader(company, bf));

            Paragraph title = new Paragraph("АКТ ПРИЁМА ОБОРУДОВАНИЯ В РЕМОНТ", h1);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(16f);
            title.setSpacingAfter(6f);
            doc.add(title);

            String number = safe(order.getOrderNumber());
            String date = order.getCreatedAt() != null ? order.getCreatedAt().format(DATE) : "";
            Paragraph sub = new Paragraph("№ " + number + "   от " + date, body);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(16f);
            doc.add(sub);

            doc.add(field("Принято от:", clientLine(order)));
            doc.add(field("Принято в ремонт:", blankIfEmpty(order.getDeviceDescription())));
            doc.add(field("Примерная стоимость ремонта:", price(order)));
            doc.add(field("Срок ремонта:", "по договорённости"));

            Paragraph signs = new Paragraph();
            signs.setSpacingBefore(40f);
            signs.add(new Chunk("Сдал (клиент):  ______________________  /  ______________________\n\n", body));
            signs.add(new Chunk("Принял:            ______________________  /  ______________________", body));
            doc.add(signs);

            Paragraph gen = new Paragraph(
                    "Акт сформирован " + LocalDateTime.now().format(DATE_TIME)
                            + ". Оборудование выдаётся при предъявлении этого акта.", foot);
            gen.setSpacingBefore(28f);
            doc.add(gen);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сформировать PDF акта: " + e.getMessage(), e);
        }
    }

    /** Шапка: логотип слева + название/подзаголовок/адрес/телефон/email. */
    private PdfPTable buildHeader(CompanySettings c, BaseFont bf) throws Exception {
        Font nameFont = new Font(bf, 13, Font.BOLD);
        Font small = new Font(bf, 9, Font.NORMAL, new Color(90, 90, 90));

        boolean hasLogo = logo() != null;
        PdfPTable t = new PdfPTable(hasLogo ? new float[]{1f, 4f} : new float[]{1f});
        t.setWidthPercentage(100);
        t.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
        t.getDefaultCell().setPaddingBottom(2f);

        if (hasLogo) {
            Image img = Image.getInstance(logo());
            img.scaleToFit(96f, 56f);
            PdfPCell logoCell = new PdfPCell(img, false);
            logoCell.setBorder(PdfPCell.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_TOP);
            logoCell.setPaddingRight(10f);
            t.addCell(logoCell);
        }

        Paragraph info = new Paragraph();
        info.setLeading(13f);
        info.add(new Chunk(orDefault(c.getName(), "FixByte"), nameFont));
        info.add(Chunk.NEWLINE);
        String subtitle = orDefault(c.getSubtitle(), "Сервисный центр · ремонт техники");
        info.add(new Chunk(subtitle, small));
        for (String line : new String[]{c.getAddress(), contact(c)}) {
            if (line != null && !line.isBlank()) {
                info.add(Chunk.NEWLINE);
                info.add(new Chunk(line, small));
            }
        }
        PdfPCell infoCell = new PdfPCell(info);
        infoCell.setBorder(PdfPCell.NO_BORDER);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(infoCell);

        return t;
    }

    private static String contact(CompanySettings c) {
        String phone = c.getPhone() == null ? "" : c.getPhone().trim();
        String email = c.getEmail() == null ? "" : c.getEmail().trim();
        if (phone.isEmpty() && email.isEmpty()) return null;
        if (phone.isEmpty()) return email;
        if (email.isEmpty()) return "тел. " + phone;
        return "тел. " + phone + "   " + email;
    }

    private Paragraph field(String labelText, String valueText) {
        BaseFont bf = font();
        Paragraph p = new Paragraph();
        p.setSpacingAfter(10f);
        p.add(new Chunk(labelText + " ", new Font(bf, 11, Font.BOLD)));
        p.add(new Phrase(valueText, new Font(bf, 11, Font.NORMAL)));
        return p;
    }

    private static String clientLine(Order order) {
        if (order.getClient() == null) return "—";
        String name = safe(order.getClient().getName());
        String phone = safe(order.getClient().getPhone());
        return phone.isEmpty() ? name : name + ", тел. " + phone;
    }

    private static String price(Order order) {
        double total = order.getTotalPrice() == null ? 0.0 : order.getTotalPrice();
        if (total <= 0) return "уточняется";
        return trimNumber(total) + " руб.";
    }

    private static String trimNumber(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String blankIfEmpty(String s) {
        return (s == null || s.isBlank()) ? "—" : s.trim();
    }

    private static String orDefault(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private byte[] logo() {
        if (!logoLoaded) {
            synchronized (this) {
                if (!logoLoaded) {
                    try (InputStream in = new ClassPathResource("static/images/logo.png").getInputStream()) {
                        logoBytes = in.readAllBytes();
                    } catch (Exception e) {
                        log.warn("Логотип для акта не загружен (static/images/logo.png): {}", e.getMessage());
                        logoBytes = null;
                    }
                    logoLoaded = true;
                }
            }
        }
        return logoBytes;
    }

    private BaseFont font() {
        BaseFont f = baseFont;
        if (f == null) {
            synchronized (this) {
                if (baseFont == null) {
                    try (InputStream in = new ClassPathResource("fonts/DroidSans.ttf").getInputStream()) {
                        byte[] bytes = in.readAllBytes();
                        baseFont = BaseFont.createFont("DroidSans.ttf", BaseFont.IDENTITY_H,
                                BaseFont.EMBEDDED, true, bytes, null);
                    } catch (Exception e) {
                        throw new IllegalStateException("Не удалось загрузить шрифт акта (fonts/DroidSans.ttf)", e);
                    }
                }
                f = baseFont;
            }
        }
        return f;
    }
}
