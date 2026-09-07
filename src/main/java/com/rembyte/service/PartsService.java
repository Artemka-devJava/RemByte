package com.rembyte.service;

import com.rembyte.model.PartItem;
import com.rembyte.model.PartItemPhoto;
import com.rembyte.model.PartLot;
import com.rembyte.model.PartsBudget;
import com.rembyte.repository.PartItemPhotoRepository;
import com.rembyte.repository.PartItemRepository;
import com.rembyte.repository.PartLotRepository;
import com.rembyte.repository.PartsBudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Учёт закупки/перепродажи комплектующих: отдельные детали и сборные лоты.
 * Отдельный домен от клиентских заказов ({@link ClientService}/{@link OrderService}) —
 * деталь не привязана к клиенту-заказчику.
 */
@Service
@Transactional
public class PartsService {

    private static final long MAX_PHOTO_BYTES = 15L * 1024 * 1024;
    private static final String STATUS_IN_STOCK = "IN_STOCK";
    private static final String STATUS_SOLD = "SOLD";

    private final PartItemRepository itemRepository;
    private final PartLotRepository lotRepository;
    private final PartItemPhotoRepository photoRepository;
    private final PartsBudgetRepository budgetRepository;

    public PartsService(PartItemRepository itemRepository,
                        PartLotRepository lotRepository,
                        PartItemPhotoRepository photoRepository,
                        PartsBudgetRepository budgetRepository) {
        this.itemRepository = itemRepository;
        this.lotRepository = lotRepository;
        this.photoRepository = photoRepository;
        this.budgetRepository = budgetRepository;
    }

    // ── Детали ─────────────────────────────────────────────────

    public PartItem createItem(PartItem data) {
        PartItem item = new PartItem();
        applyItemFields(item, data);
        item.setStatus(STATUS_IN_STOCK);
        return itemRepository.save(item);
    }

    public List<PartItem> getAllItems(String status) {
        if (status == null || status.isBlank()) return itemRepository.findAllByOrderByCreatedAtDesc();
        return itemRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
    }

    public Optional<PartItem> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    public PartItem updateItem(Long id, PartItem data) {
        PartItem item = requireItem(id);
        applyItemFields(item, data);
        return itemRepository.save(item);
    }

    private void applyItemFields(PartItem item, PartItem data) {
        item.setTitle(data.getTitle());
        item.setCategory(data.getCategory());
        item.setSource(data.getSource());
        item.setPurchasePrice(data.getPurchasePrice());
        if (data.getPurchaseDate() != null) item.setPurchaseDate(data.getPurchaseDate());
        item.setNotes(data.getNotes());
    }

    public void deleteItem(Long id) {
        PartItem item = requireItem(id);
        photoRepository.deleteByItem_Id(id);
        itemRepository.delete(item);
    }

    public PartItem sellItem(Long id, Double salePrice, LocalDateTime saleDate) {
        PartItem item = requireItem(id);
        if (item.getLot() != null) {
            throw new IllegalArgumentException(
                    "Деталь в составе лота «" + item.getLot().getTitle() + "» — продайте лот целиком");
        }
        if (salePrice == null) throw new IllegalArgumentException("Не указана цена продажи");
        item.setSalePrice(salePrice);
        item.setSaleDate(saleDate != null ? saleDate : LocalDateTime.now());
        item.setStatus(STATUS_SOLD);
        return itemRepository.save(item);
    }

    // ── Фото деталей ───────────────────────────────────────────

    public List<PartItemPhoto> listPhotos(Long itemId) {
        return photoRepository.findByItem_IdOrderByCreatedAtDesc(itemId);
    }

    public List<PartItemPhoto> addPhotos(Long itemId, MultipartFile[] files) {
        PartItem item = requireItem(itemId);
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
            PartItemPhoto photo = new PartItemPhoto();
            photo.setItem(item);
            photo.setOriginalName(safeName(file.getOriginalFilename()));
            photo.setContentType(ct);
            try {
                photo.setContent(file.getBytes());
            } catch (Exception e) {
                throw new IllegalArgumentException("Не удалось прочитать файл: " + file.getOriginalFilename());
            }
            photoRepository.save(photo);
        }
        return listPhotos(itemId);
    }

    public Optional<PartItemPhoto> getPhoto(Long itemId, Long photoId) {
        return photoRepository.findById(photoId).filter(p -> p.getItem().getId().equals(itemId));
    }

    /** Байты фото вместе с типом — читаются внутри транзакции (LOB — LAZY). */
    public Optional<PhotoData> getPhotoData(Long itemId, Long photoId) {
        return getPhoto(itemId, photoId)
                .map(p -> new PhotoData(p.getContent(), p.getContentType(), p.getOriginalName()));
    }

    public void deletePhoto(Long itemId, Long photoId) {
        getPhoto(itemId, photoId).ifPresent(photoRepository::delete);
    }

    public record PhotoData(byte[] content, String contentType, String name) {}

    // ── Лоты ───────────────────────────────────────────────────

    public List<PartLot> getAllLots() {
        return lotRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<PartLot> getLotById(Long id) {
        return lotRepository.findById(id);
    }

    public PartLot createLot(String title, List<Long> itemIds) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Укажите название лота");
        if (itemIds == null || itemIds.size() < 2) {
            throw new IllegalArgumentException("В лот нужно выбрать минимум 2 детали");
        }
        PartLot lot = lotRepository.save(new PartLot(title.trim()));
        assignItemsToLot(lot, itemIds);
        return lot;
    }

    public PartLot updateLotItems(Long lotId, String title, List<Long> itemIds) {
        PartLot lot = requireLot(lotId);
        if (STATUS_SOLD.equals(lot.getStatus())) throw new IllegalArgumentException("Лот уже продан");
        if (title != null && !title.isBlank()) lot.setTitle(title.trim());

        List<PartItem> current = itemRepository.findByLot_Id(lotId);
        for (PartItem item : current) {
            if (itemIds == null || !itemIds.contains(item.getId())) {
                item.setLot(null);
                itemRepository.save(item);
            }
        }
        assignItemsToLot(lot, itemIds);
        return lotRepository.save(lot);
    }

    private void assignItemsToLot(PartLot lot, List<Long> itemIds) {
        if (itemIds == null) return;
        for (Long itemId : itemIds) {
            PartItem item = requireItem(itemId);
            if (item.getLot() != null && !item.getLot().getId().equals(lot.getId())) {
                throw new IllegalArgumentException("Деталь «" + item.getTitle() + "» уже в другом лоте");
            }
            if (STATUS_SOLD.equals(item.getStatus()) && (item.getLot() == null || !item.getLot().getId().equals(lot.getId()))) {
                throw new IllegalArgumentException("Деталь «" + item.getTitle() + "» уже продана");
            }
            item.setLot(lot);
            itemRepository.save(item);
            // Держим коллекцию lot.items в актуальном состоянии в памяти: без этого только что
            // созданный/изменённый лот сериализуется с пустым items/costTotal=0 до перезагрузки из БД.
            if (lot.getItems().stream().noneMatch(existing -> existing.getId() != null && existing.getId().equals(item.getId()))) {
                lot.getItems().add(item);
            }
        }
    }

    public PartLot sellLot(Long lotId, Double salePrice, LocalDateTime saleDate) {
        PartLot lot = requireLot(lotId);
        if (salePrice == null) throw new IllegalArgumentException("Не указана цена продажи");
        List<PartItem> items = itemRepository.findByLot_Id(lotId);
        if (items.isEmpty()) throw new IllegalArgumentException("В лоте нет деталей");

        LocalDateTime when = saleDate != null ? saleDate : LocalDateTime.now();
        lot.setSalePrice(salePrice);
        lot.setSaleDate(when);
        lot.setStatus(STATUS_SOLD);
        lotRepository.save(lot);

        for (PartItem item : items) {
            item.setStatus(STATUS_SOLD);
            item.setSaleDate(when);
            itemRepository.save(item);
        }
        return lot;
    }

    /** Разобрать лот: детали возвращаются в самостоятельную продажу, лот удаляется. */
    public void disbandLot(Long lotId) {
        PartLot lot = requireLot(lotId);
        if (STATUS_SOLD.equals(lot.getStatus())) {
            throw new IllegalArgumentException("Лот уже продан — разобрать нельзя");
        }
        List<PartItem> items = itemRepository.findByLot_Id(lotId);
        for (PartItem item : items) {
            item.setLot(null);
            itemRepository.save(item);
        }
        lotRepository.delete(lot);
    }

    // ── Бюджет ─────────────────────────────────────────────────

    public PartsBudget getBudget() {
        return budgetRepository.findById(1L).orElseGet(() -> budgetRepository.save(new PartsBudget()));
    }

    public PartsBudget setStartingAmount(Double amount) {
        PartsBudget budget = getBudget();
        budget.setStartingAmount(amount);
        budget.setUpdatedAt(LocalDateTime.now());
        return budgetRepository.save(budget);
    }

    // ── Статистика ─────────────────────────────────────────────

    /** Статистика за период [from, to]; null-границы = без ограничения с этой стороны. */
    public PartsStatistics getStatistics(LocalDateTime from, LocalDateTime to) {
        PartsStatistics stats = new PartsStatistics();

        List<PartItem> allItems = itemRepository.findAll();
        List<PartLot> allLots = lotRepository.findAll();

        // Баланс и стоимость склада считаются всегда за всё время, независимо от периода.
        double totalPurchasedAllTime = 0.0;
        double totalRevenueStandaloneAllTime = 0.0;
        double inventoryValue = 0.0;
        for (PartItem item : allItems) {
            double purchase = nz(item.getPurchasePrice());
            totalPurchasedAllTime += purchase;
            boolean sold = STATUS_SOLD.equals(item.getStatus());
            boolean inLot = item.getLot() != null;
            if (sold && !inLot) totalRevenueStandaloneAllTime += nz(item.getSalePrice());
            if (!sold) inventoryValue += purchase;
        }
        double totalRevenueLotsAllTime = 0.0;
        for (PartLot lot : allLots) {
            if (STATUS_SOLD.equals(lot.getStatus())) totalRevenueLotsAllTime += nz(lot.getSalePrice());
        }
        double startingAmount = nz(getBudget().getStartingAmount());
        stats.setCurrentBalance(startingAmount - totalPurchasedAllTime
                + totalRevenueStandaloneAllTime + totalRevenueLotsAllTime);
        stats.setInventoryValue(inventoryValue);

        // Показатели за выбранный период.
        LocalDateTime periodFrom = from != null ? from : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime periodTo = to != null ? to : LocalDateTime.now();

        int purchasedCount = 0;
        double purchasedSum = 0.0;
        for (PartItem item : allItems) {
            if (inRange(item.getPurchaseDate(), periodFrom, periodTo)) {
                purchasedCount++;
                purchasedSum += nz(item.getPurchasePrice());
            }
        }

        int soldCount = 0;
        double revenueSum = 0.0;
        double profitSum = 0.0;
        for (PartItem item : allItems) {
            if (item.getLot() != null) continue; // учитывается через лот
            if (!STATUS_SOLD.equals(item.getStatus()) || !inRange(item.getSaleDate(), periodFrom, periodTo)) continue;
            soldCount++;
            revenueSum += nz(item.getSalePrice());
            profitSum += nz(item.getSalePrice()) - nz(item.getPurchasePrice());
        }
        for (PartLot lot : allLots) {
            if (!STATUS_SOLD.equals(lot.getStatus()) || !inRange(lot.getSaleDate(), periodFrom, periodTo)) continue;
            soldCount++;
            revenueSum += nz(lot.getSalePrice());
            profitSum += nz(lot.getProfit());
        }

        stats.setPurchasedCount(purchasedCount);
        stats.setPurchasedSum(purchasedSum);
        stats.setSoldCount(soldCount);
        stats.setRevenueSum(revenueSum);
        stats.setProfitSum(profitSum);
        return stats;
    }

    // ── Вспомогательное ────────────────────────────────────────

    private PartItem requireItem(Long id) {
        return itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Деталь не найдена"));
    }

    private PartLot requireLot(Long id) {
        return lotRepository.findById(id).orElseThrow(() -> new RuntimeException("Лот не найден"));
    }

    private static boolean inRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        return value != null && !value.isBefore(from) && !value.isAfter(to);
    }

    private static double nz(Double v) { return v == null ? 0.0 : v; }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "photo.jpg";
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}
