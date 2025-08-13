package com.example.demo.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CallbackRequestDtoTest {

    @Test
    void testCallbackRequestDtoCreation() {
        // Given
        CallbackRequestDto dto = new CallbackRequestDto();
        dto.setCode("authorization-code");
        dto.setState("state-value");

        // Then
        assertEquals("authorization-code", dto.getCode());
        assertEquals("state-value", dto.getState());
    }

    @Test
    void testCallbackRequestDtoEqualsAndHashCode() {
        // Given
        CallbackRequestDto dto1 = new CallbackRequestDto();
        dto1.setCode("authorization-code");
        dto1.setState("state-value");
        
        CallbackRequestDto dto2 = new CallbackRequestDto();
        dto2.setCode("authorization-code");
        dto2.setState("state-value");

        // Then
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
