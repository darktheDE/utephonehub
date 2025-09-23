package com.example.service;

import com.example.model.Address;
import com.example.model.User;
import com.example.model.dto.AddressDTO;
import com.example.repository.AddressRepository;

import java.util.List;
import java.util.Optional;

public class AddressService {

    private AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public List<Address> getAddressesByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return addressRepository.findByUserId(userId);
    }

    public Optional<Address> getAddressById(Long addressId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID cannot be null");
        }
        return addressRepository.findById(addressId);
    }

    public Address createAddress(AddressDTO addressDTO, User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User is required for the address");
        }
        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(addressDTO.getRecipientName());
        address.setPhoneNumber(addressDTO.getPhoneNumber());
        address.setStreetAddress(addressDTO.getStreetAddress());
        address.setCity(addressDTO.getCity());
        address.setIsDefault(addressDTO.getIsDefault() != null && addressDTO.getIsDefault());

        return addressRepository.save(address);
    }

    public Address updateAddress(Long addressId, AddressDTO addressDTO, User user) {
        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found with id: " + addressId));

        existingAddress.setRecipientName(addressDTO.getRecipientName());
        existingAddress.setPhoneNumber(addressDTO.getPhoneNumber());
        existingAddress.setStreetAddress(addressDTO.getStreetAddress());
        existingAddress.setCity(addressDTO.getCity());
        existingAddress.setIsDefault(addressDTO.getIsDefault() != null && addressDTO.getIsDefault());

        return addressRepository.save(existingAddress);
    }

    public void deleteAddress(Long addressId) {
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID cannot be null");
        }
        addressRepository.deleteById(addressId);
    }

    public void setDefaultAddress(Long userId, Long addressId) {
        if (userId == null || addressId == null) {
            throw new IllegalArgumentException("User ID and Address ID cannot be null");
        }
        addressRepository.setDefaultAddress(userId, addressId);
    }

}
