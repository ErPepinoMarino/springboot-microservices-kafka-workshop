package com.workshop.orders.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateOrderRequest {

    @NotBlank(message = "El email del cliente es obligatorio")
    @Email(message = "El email del cliente debe tener formato válido")
    private String customerEmail;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
}