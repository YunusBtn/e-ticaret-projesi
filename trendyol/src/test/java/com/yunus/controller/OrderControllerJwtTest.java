@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class, JwtService.class})
@TestPropertySource(properties = {
        "jwt.secret=YnUteWFsbml6Y2EtdGVzdC1pY2luLXVyZXRpbG1pcy1hbmFodGFyLTAxMjM0NTY3ODktYWJjZGVm",
        "jwt.expiration=900000"
})
@DisplayName("JwtFilter (gerçek JWT ile)")
class OrderControllerJwtTest {

    private static final String TEST_SECRET =
            "YnUteWFsbml6Y2EtdGVzdC1pY2luLXVyZXRpbG1pcy1hbmFodGFyLTAxMjM0NTY3ODktYWJjZGVm";
    private static final String BASKA_ANAHTAR =
            "YmFza2EtYmlyLXRlc3QtYW5haHRhcmktaW16YS11eXVzbWF6bGlnaS1pY2luLTk4NzY1NDMyMTA=";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    OrderService orderService;

    private final UserPrincipal kullanici = principal(1L, Role.USER);
    private final UserPrincipal admin = principal(2L, Role.ADMIN);

    private static UserPrincipal principal(Long id, Role rol) {
        return new UserPrincipal(User.builder().id(id).username("kullanici-" + id).role(rol).build());
    }

    private static SecretKey anahtar(String base64) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64));
    }

    private void reddedilir(String authorizationBasligi) throws Exception {
        mockMvc.perform(get("/api/orders/my").header(HttpHeaders.AUTHORIZATION, authorizationBasligi))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("geçerli USER token'ı: istek geçer ve controller'a filtrenin kurduğu UserPrincipal ulaşır")
    void gecerliToken_kimlikKurulur() throws Exception {
        String token = jwtService.generateToken(kullanici);
        when(customUserDetailsService.loadUserByUsername("kullanici-1")).thenReturn(kullanici);

        mockMvc.perform(get("/api/orders/get/10").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        verify(orderService).getOrderById(eq(10L), same(kullanici));
    }

    @Test
    @DisplayName("geçerli ADMIN token'ı: admin ucu 200 (yetki, token'dan değil kullanıcı kaydından geliyor)")
    void gecerliAdminToken_adminUcu200() throws Exception {
        String token = jwtService.generateToken(admin);
        when(customUserDetailsService.loadUserByUsername("kullanici-2")).thenReturn(admin);

        mockMvc.perform(get("/api/orders/all").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("geçerli USER token'ı admin ucunda 403 (kimlik var ama yetki yok)")
    void gecerliUserToken_adminUcu403() throws Exception {
        String token = jwtService.generateToken(kullanici);
        when(customUserDetailsService.loadUserByUsername("kullanici-1")).thenReturn(kullanici);

        mockMvc.perform(get("/api/orders/all").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("başlık 'Bearer ' ile başlamıyorsa token hiç okunmaz → 401")
    void bearerOnekiYok_401() throws Exception {
        String token = jwtService.generateToken(kullanici);

        reddedilir(token);

        verify(customUserDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("rastgele metin token → 401, 500 değil; kullanıcı aranmaz")
    void rastgeleMetin_401() throws Exception {
        reddedilir("Bearer bu.bir.token.degil");

        verify(customUserDetailsService, never()).loadUserByUsername(any());
    }