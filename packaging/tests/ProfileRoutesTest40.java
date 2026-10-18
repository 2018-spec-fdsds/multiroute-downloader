package multiroute;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Owned persistent queue/profile regression; no source, router or user data is accessed. */
public final class ProfileRoutesTest40 {
    private static int checks;
    private static final List<String> cases = new ArrayList<>();
    private static final AtomicInteger sourceCalls = new AtomicInteger();
    private static final String URL = "https://profile-routes.invalid/каталог/Файл_日本語_é_🚀.bin";
    private static final List<String> GATEWAYS = List.of("192.0.2.1", "192.0.2.2", "192.0.2.3");
    private static void check(boolean valid, String name) {
        checks++; if (!valid) throw new AssertionError(name); cases.add(name);
    }
    @FunctionalInterface private interface Task { void run() throws Exception; }
    private static void rejected(Task action, String name) throws Exception {
        try { action.run(); throw new AssertionError(name + " was accepted"); }
        catch (IOException | IllegalArgumentException expected) { check(true, name); }
    }
    private static Map<String,Object> route(String gateway, String profile) {
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("type", "gateway"); value.put("gateway", gateway); value.put("enabled", true);
        if (profile != null) value.put("site_profile_id", profile);
        return value;
    }
    private static Map<String,Object> request(String name, List<?> routes) {
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("url", URL); value.put("filename", name); value.put("routes", routes);
        value.put("start_after_ms", System.currentTimeMillis() + 86400000L);
        return value;
    }
    private static Main.Manager manager(Path own) throws IOException {
        Main.Manager value = new Main.Manager(own, (url, route) -> {
            sourceCalls.incrementAndGet(); throw new AssertionError("No source access is allowed in this fixture");
        }, path -> Long.MAX_VALUE, (route, torrent) -> { });
        value.queueTimer.shutdownNow(); return value;
    }
    private static void binding(Map<String,Object> state, String id, String username) throws Exception {
        check(id.equals(state.get("site_profile_id")), "Job retains canonical profile identity");
        for (Object raw : Main.arr(state.get("routes"))) {
            Map<String,Object> route = Main.obj(raw);
            check(id.equals(route.get("site_profile_id")), "Every retained gateway keeps the same profile identity");
            check(username.equals(SourceAuth.forUri(URI.create(URL), route).get("username")),
                    "Gateway resolves selected profile credentials without global-origin fallback");
        }
        check(!Json.stringify(state).contains("OWN-PASSWORD") && !Json.stringify(state).contains("OWN-GLOBAL"),
                "Queue API snapshot contains no credential values");
    }
    private static Set<String> enabled(Map<String,Object> state) {
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        for (Object raw : Main.arr(state.get("routes"))) if (Main.yes(Main.obj(raw).get("enabled")))
            values.add(Main.str(Main.obj(raw).get("gateway")));
        return values;
    }
    public static void main(String[] args) throws Exception {
        Main.configureUtf8Console();
        Path requested = Path.of(args[0]).toAbsolutePath().normalize(); Files.createDirectories(requested);
        Path own = requested.toRealPath();
        SourceAuth.configure(own); AppLog.initialize(own);
        GatewayNetwork.configure("192.0.2.70", GATEWAYS);
        final Main.Manager manager = manager(own);
        String firstId;
        String profile;
        try {
            SourceAuth.save(Map.of("origin", "https://profile-routes.invalid", "type", "basic",
                    "username", "GLOBAL-ACCOUNT", "password", "OWN-GLOBAL"));
            Map<String,Object> saved = SiteProfiles.save(Map.of("name", "Сайт_日本語_é_🚀", "base_url",
                    "https://profile-routes.invalid/каталог/", "type", "basic", "username", "SELECTED-ACCOUNT",
                    "password", "OWN-PASSWORD", "download_directory", own.resolve("Загрузки_ä_🚀").toString()));
            profile = Main.str(saved.get("id"));
            String second = Main.str(SiteProfiles.save(Map.of("name", "Second account", "base_url",
                    "https://profile-routes.invalid/", "type", "basic", "username", "SECOND-ACCOUNT",
                    "password", "OWN-PASSWORD-SECOND")).get("id"));
            Map<String,Object> input = request("Сохранённый_日本語_é_🚀.bin", List.of(route(GATEWAYS.get(0), profile)));
            Map<String,Object> created = manager.create(input); firstId = Main.str(created.get("id"));
            binding(created, profile, "SELECTED-ACCOUNT");
            check(Main.str(created.get("directory")).equals(own.resolve("Загрузки_ä_🚀").toString()),
                    "Route-only profile supplies its saved Unicode download directory");
            check(!input.containsKey("site_profile_id") && !input.containsKey("directory"),
                    "Route-only normalization never mutates caller input");
            check(manager.jobs.get(firstId).thread == null, "Future scheduled fixture never starts a source worker");
            Map<String,Object> added = manager.updateJobRoutes(firstId,
                    List.of(route(GATEWAYS.get(0), null), route(GATEWAYS.get(1), null)));
            binding(added, profile, "SELECTED-ACCOUNT");
            check(enabled(added).equals(Set.of(GATEWAYS.get(0), GATEWAYS.get(1))),
                    "Adding a gateway retains both requested gateways with original authentication");
            Map<String,Object> removed = manager.updateJobRoutes(firstId, List.of(route(GATEWAYS.get(1), null)));
            binding(removed, profile, "SELECTED-ACCOUNT");
            check(enabled(removed).equals(Set.of(GATEWAYS.get(1))), "Removing a gateway preserves the remaining profile binding");
            String before = Json.stringify(manager.job(firstId).snapshot());
            rejected(() -> manager.updateJobRoutes(firstId, List.of(route(GATEWAYS.get(1), second))),
                    "Runtime route replacement rejects another account profile");
            check(before.equals(Json.stringify(manager.job(firstId).snapshot())), "Rejected account switch does not mutate the saved download");
            Map<String,Object> legacy = manager.jobs.get(firstId).state;
            synchronized (manager.jobs.get(firstId)) { legacy.remove("site_profile_id"); manager.jobs.get(firstId).persist(); }
            Map<String,Object> legacyAdded = manager.updateJobRoutes(firstId,
                    List.of(route(GATEWAYS.get(1), null), route(GATEWAYS.get(2), null)));
            binding(legacyAdded, profile, "SELECTED-ACCOUNT");
            check(enabled(legacyAdded).equals(Set.of(GATEWAYS.get(1), GATEWAYS.get(2))),
                    "Legacy route-only job derives one profile before adding another gateway");
            rejected(() -> manager.create(request("conflict.bin", List.of(route(GATEWAYS.get(0), profile), route(GATEWAYS.get(1), second)))),
                    "Conflicting per-gateway profile IDs are rejected before queue creation");
            Map<String,Object> mismatch = request("mismatch.bin", List.of(route(GATEWAYS.get(0), second))); mismatch.put("site_profile_id", profile);
            rejected(() -> manager.create(mismatch), "Conflicting top-level and route profile IDs are rejected");
            Map<String,Object> invalidRoute = route(GATEWAYS.get(0), null); invalidRoute.put("site_profile_id", 17);
            rejected(() -> manager.create(request("invalid.bin", List.of(invalidRoute))), "Non-string per-gateway profile ID is rejected");
            Map<String,Object> foreign = request("foreign.bin", List.of(route(GATEWAYS.get(0), profile)));
            foreign.put("url", "https://foreign-owned.invalid/file.bin");
            rejected(() -> manager.create(foreign), "Route-only profile enforces the initial source origin");
            Map<String,Object> single = SiteProfiles.apply(Map.of("url", "relative.bin", "route", route(GATEWAYS.get(0), profile)));
            check(profile.equals(single.get("site_profile_id")) && Main.str(single.get("url")).equals("https://profile-routes.invalid/каталог/relative.bin"),
                    "Single-route profile derives identity and resolves a relative URL");
            Map<String,Object> fromSource = manager.createFromSource(request("Созданный-источником_🚀.bin", List.of(route(GATEWAYS.get(0), profile))));
            binding(Main.obj(Main.arr(fromSource.get("jobs")).get(0)), profile, "SELECTED-ACCOUNT");
            List<Object> normalized = Main.normalizeRoutes(List.of(route(GATEWAYS.get(0), profile), route(GATEWAYS.get(0), profile)));
            check(normalized.size() == 1 && profile.equals(Main.obj(normalized.get(0)).get("site_profile_id")),
                    "Duplicate equivalent gateway normalization retains exactly one authenticated path");
            rejected(() -> Main.normalizeRoutes(List.of(route(GATEWAYS.get(0), profile), route(GATEWAYS.get(0), second))),
                    "Duplicate gateway with conflicting account binding is rejected");
            String manifest = Files.readString(own.resolve(firstId + ".json"), StandardCharsets.UTF_8);
            check(manifest.contains("Сохранённый_日本語_é_🚀.bin") && !manifest.contains("\ufffd") && !manifest.contains("OWN-PASSWORD"),
                    "Persistent profile route manifest preserves exact Unicode without source credentials");
        } finally { manager.shutdown(); }
        Main.Manager restarted = manager(own);
        try {
            binding(restarted.job(firstId).snapshot(), profile, "SELECTED-ACCOUNT");
            check(sourceCalls.get() == 0, "Entire profile normalization and restart fixture uses zero source requests");
        } finally { restarted.shutdown(); GatewayNetwork.close(); AppLog.close(); }
        Map<String,Object> report = new LinkedHashMap<>(); report.put("status", "PASS"); report.put("version", Main.VERSION);
        report.put("checks", checks); report.put("cases", cases); report.put("network_calls", 0); report.put("source_calls", sourceCalls.get());
        report.put("user_data_read", false); report.put("os_routes_changed", false); report.put("physical_gateway_tested", false);
        Files.writeString(own.resolve("PROFILE-ROUTES-TESTS.json"), Json.stringify(report) + "\n", StandardCharsets.UTF_8);
        System.out.println("PASS " + checks + " profile routes checks");
    }
}
