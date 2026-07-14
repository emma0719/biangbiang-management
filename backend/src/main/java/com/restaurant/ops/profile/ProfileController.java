package com.restaurant.ops.profile;

import com.restaurant.ops.auth.AppPrincipal;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class ProfileController {
  private final ProfileService profileService;
  private final ProfilePhotoService profilePhotoService;

  public ProfileController(ProfileService profileService, ProfilePhotoService profilePhotoService) {
    this.profileService = profileService;
    this.profilePhotoService = profilePhotoService;
  }

  @GetMapping
  ProfileDtos.EmployeePrivateResponse me(@AuthenticationPrincipal AppPrincipal principal) {
    return profileService.privateResponse(principal.employee());
  }

  @PatchMapping("/profile")
  ProfileDtos.EmployeePrivateResponse updateProfile(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ProfileDtos.UpdateProfileRequest request) {
    return profileService.privateResponse(profileService.updateProfile(principal.employee(), request));
  }

  @PatchMapping("/stores")
  ProfileDtos.EmployeePrivateResponse updateStores(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ProfileDtos.UpdateStoresRequest request) {
    return profileService.privateResponse(profileService.updateStores(principal.employee(), request));
  }

  @PatchMapping("/toast-pin")
  Map<String, String> updateToastPin(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ProfileDtos.UpdateToastPinRequest request) {
    profileService.updateToastPin(principal.employee(), principal.employee(), request.toastPin());
    return Map.of("status", "ok");
  }

  @PostMapping("/email-change/request")
  Map<String, String> requestEmail(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ProfileDtos.RequestEmailChange request) {
    profileService.requestEmailChange(principal.employee(), request.email());
    return Map.of("status", "ok");
  }

  @PostMapping("/email-change/verify")
  Map<String, String> verifyEmail(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ProfileDtos.VerifyContactChange request) {
    profileService.verifyContactChange(principal.employee(), request.token(), PendingContactChange.Type.EMAIL);
    return Map.of("status", "ok");
  }

  @PostMapping("/phone-change/request")
  Map<String, String> requestPhone(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ProfileDtos.RequestPhoneChange request) {
    profileService.requestPhoneChange(principal.employee(), request.phone());
    return Map.of("status", "ok");
  }

  @PostMapping("/phone-change/verify")
  Map<String, String> verifyPhone(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ProfileDtos.VerifyContactChange request) {
    profileService.verifyContactChange(principal.employee(), request.token(), PendingContactChange.Type.PHONE);
    return Map.of("status", "ok");
  }

  @PostMapping("/profile-photo")
  Map<String, String> profilePhoto(@AuthenticationPrincipal AppPrincipal principal, @RequestPart("file") MultipartFile file) {
    return Map.of("profilePhotoKey", profilePhotoService.upload(principal.employee(), file));
  }

  @DeleteMapping("/profile-photo")
  Map<String, String> deleteProfilePhoto(@AuthenticationPrincipal AppPrincipal principal) {
    profilePhotoService.delete(principal.employee());
    return Map.of("status", "ok");
  }
}
