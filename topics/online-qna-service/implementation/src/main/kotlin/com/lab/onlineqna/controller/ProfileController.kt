package com.lab.onlineqna.controller

import com.lab.onlineqna.dto.UserProfileResponse
import com.lab.onlineqna.security.currentUserId
import com.lab.onlineqna.service.ProfileService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/profiles")
class ProfileController(
    private val profileService: ProfileService
) {

    @GetMapping("/me")
    fun getMyProfile(): UserProfileResponse = profileService.getProfile(currentUserId())
}
