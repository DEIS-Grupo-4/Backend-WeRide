package org.example.backendweride.platform.iam.domain.model.aggregates;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backendweride.platform.iam.domain.model.commands.SignUpCommand;
import org.example.backendweride.platform.iam.domain.model.valueobjects.ProfileId;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Objects;

/**
 * Account Aggregate Root
 *
 * @summary Represents an account in WeRide Platform.
 */
@Entity
@Table(name = "accounts")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Account extends AbstractAggregateRoot<Account> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @NotBlank
    @Getter
    @Size(max=20)
    @Column(unique = true)
    private String userName;

    @NotBlank
    @Getter
    private String password;

    @Getter
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ROLE_CLIENT'")
    private String role = "ROLE_CLIENT";

    @Embedded
    @Getter
    private ProfileId profileId;

    /**
     * Creates an account using a username and password.
     *
     * @param username account username
     * @param password account password
     */
    public Account(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.userName = username;
        this.password = password;
    }

    /**
     * Creates an account associated with a profile.
     *
     * @param username account username
     * @param password account password
     * @param profileId identifier of the associated profile
     */
    public Account(String username, String password, ProfileId profileId) {
        this(username, password);
        this.profileId = profileId;
    }

    /**
     * Creates an account using registration information.
     *
     * @param command account registration command
     */
    public Account(SignUpCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("SignUpCommand cannot be null");
        }
        if (command.username() == null || command.username().isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (command.password() == null || command.password().isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.userName = command.username();
        this.password = command.password();
    }

    /**
     * Creates an account using a registration command and hashed password.
     *
     * @param command account registration command
     * @param hashedPassword hashed account password
     */
    public Account(SignUpCommand command, String hashedPassword) {
        if (command == null) {
            throw new IllegalArgumentException("SignUpCommand cannot be null");
        }
        if (command.username() == null || command.username().isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (hashedPassword == null || hashedPassword.isBlank()) {
            throw new IllegalArgumentException("Hashed password cannot be null or empty");
        }
        this.userName = command.username();
        this.password = hashedPassword;
    }

    /**
     * Creates an account with registration information and a profile.
     *
     * @param command account registration command
     * @param hashedPassword hashed account password
     * @param profileId identifier of the associated profile
     */
    public Account(SignUpCommand command, String hashedPassword, ProfileId profileId) {
        this(command, hashedPassword);
        this.profileId = profileId;
    }

    /**
     * Updates the account password.
     *
     * @param password new account password
     * @return the updated account
     */
    public Account updatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.password = password;
        return this;
    }

    /**
     * Updates the account username.
     *
     * @param userName new account username
     * @return the updated account
     */
    public Account updateUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        this.userName = userName;
        return this;
    }

    /**
     * Updates the profile associated with the account.
     *
     * @param profileId new profile identifier
     * @return the updated account
     */
    public Account updateProfileId(ProfileId profileId) {
        if (profileId == null) {
            throw new IllegalArgumentException("ProfileId cannot be null");
        }
        this.profileId = profileId;
        return this;
    }

    /**
     * Assigns a role to the account.
     *
     * @param role role to be assigned
     * @return the updated account
     */
    public Account assignRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        this.role = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account other = (Account) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
