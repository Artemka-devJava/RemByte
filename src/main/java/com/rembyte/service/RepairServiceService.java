package com.rembyte.service;

import com.rembyte.model.RepairService;
import com.rembyte.repository.RepairServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления услугами ремонта
 */
@Service
@Transactional
public class RepairServiceService {
    private final RepairServiceRepository serviceRepository;

    public RepairServiceService(RepairServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public RepairService createService(RepairService service) {
        return serviceRepository.save(service);
    }

    public Optional<RepairService> getServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    public List<RepairService> getAllServices() {
        return serviceRepository.findAll();
    }

    public List<RepairService> getActiveServices() {
        return serviceRepository.findByIsActiveTrue();
    }

    public List<RepairService> getServicesByCategory(String category) {
        return serviceRepository.findByCategory(category);
    }

    public Optional<RepairService> findByName(String name) {
        return serviceRepository.findByName(name);
    }

    public RepairService updateService(Long id, RepairService serviceData) {
        return serviceRepository.findById(id).map(service -> {
            service.setName(serviceData.getName());
            service.setDescription(serviceData.getDescription());
            service.setBasePrice(serviceData.getBasePrice());
            service.setCategory(serviceData.getCategory());
            service.setIsActive(serviceData.getIsActive());
            return serviceRepository.save(service);
        }).orElseThrow(() -> new RuntimeException("Услуга не найдена"));
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    /**
     * Инициализация стандартных услуг
     */
    public void initializeDefaultServices() {
        if (serviceRepository.count() == 0) {
            RepairService s1 = new RepairService();
            s1.setName("Замена BGA микросхемы");
            s1.setDescription("Замена вышедшей из строя микросхемы BGA (видеокарта, чипсет и т.д.)");
            s1.setBasePrice(3500.0);
            s1.setCategory("BGA");
            s1.setIsActive(true);
            serviceRepository.save(s1);

            RepairService s2 = new RepairService();
            s2.setName("Замена экрана (15 дюйм)");
            s2.setDescription("Замена разбитого или неработающего экрана");
            s2.setBasePrice(2000.0);
            s2.setCategory("Экран");
            s2.setIsActive(true);
            serviceRepository.save(s2);

            RepairService s3 = new RepairService();
            s3.setName("Замена батареи");
            s3.setDescription("Замена батареи ноутбука");
            s3.setBasePrice(1500.0);
            s3.setCategory("Батарея");
            s3.setIsActive(true);
            serviceRepository.save(s3);

            RepairService s4 = new RepairService();
            s4.setName("Замена материнской платы");
            s4.setDescription("Замена вышедшей из строя материнской платы");
            s4.setBasePrice(5000.0);
            s4.setCategory("Материнская плата");
            s4.setIsActive(true);
            serviceRepository.save(s4);

            RepairService s5 = new RepairService();
            s5.setName("Чистка и смена термопасты");
            s5.setDescription("Профилактическая чистка от пыли и смена термопасты");
            s5.setBasePrice(500.0);
            s5.setCategory("Обслуживание");
            s5.setIsActive(true);
            serviceRepository.save(s5);

            RepairService s6 = new RepairService();
            s6.setName("Замена жесткого диска на SSD");
            s6.setDescription("Замена старого жесткого диска на быстрый SSD");
            s6.setBasePrice(1200.0);
            s6.setCategory("Хранилище");
            s6.setIsActive(true);
            serviceRepository.save(s6);

            RepairService s7 = new RepairService();
            s7.setName("Восстановление данных");
            s7.setDescription("Восстановление данных с неработающего жесткого диска");
            s7.setBasePrice(2500.0);
            s7.setCategory("Данные");
            s7.setIsActive(true);
            serviceRepository.save(s7);

            RepairService s8 = new RepairService();
            s8.setName("Переустановка ОС");
            s8.setDescription("Переустановка операционной системы (Windows, Linux)");
            s8.setBasePrice(800.0);
            s8.setCategory("Программное обеспечение");
            s8.setIsActive(true);
            serviceRepository.save(s8);
        }
    }
}

