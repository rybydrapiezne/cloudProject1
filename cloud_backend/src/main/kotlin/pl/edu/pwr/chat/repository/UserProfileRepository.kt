package pl.edu.pwr.chat.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.edu.pwr.chat.model.UserProfile

@Repository
interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findByUsername(username: String): UserProfile?
}
