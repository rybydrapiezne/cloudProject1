package pl.edu.pwr.chat.model

import jakarta.persistence.*

@Entity
@Table(name = "user_profiles")
data class UserProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val username: String,

    @Column(name = "profile_picture_url")
    var profilePictureUrl: String? = null
)
