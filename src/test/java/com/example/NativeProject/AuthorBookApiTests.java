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
    void bookResponsesShowCurrentStock() throws Exception {
        String authorJson = mockMvc.perform(post("/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Stok Yazarı\",\"birthYear\":1980}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer authorId = JsonPath.read(authorJson, "$.id");

        // Yeni kitap: stok kaydı otomatik açılır, başlangıç 0
        String bookJson = mockMvc.perform(post("/authors/{id}/books", authorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Stoklu Kitap\",\"pageCount\":100,\"price\":50.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(0))
                .andReturn().getResponse().getContentAsString();
        Integer bookId = JsonPath.read(bookJson, "$.id");

        // Cache'i ısıt: tek kitap ve liste
        mockMvc.perform(get("/books/{id}", bookId))
                .andExpect(jsonPath("$.quantity").value(0));
        mockMvc.perform(get("/authors/{id}/books", authorId))
                .andExpect(jsonPath("$.content[0].quantity").value(0));

        // Stok artınca cache'lenmiş cevaplarda da yeni değer görünmeli
        mockMvc.perform(post("/books/{id}/stock/increase", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/books/{id}", bookId))
                .andExpect(jsonPath("$.quantity").value(10));
        mockMvc.perform(get("/books").param("size", "100"))
                .andExpect(jsonPath("$.content[?(@.id==" + bookId + ")].quantity").value(10));

        // Stok azalınca da güncel değer dönmeli
        mockMvc.perform(post("/books/{id}/stock/decrease", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":3}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/books/{id}", bookId))
                .andExpect(jsonPath("$.quantity").value(7));
        mockMvc.perform(get("/authors/{id}/books", authorId))
                .andExpect(jsonPath("$.content[0].quantity").value(7));

        // Stoktan fazlası azaltılamaz: 409, stok değişmez
        mockMvc.perform(post("/books/{id}/stock/decrease", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":999}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/books/{id}", bookId))
                .andExpect(jsonPath("$.quantity").value(7));

        // Olmayan kitabın stoğu azaltılırsa 404 (409 değil)
        mockMvc.perform(post("/books/{id}/stock/decrease", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1}"))
                .andExpect(status().isNotFound());

        // Temizlik
        mockMvc.perform(delete("/books/{id}", bookId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/authors/{id}", authorId)).andExpect(status().isNoContent());
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
