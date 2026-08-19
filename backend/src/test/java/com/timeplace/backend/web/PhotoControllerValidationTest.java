package com.timeplace.backend.web;

import com.timeplace.backend.dto.PhotoDto;
import com.timeplace.backend.repository.PhotoNearbyRepository;
import com.timeplace.backend.service.LocationCorrectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhotoController.class)
class PhotoControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhotoNearbyRepository photoNearbyRepository;

    @MockBean
    private LocationCorrectionService locationCorrectionService;

    @Test
    void rejectsOutOfRangeLatitude() throws Exception {
        mockMvc.perform(get("/api/photos/nearby").param("lat", "95").param("lon", "7.68"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsRadiusAboveMax() throws Exception {
        mockMvc.perform(get("/api/photos/nearby").param("lat", "45.07").param("lon", "7.68").param("radius", "50000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsPhotosForValidRequest() throws Exception {
        PhotoDto dto = new PhotoDto(1L, "wikimedia", "abc", "Title", "https://img", "https://thumb",
                1950, null, 45.07, 7.68, "CC BY-SA 4.0", "Author", "Attribution", false, 42.0);
        when(photoNearbyRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/photos/nearby").param("lat", "45.07").param("lon", "7.68"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].source").value("wikimedia"));
    }

    @Test
    void rejectsInvalidCorrectionBody() throws Exception {
        mockMvc.perform(post("/api/photos/1/correct-location")
                        .contentType("application/json")
                        .content("{\"lat\": 200, \"lon\": 7.68}"))
                .andExpect(status().isBadRequest());
    }
}
