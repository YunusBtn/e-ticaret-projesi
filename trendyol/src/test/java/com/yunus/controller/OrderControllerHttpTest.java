package com.yunus.controller;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class})
@DisplayName("OrderController HTTP sözleşmesi")
class OrderControllerHttpTest {

    private static final Long KULLANICI_ID = 1L;
    private static final Long ADMIN_ID = 2L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    private final UserPrincipal kullanici = principal(KULLANICI_ID, Role.USER);
    private final UserPrincipal admin = principal(ADMIN_ID, Role.ADMIN);

    private static UserPrincipal principal(Long id, Role rol) {
        return new UserPrincipal(User.builder().id(id).username("kullanici-" + id).role(rol).build());
    }

    private ResultActions siparisGonder(String json) throws Exception {
        return mockMvc.perform(post("/api/orders/create")
                .with(user(kullanici))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }