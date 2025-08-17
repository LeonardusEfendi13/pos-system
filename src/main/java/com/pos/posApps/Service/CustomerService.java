package com.pos.posApps.Service;

import com.pos.posApps.DTO.Dtos.EditUserRequest;
import com.pos.posApps.DTO.Dtos.RegisterRequest;
import com.pos.posApps.DTO.Dtos.UserDTO;
import com.pos.posApps.Entity.AccountEntity;
import com.pos.posApps.Entity.ClientEntity;
import com.pos.posApps.Entity.CustomerEntity;
import com.pos.posApps.Repository.AccountRepository;
import com.pos.posApps.Repository.ClientRepository;
import com.pos.posApps.Repository.CustomerRepository;
import com.pos.posApps.Util.Generator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.pos.posApps.Util.Generator.getCurrentTimestamp;

@Service
public class CustomerService {
    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public boolean doCreateCustomer(String customerName, String clientId){
        try{
            CustomerEntity customerEntity = customerRepository.findByNameAndDeletedAtIsNullAndClientEntity_ClientId(customerName, clientId);
            if(customerEntity != null){
                return false;
            }
            String lastCustomerId = customerRepository.findFirstByOrderByCustomerIdDesc().map(CustomerEntity::getCustomerId).orElse("CST0");
            String newCustomerId = Generator.generateId(lastCustomerId);

            ClientEntity clientEntity = clientRepository.findByClientIdAndDeletedAtIsNull(clientId);

            CustomerEntity newCustomerEntity = new CustomerEntity();
            newCustomerEntity.setCustomerId(newCustomerId);
            newCustomerEntity.setName(customerName);
            newCustomerEntity.setClientEntity(clientEntity);
            customerRepository.save(newCustomerEntity);
            return true;
        }catch (Exception e){
            return false;
        }

    }

    public boolean doUpdateCustomer(String customerId, String customerName){
        CustomerEntity customerEntity = customerRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
        if(customerEntity != null){
            System.out.println("Customer found");
            customerEntity.setName(customerName);
            customerRepository.save(customerEntity);
            return true;
        }else{
            System.out.println("Customer Not Found");
            return false;
        }
    }

    public List<CustomerEntity> getCustomerList(String clientId){
        return customerRepository.findAllByClientEntity_ClientIdAndDeletedAtIsNullOrderByCustomerIdAsc(clientId);
    }

    public boolean deleteCustomer(String customerId){
        CustomerEntity customerEntity = customerRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
        if(customerEntity == null){
            System.out.println("Customer Not Found");
            return false;
        }

        customerEntity.setDeletedAt(getCurrentTimestamp());
        customerRepository.save(customerEntity);
        return true;
    }
}
