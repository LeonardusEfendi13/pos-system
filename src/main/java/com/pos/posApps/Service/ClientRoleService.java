package com.pos.posApps.Service;

import com.pos.posApps.DTO.Enum.EnumRole.Roles;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientRoleEntity;
import com.pos.posApps.Entity.RoleMenuGrantEntity;
import com.pos.posApps.Repository.AccountRepository;
import com.pos.posApps.Repository.ClientRoleRepository;
import com.pos.posApps.Repository.RoleMenuGrantRepository;
import com.pos.posApps.Util.MenuCatalog;
import com.pos.posApps.Util.UserAssignment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
public class ClientRoleService {
    private final ClientRoleRepository clientRoleRepository;
    private final RoleMenuGrantRepository roleMenuGrantRepository;
    private final AccountRepository accountRepository;

    public ClientRoleService(
            ClientRoleRepository clientRoleRepository,
            RoleMenuGrantRepository roleMenuGrantRepository,
            AccountRepository accountRepository
    ) {
        this.clientRoleRepository = clientRoleRepository;
        this.roleMenuGrantRepository = roleMenuGrantRepository;
        this.accountRepository = accountRepository;
    }

    public List<ClientRoleView> list(Long clientId) {
        return clientRoleRepository.findByClientIdOrderByNameAsc(clientId).stream()
                .map(role -> new ClientRoleView(
                        role.getClientRoleId(),
                        role.getName(),
                        accountRepository.countByClientRole_ClientRoleIdAndDeletedAtIsNull(role.getClientRoleId()),
                        keysFor(role.getClientRoleId())
                ))
                .toList();
    }

    @Transactional
    public Optional<String> create(Long clientId, String rawName) {
        String name = normalizeName(rawName);
        String invalid = validateName(clientId, name, null);
        if (invalid != null) {
            return Optional.of(invalid);
        }

        ClientRoleEntity role = new ClientRoleEntity();
        role.setClientId(clientId);
        role.setName(name);
        role.setSeeded(true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        clientRoleRepository.save(role);
        return Optional.empty();
    }

    @Transactional
    public Optional<String> update(Long clientId, Long clientRoleId, String rawName, List<String> menuKeys) {
        ClientRoleEntity role = clientRoleRepository.findByClientRoleIdAndClientId(clientRoleId, clientId).orElse(null);
        if (role == null) {
            return Optional.of("Role tidak ditemukan");
        }

        String name = normalizeName(rawName);
        String invalid = validateName(clientId, name, role.getClientRoleId());
        if (invalid != null) {
            return Optional.of(invalid);
        }

        List<String> keys = sanitizeKeys(menuKeys);
        if (keys == null) {
            return Optional.of("Menu tidak valid");
        }

        role.setName(name);
        role.setUpdatedAt(LocalDateTime.now());
        clientRoleRepository.save(role);
        roleMenuGrantRepository.deleteByClientRoleId(role.getClientRoleId());
        for (String key : keys) {
            RoleMenuGrantEntity grant = new RoleMenuGrantEntity();
            grant.setClientRoleId(role.getClientRoleId());
            grant.setMenuKey(key);
            roleMenuGrantRepository.save(grant);
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<String> delete(Long clientId, Long clientRoleId) {
        ClientRoleEntity role = clientRoleRepository.findByClientRoleIdAndClientId(clientRoleId, clientId).orElse(null);
        if (role == null) {
            return Optional.of("Role tidak ditemukan");
        }
        long used = accountRepository.countByClientRole_ClientRoleIdAndDeletedAtIsNull(clientRoleId);
        if (used > 0) {
            return Optional.of("Role masih dipakai user");
        }
        roleMenuGrantRepository.deleteByClientRoleId(clientRoleId);
        clientRoleRepository.delete(role);
        return Optional.empty();
    }

    public ClientRoleEntity findOwned(Long clientId, Long clientRoleId) {
        if (clientId == null || clientRoleId == null) {
            return null;
        }
        return clientRoleRepository.findByClientRoleIdAndClientId(clientRoleId, clientId).orElse(null);
    }

    public Roles operationalColumnRole() {
        return Roles.ADMIN_1;
    }

    public boolean assignable(boolean godAdmin, ClientRoleEntity role) {
        return UserAssignment.create(godAdmin, role != null) != UserAssignment.Kind.INVALID
                || godAdmin;
    }

    private List<String> keysFor(Long clientRoleId) {
        return roleMenuGrantRepository.findByClientRoleId(clientRoleId).stream()
                .map(RoleMenuGrantEntity::getMenuKey)
                .filter(MenuCatalog::isKnownKey)
                .toList();
    }

    private List<String> sanitizeKeys(List<String> menuKeys) {
        if (menuKeys == null) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String key : menuKeys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            if (!MenuCatalog.isKnownKey(key)) {
                return null;
            }
            unique.add(key);
        }
        return new ArrayList<>(unique);
    }

    private String validateName(Long clientId, String name, Long currentId) {
        if (name.isBlank()) {
            return "Nama role wajib diisi";
        }
        if (name.length() > 80) {
            return "Nama role maksimal 80 karakter";
        }
        if (UserAssignment.reservedRoleName(name)) {
            return "Nama role tidak valid";
        }
        Optional<ClientRoleEntity> existing = clientRoleRepository.findByClientIdOrderByNameAsc(clientId).stream()
                .filter(role -> role.getName().equalsIgnoreCase(name))
                .findFirst();
        if (existing.isPresent() && (currentId == null || !existing.get().getClientRoleId().equals(currentId))) {
            return "Nama role sudah dipakai";
        }
        return null;
    }

    private String normalizeName(String rawName) {
        return rawName == null ? "" : rawName.trim();
    }

    public record ClientRoleView(Long clientRoleId, String name, long userCount, List<String> menuKeys) {
    }
}
