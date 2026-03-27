package com.rembyte.service;

import com.rembyte.model.Client;
import com.rembyte.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления клиентами
 */
@Service
@Transactional
public class ClientService {
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client createClient(Client client) {
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
        return clientRepository.findByPhone(phone);
    }

    public Optional<Client> findByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    public List<Client> searchByName(String name) {
        return clientRepository.findByNameContainingIgnoreCase(name);
    }

    public Client updateClient(Long id, Client clientData) {
        return clientRepository.findById(id).map(client -> {
            client.setName(clientData.getName());
            client.setPhone(clientData.getPhone());
            client.setEmail(clientData.getEmail());
            client.setAddress(clientData.getAddress());
            client.setNotes(clientData.getNotes());
            client.setIsActive(clientData.getIsActive());
            return clientRepository.save(client);
        }).orElseThrow(() -> new RuntimeException("Клиент не найден"));
    }

    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }

    public void deactivateClient(Long id) {
        clientRepository.findById(id).ifPresent(client -> {
            client.setIsActive(false);
            clientRepository.save(client);
        });
    }
}

