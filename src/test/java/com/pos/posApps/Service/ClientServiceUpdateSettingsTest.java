package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.DTO.Dtos.UpdateClientSettingsRequest;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceUpdateSettingsTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    private ClientEntity existingClient;

    @BeforeEach
    void setUp() {
        existingClient = new ClientEntity();
        existingClient.setClientId(1L);
        existingClient.setName("TOKO LAMA");
        existingClient.setAlamat("JL. LAMA 1");
        existingClient.setKota("KOTA LAMA");
        existingClient.setNoTelp("08123456789");
        existingClient.setCatatan("CATATAN LAMA");
        existingClient.setKingDiscYmh(BigDecimal.valueOf(21));
        existingClient.setKingDiscHnd(BigDecimal.valueOf(17));
    }

    private UpdateClientSettingsRequest validRequest() {
        return new UpdateClientSettingsRequest(
                "Toko Baru",
                "Jl. Baru 99",
                "Jakarta",
                "0812345",
                "Catatan baru",
                BigDecimal.valueOf(21.5),
                BigDecimal.valueOf(17)
        );
    }

    @Test
    void saveSuccess_allFieldsStoredUppercase() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingClient));

        ResponseInBoolean result = clientService.updateClientSettings(1L, validRequest());

        assertTrue(result.isStatus());
        assertEquals("Berhasil update setting", result.getMessage());
        verify(clientRepository).save(existingClient);

        assertEquals("TOKO BARU", existingClient.getName());
        assertEquals("JL. BARU 99", existingClient.getAlamat());
        assertEquals("JAKARTA", existingClient.getKota());
        assertEquals("0812345", existingClient.getNoTelp());
        assertEquals("CATATAN BARU", existingClient.getCatatan());
        assertEquals(BigDecimal.valueOf(21.5), existingClient.getKingDiscYmh());
        assertEquals(BigDecimal.valueOf(17), existingClient.getKingDiscHnd());
    }

    @Test
    void phoneStripsNonDigits() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingClient));

        UpdateClientSettingsRequest req = validRequest();
        req.setNoTelp("0812-345 678");

        clientService.updateClientSettings(1L, req);

        assertEquals("0812345678", existingClient.getNoTelp());
    }

    @Test
    void disc95IsAccepted() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingClient));

        UpdateClientSettingsRequest req = validRequest();
        req.setKingDiscYmh(BigDecimal.valueOf(95));
        req.setKingDiscHnd(BigDecimal.valueOf(95));

        ResponseInBoolean result = clientService.updateClientSettings(1L, req);

        assertTrue(result.isStatus());
        verify(clientRepository).save(any());
    }

    @Test
    void discAbove95YmhIsRejected() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingClient));

        UpdateClientSettingsRequest req = validRequest();
        req.setKingDiscYmh(BigDecimal.valueOf(95.1));

        ResponseInBoolean result = clientService.updateClientSettings(1L, req);

        assertFalse(result.isStatus());
        assertEquals("King Discount tidak boleh lebih besar dari 95%", result.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void discAbove95HndIsRejected() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingClient));

        UpdateClientSettingsRequest req = validRequest();
        req.setKingDiscHnd(BigDecimal.valueOf(96));

        ResponseInBoolean result = clientService.updateClientSettings(1L, req);

        assertFalse(result.isStatus());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void emptyNameIsRejected() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingClient));

        UpdateClientSettingsRequest req = validRequest();
        req.setName("   ");

        ResponseInBoolean result = clientService.updateClientSettings(1L, req);

        assertFalse(result.isStatus());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void nullNameIsRejected() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingClient));

        UpdateClientSettingsRequest req = validRequest();
        req.setName(null);

        ResponseInBoolean result = clientService.updateClientSettings(1L, req);

        assertFalse(result.isStatus());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void clientNotFoundReturnsError() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        ResponseInBoolean result = clientService.updateClientSettings(99L, validRequest());

        assertFalse(result.isStatus());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void nullDiscIsAllowed() {
        when(clientRepository.findByClientIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(existingClient));

        UpdateClientSettingsRequest req = validRequest();
        req.setKingDiscYmh(null);
        req.setKingDiscHnd(null);

        ResponseInBoolean result = clientService.updateClientSettings(1L, req);

        assertTrue(result.isStatus());
        assertNull(existingClient.getKingDiscYmh());
        assertNull(existingClient.getKingDiscHnd());
    }
}
