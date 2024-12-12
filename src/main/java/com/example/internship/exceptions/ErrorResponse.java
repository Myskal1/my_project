package com.example.internship.exceptions; // Или com.example.internship.models, в зависимости от пакета


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
}
