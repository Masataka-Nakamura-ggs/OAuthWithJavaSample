package com.example.demo.dto;

import lombok.Data;

/**
 * Google OAuth2認証のコールバック時に受け取るリクエストDTO。
 * <p>
 * 認可コード（code）とstate値を保持します。
 * </p>
 */
@Data
public class CallbackRequestDto {
    /**
     * Google認証から返却される認可コード
     */
    private String code;

    /**
     * CSRF対策用のstate値
     */
    private String state;
}
