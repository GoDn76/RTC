package org.godn.rc.auth.service;

import org.godn.rc.auth.model.User;
import org.godn.rc.auth.payload.UpdateProfileDto;
import org.godn.rc.auth.payload.UserProfileDto;

public interface ProfileService {
    UserProfileDto getUserProfile(String email);

    // Update the profile of the currently logged-in user
    UserProfileDto updateProfile(String email, UpdateProfileDto updateProfileDto);

    User getUserByEmail(String email);
}
