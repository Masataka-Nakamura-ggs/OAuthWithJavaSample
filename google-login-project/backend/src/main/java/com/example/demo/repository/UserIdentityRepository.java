// UserIdentityRepository.java
public interface UserIdentityRepository extends JpaRepository<UserIdentity, String> {
    Optional<UserIdentity> findByProviderNameAndProviderUserId(String providerName, String providerUserId);
}