package com.gamevault;

import com.gamevault.model.*;
import com.gamevault.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Demo data for local development — disable in production (app.seed.enabled=false). */
    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    private final UserRepository        userRepo;
    private final GameRepository        gameRepo;
    private final GameListRepository    listRepo;
    private final GameCatalogRepository catalogRepo;
    private final FriendshipRepository  friendshipRepo;
    private final ActivityRepository    activityRepo;
    private final FranchiseRepository   franchiseRepo;
    private final com.gamevault.repository.CalendarEventRepository calendarRepo;
    private final PasswordEncoder       passwordEncoder;

    public DataSeeder(UserRepository userRepo, GameRepository gameRepo, GameListRepository listRepo,
                      GameCatalogRepository catalogRepo, FriendshipRepository friendshipRepo,
                      ActivityRepository activityRepo, FranchiseRepository franchiseRepo,
                      com.gamevault.repository.CalendarEventRepository calendarRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.gameRepo = gameRepo;
        this.listRepo = listRepo;
        this.catalogRepo = catalogRepo;
        this.friendshipRepo = friendshipRepo;
        this.activityRepo = activityRepo;
        this.franchiseRepo = franchiseRepo;
        this.calendarRepo = calendarRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled || userRepo.count() > 0) return;

        log.info("Seeding GameVault database...");

        User david = saveUser("David Cruz", "dvd-cruz", "dvd@gamevault.dev", "🧑‍💻",
                "Gamer desde sempre. RPGs, Roguelites e tudo o que desafie.", "Lisboa, Portugal", 400);
        david.setAdmin(true);
        userRepo.save(david);

        User ana = saveUser("Ana Silva", "anasilva", "ana@gamevault.dev", "🧑‍🎤",
                "Platformers, JRPGs e completionist por natureza.", "Porto", 600);

        User tiago = saveUser("Tiago Costa", "tiagoc", "tiago@gamevault.dev", "🧔",
                "Action RPGs e shooters.", "Braga", 800);

        User mariana = saveUser("Mariana R.", "marianar", "mariana@gamevault.dev", "👩‍💻",
                "Narrativa em primeiro lugar — RPGs, point-and-click e indies estranhos.", "Coimbra", 500);

        User bruno = saveUser("Bruno M.", "brunom", "bruno@gamevault.dev", "🧑‍🚀",
                "Sci-fi, simuladores espaciais e tudo o que tenha mundo aberto.", "Faro", 350);

        User miguel = saveUser("Miguel Santos", "miguelsantos", "miguel@gamevault.dev", "🧑‍🎮",
                "Fã de estratégia, simuladores e jogos indie.", "Aveiro", 450);

        Friendship davidMiguel = new Friendship();
        davidMiguel.setRequester(david);
        davidMiguel.setRecipient(miguel);
        davidMiguel.setStatus(Friendship.Status.ACCEPTED);
        davidMiguel.setCreatedAt(System.currentTimeMillis());
        friendshipRepo.save(davidMiguel);

        User sofia = saveUser("Sofia Pereira", "sofiapereira", "sofia@gamevault.dev", "🧑‍🎨",
                "Plataformas, jogos coop e tudo o que tenha boa direção de arte.", "Setúbal", 380);

        Friendship davidSofia = new Friendship();
        davidSofia.setRequester(david);
        davidSofia.setRecipient(sofia);
        davidSofia.setStatus(Friendship.Status.ACCEPTED);
        davidSofia.setCreatedAt(System.currentTimeMillis());
        friendshipRepo.save(davidSofia);

        Friendship davidTiago = new Friendship();
        davidTiago.setRequester(david);
        davidTiago.setRecipient(tiago);
        davidTiago.setStatus(Friendship.Status.ACCEPTED);
        davidTiago.setCreatedAt(System.currentTimeMillis());
        friendshipRepo.save(davidTiago);

        Friendship davidBruno = new Friendship();
        davidBruno.setRequester(david);
        davidBruno.setRecipient(bruno);
        davidBruno.setStatus(Friendship.Status.ACCEPTED);
        davidBruno.setCreatedAt(System.currentTimeMillis());
        friendshipRepo.save(davidBruno);

        Friendship davidMariana = new Friendship();
        davidMariana.setRequester(david);
        davidMariana.setRecipient(mariana);
        davidMariana.setStatus(Friendship.Status.ACCEPTED);
        davidMariana.setCreatedAt(System.currentTimeMillis());
        friendshipRepo.save(davidMariana);

        Friendship davidAna = new Friendship();
        davidAna.setRequester(david);
        davidAna.setRecipient(ana);
        davidAna.setStatus(Friendship.Status.ACCEPTED);
        davidAna.setCreatedAt(System.currentTimeMillis());
        friendshipRepo.save(davidAna);

        // Catalog — general game registry (announced/released titles, admin-curated)
        GameCatalog hadesCatalog      = catalog("Hades", "🔥", null, "PC", "Roguelite");
        GameCatalog eldenRingCatalog  = catalog("Elden Ring", "⚔️", null, "PC", "RPG");
        GameCatalog hollowKnightCat   = catalog("Hollow Knight", "🐛", null, "PC", "Metroidvania");
        GameCatalog cyberpunkCatalog  = catalog("Cyberpunk 2077", "🤖", null, "PS5", "RPG");
        GameCatalog zeldaTotkCatalog  = catalog("Zelda: TotK", "🌍", null, "Switch", "Aventura");
        GameCatalog celesteCatalog    = catalog("Celeste", "🏔️", null, "Switch", "Platformer");
        GameCatalog persona5Catalog   = catalog("Persona 5 Royal", "🎭", null, "PC", "JRPG");
        // Catalog-only titles — registered but not yet in anyone's library (proves catalog ≠ library)
        // Released/trending titles (used by the Discover page "Em alta" / "Recomendado" sections)
        GameCatalog balatroCatalog = catalog("Balatro", "🃏", null, "PC", "Roguelite",
                date(2024, 2, 20), "LocalThunk", "Playstack",
                "Um jogo de cartas roguelite que redefine o género. Impossível de parar.");
        catalog("Black Myth: Wukong", "🐒", null, "PS5 / PC", "Action RPG",
                date(2024, 8, 20), "Game Science", "Game Science",
                "Uma produção chinesa que chegou para surpreender o mundo inteiro.");
        GameCatalog discoElysiumCatalog = catalog("Disco Elysium", "🕵️", null, "PC", "RPG",
                date(2019, 10, 15), "ZA/UM", "ZA/UM",
                "Mais livro interativo do que jogo — e isso é um elogio. Uma obra-prima da escrita.");
        catalog("Monster Hunter Wilds", "🐉", null, "PC / PS5", "Action RPG",
                date(2025, 2, 28), "Capcom", "Capcom",
                "A melhor entrada da série. Mundos vivos e combate refinado ao máximo.");
        GameCatalog hadesIICatalog = catalog("Hades II", "🔱", null, "PC", "Roguelite",
                date(2025, 3, 6), "Supergiant Games", "Supergiant Games",
                "Supera o original em quase tudo. Early access impecável.");
        catalog("Metaphor: ReFantazio", "🧙", null, "PC / PS5", "JRPG",
                date(2024, 10, 11), "Studio Zero", "Atlus",
                "Atlus a fazer o que sabe melhor — estilo inconfundível e narrativa poderosa.");
        GameCatalog astroBotCatalog = catalog("Astro Bot", "🤖", null, "PS5", "Platformer",
                date(2024, 9, 6), "Team Asobi", "Sony Interactive Entertainment",
                "GOTY de 2024 para muita gente. Uma celebração pura dos videojogos.");
        // Upcoming / announced titles (future release dates — populate "Próximos lançamentos")
        catalog("Hollow Knight: Silksong", "🦋", null, "PC / Switch", "Metroidvania",
                date(2026, 9, 12), "Team Cherry", "Team Cherry",
                "A sequela mais aguardada dos últimos anos.");
        catalog("Death Stranding 2", "👶", null, "PS5", "Action",
                date(2026, 6, 26), "Kojima Productions", "Sony Interactive Entertainment",
                "Kojima regressa com a sequela da sua obra mais divisiva — e mais ambiciosa.");
        catalog("Ghost of Yotei", "⛩️", null, "PS5", "Action Adventure",
                date(2026, 10, 2), "Sucker Punch Productions", "Sony Interactive Entertainment",
                "Seguimento espiritual de Ghost of Tsushima, desta vez no Japão feudal tardio.");
        catalog("Borderlands 4", "🔫", null, "PC / PS5 / Xbox", "Looter Shooter",
                date(2026, 9, 23), "Gearbox Software", "2K",
                "Mais armas, mais caos, mais humor. A fórmula continua — desta vez com gráficos novos.");
        catalog("Fable", "🧝", null, "Xbox / PC", "Action RPG",
                date(2026, 12, 15), "Playground Games", "Xbox Game Studios",
                "O reboot da lendária série da Lionhead finalmente tem data marcada.");

        // Sonic franchise — demonstrates a game belonging to multiple franchise titles
        GameCatalog sonicGenerationsCatalog = catalog("Sonic Generations", "💙", null, "PC / PS3 / Xbox 360", "Platformer",
                date(2011, 11, 1), "Sonic Team", "Sega",
                "Celebração dos 20 anos da série, combinando o Sonic clássico e o moderno.");
        GameCatalog sonicForcesCatalog = catalog("Sonic Forces", "💙", null, "PC / PS4 / Xbox One / Switch", "Platformer",
                date(2017, 11, 7), "Sonic Team", "Sega",
                "Sonic enfrenta o Dr. Eggman com a ajuda de um herói criado pelo jogador.");
        GameCatalog sonicFrontiersCatalog = catalog("Sonic Frontiers", "💙", null, "PC / PS5 / Xbox Series / Switch", "Platformer",
                date(2022, 11, 8), "Sonic Team", "Sega",
                "A primeira aventura open-zone do Sonic, explorando as Starfall Islands.");

        // Cover + hero art for David's library games (Steam CDN; Zelda from Wikipedia — not on Steam)
        steamArt(hadesCatalog,         1145360L);
        steamArt(eldenRingCatalog,     1245620L);
        steamArt(hollowKnightCat,      367520L);
        steamArt(cyberpunkCatalog,     1091500L);
        steamArt(sonicFrontiersCatalog, 1237320L);
        art(zeldaTotkCatalog,    "https://upload.wikimedia.org/wikipedia/en/f/fb/The_Legend_of_Zelda_Tears_of_the_Kingdom_cover.jpg", null);
        // a few catalog-only titles linked to Steam so the "Em promoção" section has more candidates
        steamArt(balatroCatalog,       2379780L);
        steamArt(discoElysiumCatalog,  632470L);
        steamArt(hadesIICatalog,       1145350L);

        // Franchises — group multiple catalog games together
        franchise("Sonic", sonicGenerationsCatalog, sonicForcesCatalog, sonicFrontiersCatalog);
        franchise("Hades", hadesCatalog, hadesIICatalog);

        // Games for David
        Game hades = saveGame(hadesCatalog, "playing",
                32.0, 4, 60, 0, "Viciante demais. Narrativa fantástica.",
                List.of("Viciante", "Narrativa"), david, 30);

        saveGame(eldenRingCatalog, "completed",
                87.0, 5, 100, 1, "Obra-prima absoluta. Boss fights incríveis.",
                List.of("Difícil", "Épico"), david, 100);

        saveGame(hollowKnightCat, "paused",
                18.0, 4, 45, 0, "Difícil mas lindo. Preciso de voltar a isto.",
                List.of(), david, 200);

        saveGame(cyberpunkCatalog, "backlog",
                0.0, 0, 0, 0, "", List.of(), david, 50);

        saveGame(zeldaTotkCatalog, "wishlist",
                0.0, 0, 0, 0, "", List.of(), david, 20);

        // Playthrough + sessions on Hades (sessions only exist within a playthrough)
        Playthrough hadesRun = new Playthrough();
        hadesRun.setType("normal");
        hadesRun.setGame(hades);
        GameSession s1 = new GameSession();
        s1.setDate("2026-05-28"); s1.setHours(2.5); s1.setNotes("Chegaste ao Elysium"); s1.setPlaythrough(hadesRun);
        GameSession s2 = new GameSession();
        s2.setDate("2026-05-30"); s2.setHours(1.5); s2.setNotes("Boss Meg pela 3ª vez"); s2.setPlaythrough(hadesRun);
        hadesRun.getSessions().addAll(List.of(s1, s2));
        hades.getPlaythroughs().add(hadesRun);
        gameRepo.save(hades);

        // Games for Ana
        saveGame(celesteCatalog, "completed",
                24.0, 4, 100, 1, "Difícil mas muito recompensador.", List.of(), ana, 150);

        saveGame(persona5Catalog, "completed",
                110.0, 5, 100, 1, "O JRPG mais estiloso de sempre.", List.of("Longo"), ana, 250);

        // Games for Sofia
        saveGame(astroBotCatalog, "playing",
                6.0, 0, 25, 0, "Adoro a direção de arte, tão fofo!",
                List.of("Fofo", "Divertido"), sofia, 2);

        Activity sofiaAstroBotActivity = new Activity();
        sofiaAstroBotActivity.setActor(sofia);
        sofiaAstroBotActivity.setType("added");
        sofiaAstroBotActivity.setCatalogGame(astroBotCatalog);
        sofiaAstroBotActivity.setMessage("playing");
        sofiaAstroBotActivity.setHours(6.0);
        sofiaAstroBotActivity.setCreatedAt(daysAgo(2));
        activityRepo.save(sofiaAstroBotActivity);

        // ── Extra seed activities (demonstrate pagination — >10 items in feed) ──

        // Miguel — added Hades to backlog, then started playing it, then completed it with review
        saveGame(hadesCatalog, "completed", 45.0, 5, 100, 1, "Obra-prima do género.", List.of("Viciante", "Épico"), miguel, 60);
        Activity miguelHadesAdded = new Activity();
        miguelHadesAdded.setActor(miguel); miguelHadesAdded.setType("added");
        miguelHadesAdded.setCatalogGame(hadesCatalog); miguelHadesAdded.setMessage("backlog");
        miguelHadesAdded.setCreatedAt(daysAgo(18));
        activityRepo.save(miguelHadesAdded);

        Activity miguelHadesPlaying = new Activity();
        miguelHadesPlaying.setActor(miguel); miguelHadesPlaying.setType("playing");
        miguelHadesPlaying.setCatalogGame(hadesCatalog); miguelHadesPlaying.setHours(12.0);
        miguelHadesPlaying.setCreatedAt(daysAgo(14));
        activityRepo.save(miguelHadesPlaying);

        Activity miguelHadesCompleted = new Activity();
        miguelHadesCompleted.setActor(miguel); miguelHadesCompleted.setType("completed");
        miguelHadesCompleted.setCatalogGame(hadesCatalog); miguelHadesCompleted.setHours(45.0); miguelHadesCompleted.setRating(5);
        miguelHadesCompleted.setCreatedAt(daysAgo(10));
        activityRepo.save(miguelHadesCompleted);

        Activity miguelHadesReview = new Activity();
        miguelHadesReview.setActor(miguel); miguelHadesReview.setType("review");
        miguelHadesReview.setCatalogGame(hadesCatalog); miguelHadesReview.setRating(5);
        miguelHadesReview.setMessage("Nunca pensei que fosse tão difícil parar de jogar. Cada run ensina-te alguma coisa nova, a narrativa vai evoluindo com cada derrota, e os personagens são memoráveis. Um dos melhores jogos que já joguei.");
        miguelHadesReview.setCreatedAt(daysAgo(9));
        activityRepo.save(miguelHadesReview);

        // Miguel — added Elden Ring to wishlist
        Activity miguelEldenWishlist = new Activity();
        miguelEldenWishlist.setActor(miguel); miguelEldenWishlist.setType("added");
        miguelEldenWishlist.setCatalogGame(eldenRingCatalog); miguelEldenWishlist.setMessage("wishlist");
        miguelEldenWishlist.setCreatedAt(daysAgo(7));
        activityRepo.save(miguelEldenWishlist);

        // Tiago — playing and reviewing Elden Ring
        saveGame(eldenRingCatalog, "playing", 55.0, 0, 60, 0, "Difícil mas épico.", List.of("Difícil"), tiago, 45);
        Activity tiagoEldenPlaying = new Activity();
        tiagoEldenPlaying.setActor(tiago); tiagoEldenPlaying.setType("playing");
        tiagoEldenPlaying.setCatalogGame(eldenRingCatalog); tiagoEldenPlaying.setHours(55.0);
        tiagoEldenPlaying.setCreatedAt(daysAgo(8));
        activityRepo.save(tiagoEldenPlaying);

        Activity tiagoPost = new Activity();
        tiagoPost.setActor(tiago); tiagoPost.setType("post");
        tiagoPost.setCatalogGame(eldenRingCatalog);
        tiagoPost.setMessage("Malenia derrotada depois de 3 horas seguidas. Os meus dedos doem mas valeu completamente a pena. 🏆");
        tiagoPost.setCreatedAt(daysAgo(5));
        activityRepo.save(tiagoPost);

        // Tiago — added Hollow Knight
        saveGame(hollowKnightCat, "backlog", 0.0, 0, 0, 0, "", List.of(), tiago, 20);
        Activity tiagoHKAdded = new Activity();
        tiagoHKAdded.setActor(tiago); tiagoHKAdded.setType("added");
        tiagoHKAdded.setCatalogGame(hollowKnightCat); tiagoHKAdded.setMessage("backlog");
        tiagoHKAdded.setCreatedAt(daysAgo(6));
        activityRepo.save(tiagoHKAdded);

        // Bruno — playing and reviewing Cyberpunk
        saveGame(cyberpunkCatalog, "playing", 30.0, 0, 40, 0, "", List.of(), bruno, 35);
        Activity brunoCyberpunkPlaying = new Activity();
        brunoCyberpunkPlaying.setActor(bruno); brunoCyberpunkPlaying.setType("playing");
        brunoCyberpunkPlaying.setCatalogGame(cyberpunkCatalog); brunoCyberpunkPlaying.setHours(30.0);
        brunoCyberpunkPlaying.setCreatedAt(daysAgo(12));
        activityRepo.save(brunoCyberpunkPlaying);

        Activity brunoCyberpunkPost = new Activity();
        brunoCyberpunkPost.setActor(bruno); brunoCyberpunkPost.setType("post");
        brunoCyberpunkPost.setCatalogGame(cyberpunkCatalog);
        brunoCyberpunkPost.setMessage("Night City de noite com ray tracing ligado é de cair o queixo. Depois de todos os patches este jogo é outra coisa.");
        brunoCyberpunkPost.setCreatedAt(daysAgo(11));
        activityRepo.save(brunoCyberpunkPost);

        // Bruno — completed Zelda TotK
        saveGame(zeldaTotkCatalog, "completed", 120.0, 5, 100, 1, "Épico.", List.of("Épico", "Longo"), bruno, 90);
        Activity brunoZeldaCompleted = new Activity();
        brunoZeldaCompleted.setActor(bruno); brunoZeldaCompleted.setType("completed");
        brunoZeldaCompleted.setCatalogGame(zeldaTotkCatalog); brunoZeldaCompleted.setHours(120.0); brunoZeldaCompleted.setRating(5);
        brunoZeldaCompleted.setCreatedAt(daysAgo(4));
        activityRepo.save(brunoZeldaCompleted);

        // Mariana — completed Celeste with review
        saveGame(celesteCatalog, "completed", 28.0, 5, 100, 1, "Perfeito.", List.of("Emotivo"), mariana, 55);
        Activity marianaelesteCompleted = new Activity();
        marianaelesteCompleted.setActor(mariana); marianaelesteCompleted.setType("completed");
        marianaelesteCompleted.setCatalogGame(celesteCatalog); marianaelesteCompleted.setHours(28.0); marianaelesteCompleted.setRating(5);
        marianaelesteCompleted.setCreatedAt(daysAgo(15));
        activityRepo.save(marianaelesteCompleted);

        Activity marianaCelesteReview = new Activity();
        marianaCelesteReview.setActor(mariana); marianaCelesteReview.setType("review");
        marianaCelesteReview.setCatalogGame(celesteCatalog); marianaCelesteReview.setRating(5);
        marianaCelesteReview.setMessage("Celeste não é só um jogo de plataformas difícil — é uma história sobre saúde mental contada através do movimento e da dificuldade. A Madeline é uma das personagens mais bem escritas dos videojogos. Chorei no final.");
        marianaCelesteReview.setCreatedAt(daysAgo(13));
        activityRepo.save(marianaCelesteReview);

        // Mariana — added Persona 5 to wishlist
        Activity marianaP5Wishlist = new Activity();
        marianaP5Wishlist.setActor(mariana); marianaP5Wishlist.setType("added");
        marianaP5Wishlist.setCatalogGame(persona5Catalog); marianaP5Wishlist.setMessage("wishlist");
        marianaP5Wishlist.setCreatedAt(daysAgo(3));
        activityRepo.save(marianaP5Wishlist);

        // Ana — completed Persona 5 with a review
        Activity anaP5Review = new Activity();
        anaP5Review.setActor(ana); anaP5Review.setType("review");
        anaP5Review.setCatalogGame(persona5Catalog); anaP5Review.setRating(5);
        anaP5Review.setMessage("110 horas e ainda queria mais. A direção artística, a música, os personagens — tudo funciona em perfeita harmonia. O Joker é um ícone. Se só jogares um JRPG na vida, que seja este.");
        anaP5Review.setCreatedAt(daysAgo(16));
        activityRepo.save(anaP5Review);

        Activity anaP5Completed = new Activity();
        anaP5Completed.setActor(ana); anaP5Completed.setType("completed");
        anaP5Completed.setCatalogGame(persona5Catalog); anaP5Completed.setHours(110.0); anaP5Completed.setRating(5);
        anaP5Completed.setCreatedAt(daysAgo(20));
        activityRepo.save(anaP5Completed);

        // Sofia — review of Astro Bot
        Activity sofiaAstroBotReview = new Activity();
        sofiaAstroBotReview.setActor(sofia); sofiaAstroBotReview.setType("review");
        sofiaAstroBotReview.setCatalogGame(astroBotCatalog); sofiaAstroBotReview.setRating(5);
        sofiaAstroBotReview.setMessage("Astro Bot é pura alegria em forma de jogo. Cada nível é uma surpresa nova, as referências ao universo PlayStation são deliciosas, e o DualSense é usado de forma genial. GOTY sem discussão.");
        sofiaAstroBotReview.setCreatedAt(daysAgo(1));
        activityRepo.save(sofiaAstroBotReview);

        // Ana — playing Hollow Knight (extra "now playing" entry to test carousel pagination)
        saveGame(hollowKnightCat, "playing", 22.0, 0, 50, 0, "", List.of(), ana, 6);
        Activity anaHollowKnightPlaying = new Activity();
        anaHollowKnightPlaying.setActor(ana); anaHollowKnightPlaying.setType("playing");
        anaHollowKnightPlaying.setCatalogGame(hollowKnightCat); anaHollowKnightPlaying.setHours(22.0);
        anaHollowKnightPlaying.setCreatedAt(daysAgo(1));
        activityRepo.save(anaHollowKnightPlaying);

        // Mariana — playing Persona 5 Royal (extra "now playing" entry to test carousel pagination)
        saveGame(persona5Catalog, "playing", 14.0, 0, 30, 0, "", List.of(), mariana, 4);
        Activity marianaPersona5Playing = new Activity();
        marianaPersona5Playing.setActor(mariana); marianaPersona5Playing.setType("playing");
        marianaPersona5Playing.setCatalogGame(persona5Catalog); marianaPersona5Playing.setHours(14.0);
        marianaPersona5Playing.setCreatedAt(daysAgo(1));
        activityRepo.save(marianaPersona5Playing);

        // Miguel — playing Sonic Frontiers (extra "now playing" entry to test carousel pagination)
        saveGame(sonicFrontiersCatalog, "playing", 9.0, 0, 20, 0, "", List.of(), miguel, 2);
        Activity miguelSonicPlaying = new Activity();
        miguelSonicPlaying.setActor(miguel); miguelSonicPlaying.setType("playing");
        miguelSonicPlaying.setCatalogGame(sonicFrontiersCatalog); miguelSonicPlaying.setHours(9.0);
        miguelSonicPlaying.setCreatedAt(daysAgo(1));
        activityRepo.save(miguelSonicPlaying);

        // Shared Lists
        GameList topRpgs = new GameList();
        topRpgs.setSlug("top-rpgs-2024");
        topRpgs.setTitle("Top RPGs de 2024");
        topRpgs.setDescription("Os melhores RPGs que joguei este ano.");
        topRpgs.setAuthor(ana);
        topRpgs.setCreatedAt(daysAgo(5));
        topRpgs.getGames().addAll(List.of(
                listItem("Metaphor: ReFantazio", "🧙", "JRPG", "Obra-prima. Atlus no seu melhor.", 1, topRpgs),
                listItem("Elden Ring",           "⚔️", "Action RPG", "Já clássico.",               2, topRpgs),
                listItem("Disco Elysium",        "🕵️", "RPG", "Mais livro do que jogo. 10/10.", 3, topRpgs)
        ));
        listRepo.save(topRpgs);

        GameList roguelites = new GameList();
        roguelites.setSlug("roguelites-essenciais");
        roguelites.setTitle("Roguelites Essenciais");
        roguelites.setDescription("Se nunca jogaste um roguelite, começa por aqui.");
        roguelites.setAuthor(tiago);
        roguelites.setCreatedAt(daysAgo(12));
        roguelites.getGames().addAll(List.of(
                listItem("Hades",    "🔥", "Roguelite", "O melhor ponto de entrada.", 1, roguelites),
                listItem("Balatro",  "🃏", "Roguelite", "Viciante demais.",           2, roguelites)
        ));
        listRepo.save(roguelites);

        // ── Calendar events (recurring gaming anniversaries) ──
        calendarEvent("🍄", "MAR10 Day — dia do Mario", 3, 10);
        calendarEvent("💙", "Aniversário do Sonic the Hedgehog (1991)", 6, 23);
        calendarEvent("🗡️", "Aniversário de The Legend of Zelda (1986)", 2, 21);
        calendarEvent("⚡", "Aniversário de Pokémon (1996)", 2, 27);
        calendarEvent("🎮", "Aniversário da Famicom (1983)", 7, 15);
        calendarEvent("🌀", "Aniversário da Dreamcast (1999)", 9, 9);
        calendarEvent("🕹️", "Aniversário da NES na América do Norte (1985)", 10, 18);
        calendarEvent("🟢", "Aniversário da Xbox (2001)", 11, 15);
        calendarEvent("⛏️", "Lançamento de Minecraft (2011)", 11, 18);
        calendarEvent("🎮", "Aniversário da PlayStation (1994)", 12, 3);

        log.info("Seed complete — users: {}, games: {}, lists: {}",
                userRepo.count(), gameRepo.count(), listRepo.count());
    }

    private void calendarEvent(String emoji, String label, int month, int day) {
        com.gamevault.model.CalendarEvent e = new com.gamevault.model.CalendarEvent();
        e.setEmoji(emoji); e.setLabel(label); e.setMonth(month); e.setDay(day);
        e.setYear(null); // recurring
        e.setCreatedAt(System.currentTimeMillis());
        calendarRepo.save(e);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User saveUser(String name, String username, String email, String avatar, String bio, String location, int daysAgoJoined) {
        User u = new User();
        u.setName(name); u.setUsername(username); u.setEmail(email); u.setAvatar(avatar);
        u.setBio(bio); u.setLocation(location); u.setJoinedAt(daysAgo(daysAgoJoined));
        u.setPassword(passwordEncoder.encode("password123"));
        return userRepo.save(u);
    }

    /** Sets cover + hero art (Steam CDN / Wikipedia URLs) on an already-created catalog entry. */
    private GameCatalog art(GameCatalog c, String coverUrl, String heroImageUrl) {
        c.setCoverUrl(coverUrl);
        c.setHeroImageUrl(heroImageUrl);
        return catalogRepo.save(c);
    }

    /** Links a catalog game to its Steam app: sets the app id plus cover/hero from the Steam CDN. */
    private GameCatalog steamArt(GameCatalog c, long appId) {
        c.setSteamAppId(appId);
        c.setCoverUrl("https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/library_600x900_2x.jpg");
        c.setHeroImageUrl("https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/library_hero.jpg");
        return catalogRepo.save(c);
    }

    private GameCatalog catalog(String title, String emoji, String coverUrl, String platform, String genre) {
        GameCatalog c = new GameCatalog();
        c.setTitle(title); c.setEmoji(emoji); c.setCoverUrl(coverUrl);
        c.setPlatform(com.gamevault.util.PlatformUtil.normalize(platform)); c.setGenre(genre);
        return catalogRepo.save(c);
    }

    private GameCatalog catalog(String title, String emoji, String coverUrl, String platform, String genre,
                                Long releaseDate, String developer, String publisher, String description) {
        GameCatalog c = new GameCatalog();
        c.setTitle(title); c.setEmoji(emoji); c.setCoverUrl(coverUrl);
        c.setPlatform(com.gamevault.util.PlatformUtil.normalize(platform)); c.setGenre(genre);
        c.setReleaseDate(releaseDate); c.setDeveloper(developer); c.setPublisher(publisher);
        c.setDescription(description);
        return catalogRepo.save(c);
    }

    private Franchise franchise(String name, GameCatalog... games) {
        Franchise f = new Franchise();
        f.setName(name);
        f = franchiseRepo.save(f);
        for (GameCatalog g : games) {
            g.getFranchises().add(f);
            catalogRepo.save(g);
        }
        return f;
    }

    private long date(int year, int month, int day) {
        return java.time.LocalDate.of(year, month, day)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    private Game saveGame(GameCatalog catalogGame, String status,
                          double hours, int rating, int progress, int playthroughCount, String notes,
                          List<String> tags, User owner, int addedDaysAgo) {
        Game g = new Game();
        g.setCatalogGame(catalogGame); g.setStatus(status);
        g.setHours(hours);
        g.setRating(rating); g.setProgress(progress);
        g.setNotes(notes); g.getTags().addAll(tags);
        g.setOwner(owner); g.setAddedAt(daysAgo(addedDaysAgo));
        g.setStatusUpdatedAt(daysAgo(addedDaysAgo));
        for (int i = 0; i < playthroughCount; i++) {
            Playthrough p = new Playthrough();
            p.setType("normal");
            p.setSpeedrun(false);
            p.setGame(g);
            g.getPlaythroughs().add(p);
        }
        return gameRepo.save(g);
    }

    private GameListItem listItem(String title, String emoji, String genre, String note, int pos, GameList list) {
        GameListItem i = new GameListItem();
        i.setTitle(title); i.setEmoji(emoji); i.setGenre(genre);
        i.setNote(note); i.setPosition(pos); i.setGameList(list);
        return i;
    }

    private long daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli();
    }
}
