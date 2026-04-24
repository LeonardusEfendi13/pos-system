package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.ResponseInBoolean;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class CustomerService {
    @Autowired
    CustomerRepository customerRepository;

    @Transactional
    public ResponseInBoolean doCreateCustomer(String customerName, String alamat, ClientEntity clientData, Boolean isKing) {
        try {
            CustomerEntity customerEntity = customerRepository.findByNameAndDeletedAtIsNullAndClientEntity_ClientId(customerName, clientData.getClientId());
            if (customerEntity != null) {
                return new ResponseInBoolean(false, "Data Customer sudah ada");
            }
            CustomerEntity newCustomerEntity = new CustomerEntity();
            newCustomerEntity.setName(customerName);
            newCustomerEntity.setAlamat(alamat);
            newCustomerEntity.setClientEntity(clientData);
            newCustomerEntity.setKing(isKing);
            customerRepository.save(newCustomerEntity);
            return new ResponseInBoolean(true, "Berhasil tambah customer baru");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ResponseInBoolean(false, "Error : " + e.getMessage());
        }

    }

    public ResponseInBoolean doUpdateCustomer(Long customerId, String customerName, Long clientId, String customerAlamat, Boolean isKing) {
        //Get Supplier Entity
        Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(customerId, clientId);
        if (customerEntityOpt.isEmpty()) {
            return new ResponseInBoolean(false, "Data customer tidak ditemukan");
        }
        CustomerEntity customerEntity = customerEntityOpt.get();
        customerEntity.setName(customerName);
        customerEntity.setAlamat(customerAlamat);
        customerEntity.setKing(isKing);
        customerRepository.save(customerEntity);
        return new ResponseInBoolean(true, "Berhasil update data customer");
    }

    public List<CustomerEntity> getCustomerList(Long clientId) {
        return customerRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByName(clientId);
    }

    public boolean deleteCustomer(Long customerId, Long clientId) {
        Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(customerId, clientId);
        if (customerEntityOpt.isEmpty()) {
            return false;
        }
        CustomerEntity customerEntity = customerEntityOpt.get();
        customerEntity.setDeletedAt(getCurrentTimestamp());
        customerRepository.save(customerEntity);
        return true;
    }
}
