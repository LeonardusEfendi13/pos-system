package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.PaymentMethodDTO;
import com.pos.posApps.DTO.Dtos.PaymentMethodRequest;
import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.IndenEntity;
import com.pos.posApps.Entity.PaymentMethodEntity;
import com.pos.posApps.Entity.PaymentTypeEntity;
import com.pos.posApps.Entity.TransactionEntity;
import com.pos.posApps.Repository.PaymentMethodRepository;
import com.pos.posApps.Repository.PaymentTypeRepository;
import com.pos.posApps.Util.PaymentMethodRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class PaymentMethodService {
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    public List<PaymentMethodDTO> list(Long clientId) {
        return paymentMethodRepository
                .findByClientEntity_ClientIdAndDeletedAtIsNullOrderByPaymentMethodIdAsc(clientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ResponseInBoolean create(ClientEntity client, PaymentMethodRequest request) {
        PaymentTypeEntity type = resolveType(client.getClientId(), request);
        if (type == null) {
            return new ResponseInBoolean(false, "Tipe pembayaran tidak ditemukan");
        }

        String error = PaymentMethodRules.validate(
                request.getName(),
                type.getBehavior(),
                request.getRekening(),
                false,
                null
        );
        if (error != null) {
            return new ResponseInBoolean(false, error);
        }

        PaymentMethodEntity entity = new PaymentMethodEntity();
        entity.setClientEntity(client);
        apply(entity, request, type, false);
        paymentMethodRepository.save(entity);
        return new ResponseInBoolean(true, "Metode pembayaran berhasil disimpan");
    }

    public ResponseInBoolean update(Long clientId, Long paymentMethodId, PaymentMethodRequest request) {
        PaymentMethodEntity entity = paymentMethodRepository
                .findByPaymentMethodIdAndClientEntity_ClientIdAndDeletedAtIsNull(paymentMethodId, clientId)
                .orElse(null);
        if (entity == null) {
            return new ResponseInBoolean(false, "Metode pembayaran tidak ditemukan");
        }

        PaymentTypeEntity type = resolveType(clientId, request);
        if (type == null) {
            return new ResponseInBoolean(false, "Tipe pembayaran tidak ditemukan");
        }

        String error = PaymentMethodRules.validate(
                request.getName(),
                type.getBehavior(),
                request.getRekening(),
                entity.isSystemDefault(),
                entity.getMethodType()
        );
        if (error != null) {
            return new ResponseInBoolean(false, error);
        }

        apply(entity, request, type, entity.isSystemDefault());
        paymentMethodRepository.save(entity);
        return new ResponseInBoolean(true, "Metode pembayaran berhasil disimpan");
    }

    public ResponseInBoolean delete(Long clientId, Long paymentMethodId) {
        PaymentMethodEntity entity = paymentMethodRepository
                .findByPaymentMethodIdAndClientEntity_ClientIdAndDeletedAtIsNull(paymentMethodId, clientId)
                .orElse(null);
        if (entity == null) {
            return new ResponseInBoolean(false, "Metode pembayaran tidak ditemukan");
        }

        String error = PaymentMethodRules.deleteError(entity.isSystemDefault());
        if (error != null) {
            return new ResponseInBoolean(false, error);
        }

        entity.setDeletedAt(getCurrentTimestamp());
        paymentMethodRepository.save(entity);
        return new ResponseInBoolean(true, "Metode pembayaran berhasil dihapus");
    }

    public PaymentMethodEntity resolve(Long clientId, Long paymentMethodId, boolean forceDefault) {
        if (forceDefault || paymentMethodId == null) {
            return paymentMethodRepository
                    .findFirstByClientEntity_ClientIdAndSystemDefaultTrueAndDeletedAtIsNull(clientId)
                    .orElse(null);
        }

        return paymentMethodRepository
                .findByPaymentMethodIdAndClientEntity_ClientIdAndDeletedAtIsNull(paymentMethodId, clientId)
                .orElse(null);
    }

    public void copyToTransaction(TransactionEntity transaction, PaymentMethodEntity method) {
        transaction.setPaymentMethodId(method.getPaymentMethodId());
        transaction.setPaymentMethodName(method.getName());
        transaction.setPaymentMethodType(method.getMethodType());
        transaction.setPaymentMethodRekening(method.getRekening() == null ? "" : method.getRekening());
        transaction.setPaymentTypeName(typeName(method));
    }

    public void copyToInden(IndenEntity inden, PaymentMethodEntity method) {
        inden.setPaymentMethodId(method.getPaymentMethodId());
        inden.setPaymentMethodName(method.getName());
        inden.setPaymentMethodType(method.getMethodType());
        inden.setPaymentMethodRekening(method.getRekening() == null ? "" : method.getRekening());
        inden.setPaymentTypeName(typeName(method));
    }

    // Snapshot of the method's type name for report grouping
    public String typeName(PaymentMethodEntity method) {
        PaymentTypeEntity type = method.getPaymentTypeEntity();
        if (type != null && type.getName() != null && !type.getName().isBlank()) {
            return type.getName();
        }

        return switch (PaymentMethodRules.normalizeType(method.getMethodType())) {
            case "transfer" -> "Transfer";
            case "qris" -> "QRIS";
            case "credit" -> "Kredit";
            default -> "Cash";
        };
    }

    private PaymentTypeEntity resolveType(Long clientId, PaymentMethodRequest request) {
        if (request.getPaymentTypeId() != null) {
            return paymentTypeRepository
                    .findByPaymentTypeIdAndClientEntity_ClientIdAndDeletedAtIsNull(request.getPaymentTypeId(), clientId)
                    .orElse(null);
        }

        return paymentTypeRepository
                .findFirstByClientEntity_ClientIdAndBehaviorAndDeletedAtIsNullOrderByPaymentTypeIdAsc(
                        clientId,
                        PaymentMethodRules.normalizeType(request.getMethodType())
                )
                .orElse(null);
    }

    private void apply(
            PaymentMethodEntity entity,
            PaymentMethodRequest request,
            PaymentTypeEntity type,
            boolean systemDefault
    ) {
        String behavior = type.getBehavior();
        entity.setName(request.getName().trim());
        entity.setPaymentTypeEntity(type);
        entity.setMethodType(behavior);
        entity.setRekening(PaymentMethodRules.rekeningFor(behavior, request.getRekening()));
        entity.setSystemDefault(systemDefault);
    }

    private PaymentMethodDTO toDto(PaymentMethodEntity entity) {
        return new PaymentMethodDTO(
                entity.getPaymentMethodId(),
                entity.getName(),
                entity.getMethodType(),
                entity.getPaymentTypeEntity() == null ? null : entity.getPaymentTypeEntity().getPaymentTypeId(),
                typeName(entity),
                entity.getRekening() == null ? "" : entity.getRekening(),
                entity.isSystemDefault()
        );
    }
}
