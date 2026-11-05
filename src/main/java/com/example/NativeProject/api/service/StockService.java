package com.example.NativeProject.api.service;

import com.example.NativeProject.api.model.StockEntity;
import com.example.NativeProject.api.repository.StockRepository;
import com.example.NativeProject.api.request.StockChangeRequest;
import com.example.NativeProject.api.request.StockRequest;
import com.example.NativeProject.api.response.StockResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public StockResponse getStockByBookId(Long bookId) {
        StockEntity stock = stockRepository.findByBookEntityId(bookId)
                .orElseThrow(() -> new NoSuchElementException("Bu kitabın stok kaydı yok: " + bookId));
        return toResponse(stock);
    }

    @Transactional
    public StockResponse updateStock(Long bookId, StockRequest request) {
        StockEntity stock = stockRepository.findByBookEntityId(bookId)
                .orElseThrow(() -> new NoSuchElementException("Bu kitabın stok kaydı yok: " + bookId));
        stock.setQuantity(request.getQuantity());
        return toResponse(stockRepository.save(stock));
    }

    @Transactional
    public StockResponse increaseStock(Long bookId, @Valid StockChangeRequest request) {
        int updated = stockRepository.increase(bookId, request.getAmount());
        if (updated == 0) {
            throw new NoSuchElementException("Bu kitabın stok kaydı yok: " + bookId);
        }
        return getStockByBookId(bookId);
    }
    @Transactional
    public StockResponse decreaseStock(Long bookId, @Valid StockChangeRequest request){
        int updated = stockRepository.decrease(bookId, request.getAmount());
        if (updated == 0){
            throw new NoSuchElementException("Bu kitabın stogu daha fazla azaltılamaz: "+bookId);
        }
        return getStockByBookId(bookId);
    }

    private StockResponse toResponse(StockEntity stock) {
        StockResponse response = new StockResponse();
        response.setBookId(stock.getBookEntity().getId());
        response.setQuantity(stock.getQuantity());
        return response;
    }
}
