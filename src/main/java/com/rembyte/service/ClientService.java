package com.rembyte.service;

import com.rembyte.model.Client;
import com.rembyte.model.ClientDevice;
import com.rembyte.model.ClientNote;
import com.rembyte.model.ClientPhoto;
import com.rembyte.model.Order;
import com.rembyte.repository.ClientDeviceRepository;
import com.rembyte.repository.ClientNoteRepository;
import com.rembyte.repository.ClientPhotoRepository;
import com.rembyte.repository.ClientRepository;
import com.rembyte.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Сервис ведения клиентов: карточка, журнал, устройства, фото, архив.
 */
@Service
@Transactional
public class ClientService {

    private static final long MAX_PHOTO_BYTES = 15L * 1024 * 1024;

    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;
    private final ClientNoteRepository noteRepository;
    private final ClientDeviceRepository deviceRepository;
    private final ClientPhotoRepository photoRepository;

    public ClientService(ClientRepository clientRepository,
                         OrderRepository orderRepository,
                         ClientNoteRepository noteRepository,
                         ClientDeviceRepository deviceRepository,
                         ClientPhotoRepository photoRepository) {
        this.clientRepository = clientRepository;
        this.orderRepository = orderRepository;
        this.noteRepository = noteRepository;
        this.deviceRepository = deviceRepository;
        this.photoRepository = photoRepository;
    }

    // ── Карточка ────────────────────────────────────────────────

    public Client createClient(Client client) {
        client.setPhone(normalizePhone(client.getPhone()));
        clientRepository.findByPhone(client.getPhone())
                .ifPresent(existing -> { throw new DuplicateClientException(existing); });
        if (client.getIsActive() == null) client.setIsActive(true);
        return clientRepository.save(client);
    }

    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public List<Client> getActiveClients() {
        return clientRepository.findByIsActiveTrue();
    }

    public Optional<Client> findByPhone(String phone) {
        return clientRepository.findByPhone(normalizePhone(phone));
    }

    public Optional<Client> findByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    public List<Client> searchByName(String name) {
        String q = name == null ? "" : name.trim();
        return clientRepository.searchByNameOrPhoneOrEmail(q);
    }

    /** Возможные дубли по нормализованному телефону (кроме самого клиента). */
    public List<Client> findPossibleDuplicates(String phone, Long excludeId) {
        String normalized = normalizePhone(phone);
        return clientRepository.findByPhone(normalized).stream()
                .filter(c -> excludeId == null || !c.getId().equals(excludeId))
                .toList();
    }

    public Client updateClient(Long id, Client data) {
        return clientRepository.findById(id).map(client -> {
            String newPhone = normalizePhone(data.getPhone());
            clientRepository.findByPhone(newPhone)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw new DuplicateClientException(other); });

            client.setName(data.getName());
            client.setPhone(newPhone);
            client.setEmail(data.getEmail());
            client.setAddress(data.getAddress());
            client.setNotes(data.getNotes());
            if (data.getIsActive() != null) client.setIsActive(data.getIsActive());
            client.setType(data.getType());
            client.setTags(data.getTags());
            client.setSource(data.getSource());
            client.setPreferredChannel(data.getPreferredChannel());
            client.setCompanyDetails(data.getCompanyDetails());

            applyConsent(client::getConsentPdnAt, client::setConsentPdnAt, data.getConsentPdnAt());
            applyConsent(client::getConsentMarketingAt, client::setConsentMarketingAt, data.getConsentMarketingAt());

            return clientRepository.save(client);
        }).orElseThrow(() -> new RuntimeException("Клиент не найден"));
    }

    public Client archiveClient(Long id) {
        return clientRepository.findById(id).map(client -> {
            client.setArchivedAt(LocalDateTime.now());
            client.setIsActive(false);
            return clientRepository.save(client);
        }).orElseThrow(() -> new RuntimeException("Клиент не найден"));
    }

    public Client restoreClient(Long id) {
        return clientRepository.findById(id).map(client -> {
            client.setArchivedAt(null);
            client.setIsActive(true);
            return clientRepository.save(client);
        }).orElseThrow(() -> new RuntimeException("Клиент не найден"));
    }

    public void deleteClient(Long id) {
        noteRepository.deleteByClient_Id(id);
        photoRepository.deleteByClient_Id(id);
        deviceRepository.deleteByClient_Id(id);
        clientRepository.deleteById(id);
    }

    public void deactivateClient(Long id) {
        clientRepository.findById(id).ifPresent(client -> {
            client.setIsActive(false);
            clientRepository.save(client);
        });
    }

    // ── Сводка ─────────────────────────────────────────────────

    public ClientSummary summary(Long clientId) {
        List<Order> orders = orderRepository.findByClientId(clientId);
        double revenue = orders.stream().mapToDouble(o -> nz(o.getTotalPrice())).sum();
        double debt = orders.stream()
                .mapToDouble(o -> Math.max(0, nz(o.getTotalPrice()) - nz(o.getPaidAmount())))
                .sum();
        LocalDateTime last = orders.stream()
                .map(Order::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return new ClientSummary(orders.size(), revenue, debt, last);
    }

    // ── Журнал ─────────────────────────────────────────────────

    public List<ClientNote> listNotes(Long clientId) {
        return noteRepository.findByClient_IdOrderByCreatedAtDesc(clientId);
    }

    public ClientNote addNote(Long clientId, String kind, String text, String author) {
        Client client = requireClient(clientId);
        if (text == null || text.isBlank()) throw new RuntimeException("Пустая запись");
        ClientNote note = new ClientNote();
        note.setClient(client);
        note.setKind(kind);
        note.setText(text.trim());
        note.setAuthor(author);
        return noteRepository.save(note);
    }

    public void deleteNote(Long clientId, Long noteId) {
        noteRepository.findById(noteId)
                .filter(n -> n.getClient().getId().equals(clientId))
                .ifPresent(noteRepository::delete);
    }

    // ── Устройства ─────────────────────────────────────────────

    public List<ClientDevice> listDevices(Long clientId) {
        return deviceRepository.findByClient_IdOrderByCreatedAtDesc(clientId);
    }

    public ClientDevice addDevice(Long clientId, ClientDevice data) {
        Client client = requireClient(clientId);
        if (data.getModel() == null || data.getModel().isBlank())
            throw new RuntimeException("Укажите модель устройства");
        ClientDevice device = new ClientDevice();
        device.setClient(client);
        device.setKind(data.getKind());
        device.setModel(data.getModel().trim());
        device.setSerialNumber(data.getSerialNumber());
        device.setNotes(data.getNotes());
        return deviceRepository.save(device);
    }

    public ClientDevice updateDevice(Long clientId, Long deviceId, ClientDevice data) {
        return deviceRepository.findById(deviceId)
                .filter(d -> d.getClient().getId().equals(clientId))
                .map(d -> {
                    d.setKind(data.getKind());
                    if (data.getModel() != null && !data.getModel().isBlank())
                        d.setModel(data.getModel().trim());
                    d.setSerialNumber(data.getSerialNumber());
                    d.setNotes(data.getNotes());
                    return deviceRepository.save(d);
                })
                .orElseThrow(() -> new RuntimeException("Устройство не найдено"));
    }

    public void deleteDevice(Long clientId, Long deviceId) {
        deviceRepository.findById(deviceId)
                .filter(d -> d.getClient().getId().equals(clientId))
                .ifPresent(deviceRepository::delete);
    }

    // ── Фото ───────────────────────────────────────────────────

    public List<ClientPhoto> listPhotos(Long clientId) {
        return photoRepository.findByClient_IdOrderByCreatedAtDesc(clientId);
    }

    public List<ClientPhoto> addPhotos(Long clientId, MultipartFile[] files, String caption, Long deviceId) {
        Client client = requireClient(clientId);
        ClientDevice device = deviceId == null ? null : deviceRepository.findById(deviceId)
                .filter(d -> d.getClient().getId().equals(clientId))
                .orElse(null);

        if (files == null || files.length == 0) throw new IllegalArgumentException("Файлы не переданы");

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String ct = file.getContentType() == null ? "" : file.getContentType();
            if (!ct.startsWith("image/")) {
                throw new IllegalArgumentException("Только изображения: " + file.getOriginalFilename());
            }
            if (file.getSize() > MAX_PHOTO_BYTES) {
                throw new IllegalArgumentException("Файл больше 15 МБ: " + file.getOriginalFilename());
            }
            ClientPhoto photo = new ClientPhoto();
            photo.setClient(client);
            photo.setDevice(device);
            photo.setOriginalName(safeName(file.getOriginalFilename()));
            photo.setContentType(ct);
            photo.setCaption(caption == null || caption.isBlank() ? null : caption.trim());
            try {
                photo.setContent(file.getBytes());
            } catch (Exception e) {
                throw new IllegalArgumentException("Не удалось прочитать файл: " + file.getOriginalFilename());
            }
            photoRepository.save(photo);
        }
        return listPhotos(clientId);
    }

    public Optional<ClientPhoto> getPhoto(Long clientId, Long photoId) {
        return photoRepository.findById(photoId)
                .filter(p -> p.getClient().getId().equals(clientId));
    }

    /** Байты фото вместе с типом — читаются внутри транзакции (LOB — LAZY). */
    public Optional<PhotoData> getPhotoData(Long clientId, Long photoId) {
        return getPhoto(clientId, photoId)
                .map(p -> new PhotoData(p.getContent(), p.getContentType(), p.getOriginalName()));
    }

    public void deletePhoto(Long clientId, Long photoId) {
        getPhoto(clientId, photoId).ifPresent(photoRepository::delete);
    }

    public record PhotoData(byte[] content, String contentType, String name) {}

    // ── Вспомогательное ────────────────────────────────────────

    private Client requireClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
    }

    private static double nz(Double v) { return v == null ? 0.0 : v; }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "photo.jpg";
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private static void applyConsent(java.util.function.Supplier<LocalDateTime> current,
                                     java.util.function.Consumer<LocalDateTime> setter,
                                     LocalDateTime incoming) {
        // Фронт присылает non-null (любую дату) = согласие есть, null = согласия нет.
        boolean wanted = incoming != null;
        boolean has = current.get() != null;
        if (wanted && !has) setter.accept(LocalDateTime.now());
        if (!wanted && has) setter.accept(null);
    }

    /**
     * Привести телефон к единому виду: только цифры, российские номера — как +7XXXXXXXXXX.
     */
    static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) return raw.trim();
        if (digits.length() == 11 && (digits.startsWith("8") || digits.startsWith("7"))) {
            return "+7" + digits.substring(1);
        }
        if (digits.length() == 10) {
            return "+7" + digits;
        }
        return "+" + digits;
    }
}
