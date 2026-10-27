# Auth Service: WebSecurityConfig

Oct 27, 2026 · @Vincent

## Overview

`WebSecurityConfig` is the single Spring Security configuration of the auth-service (port 8083, Spring Boot 3.5.0). It makes the service stateless, opens `/api/auth/**` and `/h2-console/**` to everyone, and wires username/password checking against the `users` table with BCrypt.

The auth-service issues JWTs but does not validate them on incoming requests. The JWT filter (`AuthTokenFilter`) and the unauthorized handler (`AuthEntryPointJwt`) are commented out, so every route outside the two open paths is closed with no way in.

Source: `auth-service/src/main/java/org/example/authmicroservice/security/WebSecurityConfig.java`. Collaborators: `UserDetailsServiceImpl`, `AuthController`, `JwtUtils`.

## Security filter chain

The `filterChain(HttpSecurity)` bean (lines 53–73) sets the rules below. Path rules are evaluated top to bottom; the first match wins.

| Setting                 | Value                      | Effect                                                                         |
| ----------------------- | -------------------------- | ------------------------------------------------------------------------------ |
| CSRF                    | Disabled                   | No CSRF token required on POST; acceptable because no session cookie is issued |
| Session policy          | `STATELESS`                | Spring never creates or reads an `HttpSession`                                 |
| `/api/auth/**`          | `permitAll()`              | `/signin` and `/signup` reachable without credentials                          |
| `/h2-console/**`        | `permitAll()`              | H2 web console open to anyone who can reach port 8083                          |
| Any other request       | `authenticated()`          | Requires an authenticated principal; nothing in the chain can create one       |
| Frame options           | `sameOrigin()`             | Lets the H2 console render in its own frames; applies to every response        |
| Authentication provider | `authenticationProvider()` | The BCrypt-backed `DaoAuthenticationProvider` below                            |

No JWT filter is added before `UsernamePasswordAuthenticationFilter` (line 70 is commented out), and no `httpBasic()` or `formLogin()` is configured. No custom `exceptionHandling` entry point is set either (line 56).

## Authentication components

Three beans do the password check; `AuthController` uses them directly on `/signin` and `/signup`.

| Bean                                | Type                                                       | What it does                                                                                             | Used by                                                    |
| ----------------------------------- | ---------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| `passwordEncoder()`                 | `BCryptPasswordEncoder` (default strength 10)              | Hashes passwords on signup, verifies them on signin                                                      | `DaoAuthenticationProvider`, `AuthController.registerUser` |
| `authenticationProvider()`          | `DaoAuthenticationProvider`                                | Loads the user via `UserDetailsServiceImpl` and compares the BCrypt hash                                 | `filterChain`, the global `AuthenticationManager`          |
| `authenticationManager(...)`        | `AuthenticationManager` from `AuthenticationConfiguration` | Entry point for `authenticate(...)`; Spring Boot builds it from the single `AuthenticationProvider` bean | `AuthController.authenticateUser`                          |
| `UserDetailsServiceImpl` (injected) | `UserDetailsService`                                       | Reads the user from the `users` table by username, maps roles to authorities                             | `DaoAuthenticationProvider`                                |

`@EnableMethodSecurity` turns on `@PreAuthorize` and friends, but no method in the service uses them yet.

`JwtUtils` is not wired here. The controller calls `generateJwtToken(username)` after a successful `authenticate(...)`; it signs HS256 with `app.jwt.secret` and expires after `app.jwt.expiration-ms` (3,600,000 ms, one hour).

## Request flow

Every request passes the same stateless chain; the path decides the rest. Only `/signin` with a correct password leaves with a token.

![alt text](<Authentication Flowchart_ Token Issuance Logic.png>)

`/signup` takes the same controller branch but skips the password check: it BCrypt-hashes the new password and saves the user. The bottom-right 403 is inferred from the config (no `/error` rule, no entry point), not observed.

## Observations and recommendations

The config is fine for a local demo; the H2 console and the missing error handling need fixing before anything else runs against it. Ordered by impact:

1. **H2 console is public in every environment.** `/h2-console/**` is `permitAll()` and frame options are relaxed unconditionally. Move both behind a `dev` profile, e.g. a separate `@Profile("dev")` `SecurityFilterChain` using `PathRequest.toH2Console()`.
2. **Failed logins likely return 403 with an empty body, not 401.** A `BadCredentialsException` from `/signin` is forwarded to `/error`, which falls under `anyRequest().authenticated()`; with no entry point, Spring answers 403. Add `.requestMatchers("/error").permitAll()` and restore an `AuthenticationEntryPoint` that returns 401 JSON. Worth confirming with a quick test.
3. **`authenticated()` routes are unreachable.** No JWT filter, `httpBasic` or `formLogin` is configured, so any future endpoint outside `/api/auth/**` returns 403 for every caller. Add `AuthTokenFilter` before `UsernamePasswordAuthenticationFilter` as soon as the service exposes one (e.g. `/api/users/me`).
4. **`spring.security.user.*` properties are dead config.** Because `UserDetailsServiceImpl` is a `UserDetailsService` bean, Spring Boot skips the in-memory user, so `admin` / `password` never works. Remove the two lines to avoid confusion.
5. **`authenticationProvider()` is registered twice.** Once by `AuthenticationConfiguration` (the global manager the controller uses) and once on `HttpSecurity` (line 67). Harmless, but the `http.authenticationProvider(...)` line can go.
6. **Field injection.** `@Autowired UserDetailsServiceImpl` on a package-private field; constructor injection makes the class testable and the dependency final.
7. **CORS is set only on the controller.** `@CrossOrigin(origins = "*")` works today because the paths are `permitAll()`. Once protected routes exist, configure `http.cors(...)` with a `CorsConfigurationSource` and named origins.

Outside this file but in the same security path:

- `AuthController.registerUser` lets any caller self-assign `admin` or `mod` through the `role` field on signup.
- `app.jwt.secret` is committed in `application.properties`; load it from an environment variable or secret store.
- `debug=true` is on in `application.properties`.

### Open questions

- Will the API gateway validate JWTs for downstream services, or should each service run its own `AuthTokenFilter`?
- Is H2 meant only for local runs, or does a shared environment use it too?
