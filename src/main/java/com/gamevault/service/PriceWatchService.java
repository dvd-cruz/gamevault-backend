package com.gamevault.service;

import com.gamevault.model.GameCatalog;
import com.gamevault.repository.GameCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Periodically checks the current store price of every store-linked catalog game and records an
 * "on sale" snapshot on the GameCatalog. NotificationService reads it for wishlist price-drop
 * alerts and the Discover page reads it for the "Em promoção" section — no external API calls
 * happen during those reads.
 */
@Service
public class PriceWatchService {

    private static final Logger log = LoggerFactory.getLogger(PriceWatchService.class);

    private final GameCatalogRepository catalogRepo;
    private final SteamService steamService;
    private final PsnPriceService psnPriceService;

    public PriceWatchService(GameCatalogRepository catalogRepo,
                             SteamService steamService, PsnPriceService psnPriceService) {
        this.catalogRepo = catalogRepo;
        this.steamService = steamService;
        this.psnPriceService = psnPriceService;
    }

    /** Runs shortly after startup and every 6 hours. */
    @Scheduled(initialDelay = 30_000, fixedDelay = 6 * 60 * 60 * 1000)
    public void checkPrices() {
        // every store-linked catalog game (covers wishlist alerts AND the Discover "on sale" section)
        List<GameCatalog> games = catalogRepo.findWithStoreId();
        int onSale = 0;
        for (GameCatalog c : games) {
            try {
                if (refreshSnapshot(c)) onSale++;
            } catch (Exception e) {
                log.debug("Price check failed for {}: {}", c.getTitle(), e.getMessage());
            }
        }
        log.info("Price watch: checked {} store-linked game(s), {} on sale", games.size(), onSale);
    }

    /** Fetches the current price (Steam first, then PSN) and stores the sale snapshot. Returns true if on sale. */
    private boolean refreshSnapshot(GameCatalog c) {
        Integer discount = null; Double finalPrice = null; String currency = null, store = null, url = null;

        if (c.getSteamAppId() != null) {
            SteamService.SteamPriceInfo info = steamService.fetchPriceOverview(c.getSteamAppId());
            if (info != null && !info.free() && info.discountPercent() > 0) {
                discount = info.discountPercent();
                finalPrice = info.finalPrice();
                currency = info.currency();
                store = "Steam";
                url = "https://store.steampowered.com/app/" + c.getSteamAppId();
            }
        }
        if (discount == null && c.getPsnProductId() != null) {
            PsnPriceService.PsnPriceInfo info = psnPriceService.fetchPriceByProductId(c.getPsnProductId());
            if (info != null && !info.free() && info.discountPercent() > 0) {
                discount = info.discountPercent();
                finalPrice = info.finalPrice();
                currency = info.currency();
                store = "PlayStation";
                url = "https://store.playstation.com/product/" + c.getPsnProductId();
            }
        }

        c.setSaleDiscountPercent(discount);
        c.setSaleFinalPrice(finalPrice);
        c.setSaleCurrency(currency);
        c.setSaleStore(store);
        c.setSaleStoreUrl(url);
        c.setSalePriceCheckedAt(System.currentTimeMillis());
        catalogRepo.save(c);
        return discount != null;
    }
}
