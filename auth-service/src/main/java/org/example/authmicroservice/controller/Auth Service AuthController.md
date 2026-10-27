# Auth Service: AuthController

Oct 27, 2026 · @Vincent

## Overview

`AuthController` is the auth-service's only REST controller: two POST endpoints under `/api/auth` that register users and exchange a username and password for a one-hour JWT. Both are `permitAll()` in `WebSecurityConfig`, and `@CrossOrigin(origins = "*")` opens them to any browser origin.

It depends on five injected beans:

- `AuthenticationManager`: verifies credentials through the BCrypt-backed `DaoAuthenticationProvider`
- `UserRepository` and `RoleRepository`: JPA access to the `users`, `roles` and `user_roles` tables (H2 in memory)
- `PasswordEncoder`: `BCryptPasswordEncoder`, used to hash passwords on signup
- `JwtUtils`: signs the token (HS256, `app.jwt.secret`, 3,600,000 ms expiry)

The three roles (`ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`) are seeded at startup by `initRoles` in `AuthMicroservice`.

Source: `auth-service/src/main/java/org/example/authmicroservice/controller/AuthController.java`.

## Endpoints

Both endpoints take JSON and return JSON on the happy path. The service listens on port 8083.

| Endpoint                | Request body                                                                       | Success                                                                              | Errors                                                                                                                                                   |
| ----------------------- | ---------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `POST /api/auth/signin` | `LoginRequest`: `username`, `password`                                             | 200, `JwtResponse`: `token`, `type` (`"Bearer"`), `id`, `username`, `email`, `roles` | Wrong password or unknown user throws `BadCredentialsException`; with the current security config this likely surfaces as 403 with an empty body         |
| `POST /api/auth/signup` | `SignupRequest`: `username`, `email`, `password`, `role` (optional set of strings) | 200, `{"message": "User registered successfully!"}`                                  | 400 `Error: Username is already taken!`; 400 `Error: Email is already in use!`; a missing role row throws `RuntimeException` (500, likely masked as 403) |

Example sign-in:

```bash
curl -X POST http://localhost:8083/api/auth/signin \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"s3cret"}'
```

Neither request DTO carries validation annotations, and the controller does not use `@Valid`; `spring-boot-starter-validation` is not on the classpath.

## Sign-in

`authenticateUser` delegates the password check to Spring Security, then builds the response from the authenticated principal.

![alt text](<JWT Sign-In Authentication Sequence.png>)

If the hash does not match, or the username is unknown, the provider throws `BadCredentialsException` and the controller never reaches `JwtUtils`. The token carries only the username (`sub`), `iat` and `exp`; the roles go in the response body, not in the token.

## Sign-up

`registerUser` runs four steps and returns 200 once the user row is saved. It never authenticates the new user or returns a token.

1. Reject with 400 if `existsByUsername` is true.
2. Reject with 400 if `existsByEmail` is true.
3. Build a `User` with the username, email and the BCrypt hash of the password.
4. Map the requested roles, set them on the user and call `userRepository.save`.

Role mapping is a `switch` over each string in `role`:

| Requested string                  | Assigned role    |
| --------------------------------- | ---------------- |
| `role` absent (`null`)            | `ROLE_USER`      |
| `"admin"`                         | `ROLE_ADMIN`     |
| `"mod"`                           | `ROLE_MODERATOR` |
| Anything else, including `"user"` | `ROLE_USER`      |

An empty array (`"role": []`) is not `null`, so the loop runs zero times and the user is saved with no roles.

## Observations and recommendations

The most serious issue is that any anonymous caller can register as an admin. Fix that before anything else. Ordered by impact:

1. **Self-assigned admin role.** `POST /api/auth/signup` with `"role": ["admin"]` creates a `ROLE_ADMIN` user, no authentication needed. Ignore `role` on public signup and always assign `ROLE_USER`. Move role grants to an admin-only endpoint guarded by `@PreAuthorize("hasRole('ADMIN')")`.
2. **No input validation.** Null or blank `username` and `email` are saved as-is. A missing `password` makes `BCryptPasswordEncoder.encode(null)` throw, which turns into a 500. Add `spring-boot-starter-validation`, annotate the DTOs (`@NotBlank`, `@Email`, `@Size`) and put `@Valid` on both `@RequestBody` parameters.
3. **Failed logins have no proper status.** No `@RestControllerAdvice` or entry point maps `AuthenticationException`, so a wrong password likely returns 403 with an empty body. Map it to 401 with a JSON message, and permit `/error` in `WebSecurityConfig`.
4. **Roles are not in the token.** `generateJwtToken` sets only `sub`, `iat` and `exp`. Downstream services cannot authorize by role without calling back. Add a `roles` claim taken from the authorities already computed in `authenticateUser`.
5. **Duplicate check races the insert.** Two concurrent signups with the same username both pass `existsByUsername`; the second hits the unique constraint and returns 500. Catch `DataIntegrityViolationException` and return 409.
6. **No brute-force protection on signin.** There is no rate limit or lockout. Add one at the gateway, or count failures per username.
7. **Account enumeration.** Separate "username taken" and "email in use" messages tell a caller which accounts exist. Acceptable for a demo; use one generic message if that matters.
8. **Small cleanups.**
   - `SecurityContextHolder...setAuthentication` does nothing useful in a stateless service.
   - Replace `@CrossOrigin(origins = "*")` with a central CORS config and named origins.
   - Use constructor injection instead of `@Autowired` fields.
   - Throw a dedicated exception, not `RuntimeException`, for a missing role row.
   - Reject `"role": []` or treat it like `null`.

### Open questions

- Should `/signup` return a token straight away, or keep the separate sign-in step?
- Who is allowed to create moderators and admins once self-assignment is removed?
