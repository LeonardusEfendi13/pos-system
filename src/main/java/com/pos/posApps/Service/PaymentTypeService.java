package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.PaymentTypeDTO;
import com.pos.posApps.DTO.Dtos.PaymentTypeRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.PaymentMethodEntity;
import com.pos.posApps.Entity.PaymentTypeEntity;
import com.pos.posApps.Repository.PaymentMethodRepository;
import com.pos.posApps.Repository.PaymentTypeRepository;
import com.pos.posApps.Util.PaymentTypeRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class PaymentTypeService {
    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    public List<PaymentTypeDTO> list(Long clientId) {
        return paymentTypeRepository
                .findByClientEntity_ClientIdAndDeletedAtIsNullOrderByPaymentTypeIdAsc(clientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public PaymentTypeEntity resolve(Long clientId, Long paymentTypeId) {
        if (paymentTypeId == null) {
            return null;
        }

        return paymentTypeRepository
                .findByPaymentTypeIdAndClientEntity_ClientIdAndDeletedAtIsNull(paymentTypeId, clientId)
                .orElse(null);
    }

    // Name of the client's single credit type, saved on hutang sales
    public String creditTypeName(Long clientId) {
        return paymentTypeRepository
                .findFirstByClientEntity_ClientIdAndBehaviorAndDeletedAtIsNullOrderByPaymentTypeIdAsc(clientId, "credit")
                .map(PaymentTypeEntity::getName)
                .orElse("Kredit");
    }

    public ResponseInBoolean create(ClientEntity client, PaymentTypeRequest request) {
        String error = PaymentTypeRules.validate(request.getName(), request.getBehavior(), null);
        if (error != null) {
            return new ResponseInBoolean(false, error);
        }

        String name = request.getName().trim();
        if (paymentTypeRepository.existsByClientEntity_ClientIdAndNameIgnoreCaseAndDeletedAtIsNull(
                client.getClientId(), name)) {
            return new ResponseInBoolean(false, "Nama tipe sudah dipakai");
        }

        PaymentTypeEntity entity = new PaymentTypeEntity();
        entity.setClientEntity(client);
        entity.setName(name);
        entity.setBehavior(PaymentTypeRules.normalizeBehavior(request.getBehavior()));
        entity.setSystemDefault(false);
        paymentTypeRepository.save(entity);
        return new ResponseInBoolean(true, "Tipe pembayaran berhasil disimpan");
    }

    @Transactional
    public ResponseInBoolean update(Long clientId, Long paymentTypeId, PaymentTypeRequest request) {
        PaymentTypeEntity entity = resolve(clientId, paymentTypeId);
        if (entity == null) {
            return new ResponseInBoolean(false, "Tipe pembayaran tidak ditemukan");
        }

        String error = PaymentTypeRules.validate(request.getName(), request.getBehavior(), entity.getBehavior());
        if (error != null) {
            return new ResponseInBoolean(false, error);
        }

        String name = request.getName().trim();
        if (paymentTypeRepository.existsByClientEntity_ClientIdAndNameIgnoreCaseAndDeletedAtIsNullAndPaymentTypeIdNot(
                clientId, name, paymentTypeId)) {
            return new ResponseInBoolean(false, "Nama tipe sudah dipakai");
        }

        String behavior = PaymentTypeRules.normalizeBehavior(request.getBehavior());
        boolean behaviorChanged = !behavior.equals(entity.getBehavior());
        entity.setName(name);
        entity.setBehavior(behavior);
        paymentTypeRepository.save(entity);

        if (behaviorChanged) {
            List<PaymentMethodEntity> methods =
                    paymentMethodRepository.findByPaymentTypeEntity_PaymentTypeIdAndDeletedAtIsNull(paymentTypeId);
            for (PaymentMethodEntity method : methods) {
                method.setMethodType(behavior);
            }
            paymentMethodRepository.saveAll(methods);
        }

        return new ResponseInBoolean(true, "Tipe pembayaran berhasil disimpan");
    }

    public ResponseInBoolean delete(Long clientId, Long paymentTypeId) {
        PaymentTypeEntity entity = resolve(clientId, paymentTypeId);
        if (entity == null) {
            return new ResponseInBoolean(false, "Tipe pembayaran tidak ditemukan");
        }

        String error = PaymentTypeRules.deleteError(
                entity.getBehavior(),
                paymentMethodRepository.existsByPaymentTypeEntity_PaymentTypeIdAndDeletedAtIsNull(paymentTypeId)
        );
        if (error != null) {
            return new ResponseInBoolean(false, error);
        }

        entity.setDeletedAt(getCurrentTimestamp());
        paymentTypeRepository.save(entity);
        return new ResponseInBoolean(true, "Tipe pembayaran berhasil dihapus");
    }

    private PaymentTypeDTO toDto(PaymentTypeEntity entity) {
        return new PaymentTypeDTO(
                entity.getPaymentTypeId(),
                entity.getName(),
                entity.getBehavior(),
                entity.isSystemDefault()
        );
    }
}
