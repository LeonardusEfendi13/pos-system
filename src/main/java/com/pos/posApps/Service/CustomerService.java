package com.pos.posApps.Service;

import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Repository.ClientRepository;
import com.pos.posApps.Repository.CustomerRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class CustomerService {
    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public boolean doCreateCustomer(String customerName, String alamat, Long clientId) {
        try {
            CustomerEntity customerEntity = customerRepository.findByNameAndDeletedAtIsNullAndClientEntity_ClientId(customerName, clientId);
            if (customerEntity != null) {
                return false;
            }
            Long lastCustomerId = customerRepository.findFirstByOrderByCustomerIdDesc().map(CustomerEntity::getCustomerId).orElse(0L);
            Long newCustomerId = Generator.generateId(lastCustomerId);

            ClientEntity clientEntity = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);

            CustomerEntity newCustomerEntity = new CustomerEntity();
            newCustomerEntity.setCustomerId(newCustomerId);
            newCustomerEntity.setName(customerName);
            newCustomerEntity.setAlamat(alamat);
            newCustomerEntity.setClientEntity(clientEntity);
            customerRepository.save(newCustomerEntity);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public boolean doUpdateCustomer(Long customerId, String customerName, Long clientId, String customerAlamat) {
        //Get Supplier Entity
        Optional<CustomerEntity> customerEntityOpt = customerRepository.findByCustomerIdAndDeletedAtIsNullAndClientEntity_ClientId(customerId, clientId);
        if (customerEntityOpt.isEmpty()) {
            return false;
        }
        CustomerEntity customerEntity = customerEntityOpt.get();
        System.out.println("Customer found");
        customerEntity.setName(customerName);
        customerEntity.setAlamat(customerAlamat);
        customerRepository.save(customerEntity);
        return true;
    }

    public List<CustomerEntity> getCustomerList(Long clientId) {
        return customerRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByCustomerIdDesc(clientId);
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
