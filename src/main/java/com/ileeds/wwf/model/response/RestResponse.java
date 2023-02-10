package com.ileeds.wwf.model.response;

import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record RestResponse<T>(T data) {
}
