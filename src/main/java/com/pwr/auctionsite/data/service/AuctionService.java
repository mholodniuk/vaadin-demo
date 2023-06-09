package com.pwr.auctionsite.data.service;

import com.pwr.auctionsite.data.dao.*;
import com.pwr.auctionsite.data.dto.AuctionDTO;
import com.pwr.auctionsite.data.dto.views.ActiveAuctionDTO;
import com.pwr.auctionsite.data.dto.views.FinishedAuctionDTO;
import com.pwr.auctionsite.data.entity.Auction;
import com.pwr.auctionsite.data.entity.Item;
import com.pwr.auctionsite.data.entity.ItemCategory;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class AuctionService {
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final AuctionDAO auctionDAO;
    private final JdbcTemplate template;
    private final ItemCategoryRepository itemCategoryRepository;

    public List<String> findAllCategories() {
        return itemCategoryRepository.findAll().stream().map(ItemCategory::getName).toList();
    }

    public List<ActiveAuctionDTO> findAuctions(String searchString, String category, int offset, int limit) {
        log.info("getting new auctions");
        return auctionDAO.findAllPaged(searchString, category, offset, limit);
    }

    public List<FinishedAuctionDTO> findArchivedAuctions(Long userId, int offset, int limit) {
        log.info("getting new auctions");
        return auctionDAO.findArchivedAuctions(userId, offset, limit);
    }

    public List<ActiveAuctionDTO> findMyAuctions(int offset, int limit, long userId, String relationType) {
        log.info("getting my auctions");
        return auctionDAO.findMyAuctions(offset, limit, userId, relationType);
    }

    public AuctionDTO findById(Long auctionId) {
        var auction = auctionRepository.findById(auctionId).orElseThrow();

        var dto = new AuctionDTO();
        dto.setItemId(auction.getItem().getId());
        dto.setItemQuantity(auction.getItemQuantity());
        dto.setExpirationDate(auction.getExpirationDate());
        dto.setBuyNowPrice(auction.getBuyNowPrice());
        dto.setStartingPrice(auction.getStartingPrice());
        dto.setName(auction.getItem().getName());
        dto.setName(auction.getItem().getName());
        dto.setDescription(auction.getItem().getDescription());
        dto.setCategory(auction.getItem().getCategory().getName());
        dto.setImageUrl(auction.getItem().getImageUrl());

        return dto;
    }

    @Transactional
    public void saveAuction(AuctionDTO auctionDTO, Long userId) {
        var itemCategory = itemCategoryRepository.findByName(auctionDTO.getCategory()).orElseThrow();
        var seller = userRepository.findById(userId).orElseThrow();

        var item = Item.builder()
                .name(auctionDTO.getName())
                .description(auctionDTO.getDescription())
                .imageUrl(auctionDTO.getImageUrl())
                .modifiedAt(LocalDateTime.now())
                .category(itemCategory)
                .build();

        itemRepository.save(item);

        var auction = Auction.builder()
                .itemQuantity(auctionDTO.getItemQuantity())
                .startingPrice(auctionDTO.getStartingPrice())
                .buyNowPrice(auctionDTO.getBuyNowPrice())
                .expirationDate(auctionDTO.getExpirationDate())
                .seller(seller)
                .item(item)
                .build();

        auctionRepository.save(auction);
    }

    public void placeBidProcedure(Long auctionId, Long userId, BigDecimal bidValue) {
        log.info("placing bid %s on auction %s by user %s"
                .formatted(bidValue.doubleValue(), auctionId, bidValue.doubleValue()));
        var sql = """
                CALL place_bid(?, ?, ?)
                """;
        template.update(sql, auctionId, userId, bidValue);
    }

    public void moveAuctionToFinished(Long auctionId, BigDecimal bidValue) {
        log.info("auction: %s moved to finished with price %s"
                .formatted(auctionId, bidValue.doubleValue()));
        var sql = """
                CALL move_auction_to_finished(?, ?)
                """;
        template.update(sql, auctionId, bidValue);
    }

    public void buyNow(Long auctionId, Long userId) {
        log.info("auction: %s bought by user %s".formatted(auctionId, userId));
        var sql = """
                CALL buy_now(?, ?)
                """;
        template.update(sql, auctionId, userId);
    }

    public void addAuctionToWatchlist(Long userId, Long auctionId, String relation) {
        log.info("adding: %s auction to following of %s".formatted(auctionId, userId));
        var sql = """
                CALL add_auction_to_watchlist(?, ?, ?)
                """;
        template.update(sql, userId, auctionId, relation);
    }
}