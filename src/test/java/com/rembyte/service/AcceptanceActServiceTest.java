package com.rembyte.service;

import com.rembyte.model.Client;
import com.rembyte.model.CompanySettings;
import com.rembyte.model.Order;
import com.rembyte.model.OrderAttachment;
import com.rembyte.repository.OrderAttachmentRepository;
import com.rembyte.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptanceActServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderAttachmentRepository attachmentRepository;
    @Mock CompanySettingsService companySettings;

    AcceptanceActService service;

    @BeforeEach
    void setUp() {
        service = new AcceptanceActService(orderRepository, attachmentRepository, companySettings);
        CompanySettings row = new CompanySettings();
        row.setName("Ромашка-Сервис");
        row.setSubtitle("Ремонт ноутбуков и ПК");
        row.setAddress("г. Тестоград, ул. Паяльная, 1");
        row.setPhone("+7 900 000-00-00");
        row.setEmail("hello@example.com");
        lenient().when(companySettings.current()).thenReturn(row);
    }

    private Order order() {
        Client c = new Client("Иван Петров", "+79990001122");
        Order o = new Order("ФБ-12345", c);
        o.setDeviceDescription("Ноутбук Lenovo IdeaPad, не включается");
        o.setTotalPrice(3500.0);
        return o;
    }

    @Test
    void render_producesANonTrivialPdf() {
        byte[] pdf = service.render(order());

        assertThat(pdf).isNotEmpty();
        assertThat(pdf.length).isGreaterThan(800);
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).startsWith("%PDF-");
    }

    @Test
    void render_worksWhenPriceIsZeroAndDeviceMissing() {
        Order o = new Order("ФБ-1", new Client("Клиент", "+70000000000"));
        o.setTotalPrice(0.0);

        byte[] pdf = service.render(o);

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).startsWith("%PDF-");
    }

    @Test
    void renderAndStore_upsertsSingleActAttachmentOnTheOrder() {
        Order o = order();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(o));
        when(attachmentRepository.findByOrderIdAndStoredName(5L, AcceptanceActService.ACT_STORED_NAME))
                .thenReturn(Optional.empty());
        when(attachmentRepository.save(any(OrderAttachment.class))).thenAnswer(i -> i.getArgument(0));

        byte[] pdf = service.renderAndStore(5L);

        ArgumentCaptor<OrderAttachment> captor = ArgumentCaptor.forClass(OrderAttachment.class);
        verify(attachmentRepository).save(captor.capture());
        OrderAttachment saved = captor.getValue();
        assertThat(saved.getStoredName()).isEqualTo(AcceptanceActService.ACT_STORED_NAME);
        assertThat(saved.getContentType()).isEqualTo("application/pdf");
        assertThat(saved.getAttachmentType()).isEqualTo(OrderAttachment.AttachmentType.ACT);
        assertThat(saved.getContent()).isEqualTo(pdf);
        assertThat(saved.getOrder()).isSameAs(o);
    }

    @Test
    void renderAndStore_replacesExistingActRatherThanAddingSecond() {
        Order o = order();
        OrderAttachment existing = new OrderAttachment();
        existing.setId(99L);
        existing.setStoredName(AcceptanceActService.ACT_STORED_NAME);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(o));
        when(attachmentRepository.findByOrderIdAndStoredName(5L, AcceptanceActService.ACT_STORED_NAME))
                .thenReturn(Optional.of(existing));
        when(attachmentRepository.save(any(OrderAttachment.class))).thenAnswer(i -> i.getArgument(0));

        service.renderAndStore(5L);

        ArgumentCaptor<OrderAttachment> captor = ArgumentCaptor.forClass(OrderAttachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(99L); // та же строка, не новая
    }

    @Test
    void renderAndStore_missingOrderThrows() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.renderAndStore(404L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
