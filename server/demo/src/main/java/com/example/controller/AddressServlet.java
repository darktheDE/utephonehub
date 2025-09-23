package com.example.controller;

import com.example.model.Address;
import com.example.model.User;
import com.example.model.dto.AddressDTO;
import com.example.service.AddressService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet("/api/addresses/*")
public class AddressServlet extends BaseServlet {

    private AddressService addressService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.addressService = (AddressService) getServletContext().getAttribute("addressService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User currentUser = (User) request.getAttribute("user");
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/addresses -> Get all addresses for current user
            handleGetUserAddresses(response, currentUser);
        } else {
            // GET /api/addresses/{id} -> Get address by ID
            handleGetAddressById(response, currentUser, pathInfo);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User currentUser = (User) request.getAttribute("user");
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            // POST /api/addresses -> Create a new address
            handleCreateAddress(request, response, currentUser);
        } else if (pathInfo.equals("/set-default")) {
            handleSetDefaultAddress(request, response, currentUser);
        } else {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User currentUser = (User) request.getAttribute("user");
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Address ID is required");
            return;
        }

        // PUT /api/addresses/{id} -> Update an address
        handleUpdateAddress(request, response, currentUser, pathInfo);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User currentUser = (User) request.getAttribute("user");
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Address ID is required");
            return;
        }
        
        // DELETE /api/addresses/{id} -> Delete an address
        handleDeleteAddress(response, currentUser, pathInfo);
    }

    private void handleGetUserAddresses(HttpServletResponse response, User currentUser) throws IOException {
        try {
            List<Address> addresses = addressService.getAddressesByUserId(currentUser.getId());
            sendSuccess(response, HttpServletResponse.SC_OK, "Addresses retrieved successfully", addresses);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleGetAddressById(HttpServletResponse response, User currentUser, String pathInfo) throws IOException {
        try {
            Long addressId = Long.parseLong(pathInfo.substring(1));
            Optional<Address> addressOpt = addressService.getAddressById(addressId);

            if (addressOpt.isEmpty()) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Address not found");
                return;
            }

            Address address = addressOpt.get();
            if (!address.getUser().getId().equals(currentUser.getId())) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "You are not authorized to view this address");
                return;
            }

            sendSuccess(response, HttpServletResponse.SC_OK, "Address retrieved successfully", address);
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID format");
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleCreateAddress(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        try {
            AddressDTO addressDTO = objectMapper.readValue(request.getReader(), AddressDTO.class);
            if (!validateRequest(addressDTO, response)) return;

            Address createdAddress = addressService.createAddress(addressDTO, currentUser);
            sendSuccess(response, HttpServletResponse.SC_CREATED, "Address created successfully", createdAddress);
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleUpdateAddress(HttpServletRequest request, HttpServletResponse response, User currentUser, String pathInfo) throws IOException {
        try {
            Long addressId = Long.parseLong(pathInfo.substring(1));
            Optional<Address> existingAddressOpt = addressService.getAddressById(addressId);

            if (existingAddressOpt.isEmpty()) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Address not found");
                return;
            }

            Address existingAddress = existingAddressOpt.get();
            if (!existingAddress.getUser().getId().equals(currentUser.getId())) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "You are not authorized to update this address");
                return;
            }

            AddressDTO addressDTO = objectMapper.readValue(request.getReader(), AddressDTO.class);
            if (!validateRequest(addressDTO, response)) return;


            Address result = addressService.updateAddress(addressId, addressDTO, currentUser);
            sendSuccess(response, HttpServletResponse.SC_OK, "Address updated successfully", result);
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID format");
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

    private void handleDeleteAddress(HttpServletResponse response, User currentUser, String pathInfo) throws IOException {
        try {
            Long addressId = Long.parseLong(pathInfo.substring(1));
            Optional<Address> addressOpt = addressService.getAddressById(addressId);

            if (addressOpt.isEmpty()) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Address not found");
                return;
            }

            if (!addressOpt.get().getUser().getId().equals(currentUser.getId())) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "You are not authorized to delete this address");
                return;
            }

            addressService.deleteAddress(addressId);
            sendSuccess(response, HttpServletResponse.SC_OK, "Address deleted successfully", null);
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID format");
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }
    
    private void handleSetDefaultAddress(HttpServletRequest request, HttpServletResponse response, User currentUser) throws IOException {
        try {
            Map<String, Long> requestBody = objectMapper.readValue(request.getReader(), Map.class);
            Long addressId = requestBody.get("addressId");

            if (addressId == null) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "addressId is required");
                return;
            }

            // Security check: Ensure the address belongs to the current user
            Optional<Address> addressToSetOpt = addressService.getAddressById(addressId);
            if (addressToSetOpt.isEmpty()) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Address not found");
                return;
            }

            Address addressToSet = addressToSetOpt.get();
            if (!addressToSet.getUser().getId().equals(currentUser.getId())) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "You are not authorized to set this address as default");
                return;
            }

            addressService.setDefaultAddress(currentUser.getId(), addressId);
            sendSuccess(response, HttpServletResponse.SC_OK, "Default address set successfully", null);
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred");
        }
    }

}