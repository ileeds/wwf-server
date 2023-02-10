package com.ileeds.wwf.model.response;

import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record SocketResponse<T>(T data) {
}
