package com.example.NativeProject;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthorBookApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authorAndBookCrudFlow() throws Exception {
        // Yazar oluştur
        String authorJson = mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sabahattin Ali\",\"birthYear\":1907}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer authorId = JsonPath.read(authorJson, "$.id");

        // Yazara kitap ekle
        String bookJson = mockMvc.perform(post("/authors/{id}/books", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Kürk Mantolu Madonna\",\"pageCount\":160,\"price\":120.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Sabahattin Ali"))
                .andReturn().getResponse().getContentAsString();
        Integer bookId = JsonPath.read(bookJson, "$.id");

        // Sayfalı liste: içerik + page bilgisi
        mockMvc.perform(get("/authors/{id}/books", authorId).param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Kürk Mantolu Madonna"))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // Kitabı olan yazar silinemez
        mockMvc.perform(delete("/authors/{id}", authorId))
                .andExpect(status().isConflict());

        // Kitap güncelle, sil
        mockMvc.perform(put("/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"İçimizdeki Şeytan\",\"pageCount\":250}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("İçimizdeki Şeytan"));
        mockMvc.perform(delete("/books/{id}", bookId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/books/{id}", bookId))
                .andExpect(status().isNotFound());

        // Artık yazar silinebilir
        mockMvc.perform(delete("/authors/{id}", authorId))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidRequestsReturnClientErrors() throws Exception {
        mockMvc.perform(post("/authors").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
        mockMvc.perform(post("/authors").contentType(MediaType.APPLICATION_JSON).content("{bozuk json"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/books/abc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/olmayan-adres"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/books/1"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/authors/999999"))
                .andExpect(status().isNotFound());
    }
}
