// User.java
@Entity @Table(name = "USERS")
@Data @NoArgsConstructor
public class User {
    @Id private String id;
    private String displayName;
    private String profileImageUrl;
    @CreationTimestamp private Instant createdAt;
}