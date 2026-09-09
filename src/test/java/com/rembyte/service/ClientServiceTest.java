package com.rembyte.service;

import com.rembyte.model.Client;
import com.rembyte.model.ClientDevice;
import com.rembyte.model.Order;
import com.rembyte.repository.ClientDeviceRepository;
import com.rembyte.repository.ClientNoteRepository;
import com.rembyte.repository.ClientPhotoRepository;
import com.rembyte.repository.ClientRepository;
import com.rembyte.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;
    @Mock OrderRepository orderRepository;
    @Mock ClientNoteRepository noteRepository;
    @Mock ClientDeviceRepository deviceRepository;
    @Mock ClientPhotoRepository photoRepository;

    ClientService service;

    @BeforeEach
    void setUp() {
        service = new ClientService(clientRepository, orderRepository, noteRepository,
                deviceRepository, photoRepository);
    }

    // ── normalizePhone ──────────────────────────────────────

    @Test
    void normalizePhone_russianVariantsCollapseToPlus7() {
        assertThat(ClientService.normalizePhone("8 (999) 123-45-67")).isEqualTo("+79991234567");
        assertThat(ClientService.normalizePhone("+7 999 123 45 67")).isEqualTo("+79991234567");
        assertThat(ClientService.normalizePhone("9991234567")).isEqualTo("+79991234567");
        assertThat(ClientService.normalizePhone("7(999)1234567")).isEqualTo("+79991234567");
    }

    @Test
    void normalizePhone_keepsForeignNumbersWithLeadingPlus() {
        assertThat(ClientService.normalizePhone("+375 29 111 22 33")).isEqualTo("+375291112233");
    }

    @Test
    void normalizePhone_nullAndNoDigitsPassThrough() {
        assertThat(ClientService.normalizePhone(null)).isNull();
        assertThat(ClientService.normalizePhone("  нет номера  ")).isEqualTo("нет номера");
    }

    // ── createClient ────────────────────────────────────────

    @Test
    void createClient_normalizesPhoneAndDefaultsActive() {
        when(clientRepository.findByPhone("+79990001122")).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        Client saved = service.createClient(new Client("Иван", "8 999 000 11 22"));

        assertThat(saved.getPhone()).isEqualTo("+79990001122");
        assertThat(saved.getIsActive()).isTrue();
    }

    @Test
    void createClient_duplicatePhoneThrowsWithExistingClient() {
        Client existing = new Client("Старый", "+79990001122");
        existing.setId(7L);
        when(clientRepository.findByPhone("+79990001122")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createClient(new Client("Новый", "+7 999 000 11 22")))
                .isInstanceOf(DuplicateClientException.class)
                .extracting(e -> ((DuplicateClientException) e).getExisting())
                .isEqualTo(existing);
    }

    @Test
    void updateClient_rejectsPhoneThatBelongsToAnotherClient() {
        Client target = new Client("Цель", "+79990000001");
        target.setId(1L);
        Client other = new Client("Другой", "+79990000002");
        other.setId(2L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(target));
        when(clientRepository.findByPhone("+79990000002")).thenReturn(Optional.of(other));

        Client data = new Client("Цель", "+7 999 000 00 02");
        assertThatThrownBy(() -> service.updateClient(1L, data))
                .isInstanceOf(DuplicateClientException.class);
    }

    @Test
    void updateClient_allowsKeepingItsOwnPhone() {
        Client target = new Client("Цель", "+79990000001");
        target.setId(1L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(target));
        when(clientRepository.findByPhone("+79990000001")).thenReturn(Optional.of(target));
        when(clientRepository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        Client data = new Client("Цель Новая", "8 999 000 00 01");
        Client result = service.updateClient(1L, data);

        assertThat(result.getName()).isEqualTo("Цель Новая");
        assertThat(result.getPhone()).isEqualTo("+79990000001");
    }

    // ── summary ─────────────────────────────────────────────

    @Test
    void summary_aggregatesRevenueDebtAndOrderCount() {
        Order paid = order(1000.0, 1000.0, LocalDateTime.of(2026, 1, 1, 10, 0));
        Order partial = order(500.0, 200.0, LocalDateTime.of(2026, 3, 1, 10, 0));
        Order overpaid = order(300.0, 400.0, LocalDateTime.of(2026, 2, 1, 10, 0));
        when(orderRepository.findByClientId(5L)).thenReturn(List.of(paid, partial, overpaid));

        ClientSummary s = service.summary(5L);

        assertThat(s.ordersCount()).isEqualTo(3);
        assertThat(s.revenue()).isEqualTo(1800.0);
        assertThat(s.debt()).isEqualTo(300.0); // only the partial order; overpaid clamps to 0
        assertThat(s.lastOrderAt()).isEqualTo(LocalDateTime.of(2026, 3, 1, 10, 0));
    }

    @Test
    void summary_emptyClientHasZeroesAndNullLastOrder() {
        when(orderRepository.findByClientId(5L)).thenReturn(List.of());

        ClientSummary s = service.summary(5L);

        assertThat(s.ordersCount()).isZero();
        assertThat(s.revenue()).isZero();
        assertThat(s.debt()).isZero();
        assertThat(s.lastOrderAt()).isNull();
    }

    // ── журнал / устройства ─────────────────────────────────

    @Test
    void addNote_rejectsBlankText() {
        lenient().when(clientRepository.findById(1L)).thenReturn(Optional.of(new Client("Иван", "+79990000001")));
        assertThatThrownBy(() -> service.addNote(1L, "NOTE", "   ", "admin"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void addDevice_rejectsBlankModel() {
        lenient().when(clientRepository.findById(1L)).thenReturn(Optional.of(new Client("Иван", "+79990000001")));
        ClientDevice d = new ClientDevice();
        d.setModel("  ");
        assertThatThrownBy(() -> service.addDevice(1L, d)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void addNote_onMissingClientThrows() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addNote(99L, "NOTE", "текст", "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найден");
    }

    // ── дубли ───────────────────────────────────────────────

    @Test
    void findPossibleDuplicates_excludesTheClientItself() {
        Client self = new Client("Я", "+79990000001");
        self.setId(1L);
        when(clientRepository.findByPhone("+79990000001")).thenReturn(Optional.of(self));

        assertThat(service.findPossibleDuplicates("8 999 000 00 01", 1L)).isEmpty();
        assertThat(service.findPossibleDuplicates("8 999 000 00 01", 2L)).containsExactly(self);
    }

    private static Order order(double total, double paid, LocalDateTime createdAt) {
        Order o = new Order();
        o.setTotalPrice(total);
        o.setPaidAmount(paid);
        o.setCreatedAt(createdAt);
        return o;
    }
}
